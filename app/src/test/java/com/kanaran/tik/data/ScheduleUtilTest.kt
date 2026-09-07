package com.kanaran.tik.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Covers the repeat-schedule calendar math. Dates are built from [LocalDate] rather than
 * hardcoded epoch-day numbers so the intent stays readable, and expected fire times are
 * derived in the system zone so the suite passes wherever it runs.
 */
class ScheduleUtilTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    // A known Thursday, used as the anchor for every weekday-sensitive case.
    private val thursday = LocalDate.of(2026, 9, 3)
    private val friday = thursday.plusDays(1)
    private val nextThursday = thursday.plusWeeks(1)

    private fun day(date: LocalDate) = date.toEpochDay()

    private fun millisAt(date: LocalDate, minutesOfDay: Int): Long =
        date.atStartOfDay(zone).plusMinutes(minutesOfDay.toLong()).toInstant().toEpochMilli()

    private fun task(
        repeat: RepeatType,
        singleDate: LocalDate? = null,
        weekdaysMask: Int = 0,
        rangeStart: LocalDate? = null,
        rangeEnd: LocalDate? = null,
        customDates: List<LocalDate> = emptyList(),
        timeOfDayMinutes: Int? = null,
        windowEndTimeOfDayMinutes: Int? = null,
        isTimeReminderEnabled: Boolean = timeOfDayMinutes != null
    ) = Task(
        title = "Test",
        repeatType = repeat.name,
        singleDateEpochDay = singleDate?.toEpochDay(),
        weekdaysMask = weekdaysMask,
        rangeStartEpochDay = rangeStart?.toEpochDay(),
        rangeEndEpochDay = rangeEnd?.toEpochDay(),
        customDatesEpochDays = Task.encodeCustomDates(customDates.map { it.toEpochDay() }),
        isTimeReminderEnabled = isTimeReminderEnabled,
        timeOfDayMinutes = timeOfDayMinutes,
        windowEndTimeOfDayMinutes = windowEndTimeOfDayMinutes
    )

    // --- isScheduledDay -------------------------------------------------------------

    @Test
    fun `once fires only on its single date`() {
        val t = task(RepeatType.ONCE, singleDate = thursday)
        assertTrue(ScheduleUtil.isScheduledDay(t, day(thursday)))
        assertFalse(ScheduleUtil.isScheduledDay(t, day(thursday.minusDays(1))))
        assertFalse(ScheduleUtil.isScheduledDay(t, day(friday)))
    }

    @Test
    fun `once with no date chosen never fires`() {
        val t = task(RepeatType.ONCE, singleDate = null)
        assertFalse(ScheduleUtil.isScheduledDay(t, day(thursday)))
    }

    @Test
    fun `daily fires on every day`() {
        val t = task(RepeatType.DAILY)
        assertTrue(ScheduleUtil.isScheduledDay(t, day(thursday)))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(thursday.plusDays(1))))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(thursday.plusYears(3))))
    }

    @Test
    fun `weekly mask uses bit 0 for Monday through bit 6 for Sunday`() {
        val monday = LocalDate.of(2026, 8, 31) // the Monday before our anchor Thursday
        assertTrue(ScheduleUtil.isScheduledDay(task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 1 shl 0), day(monday)))
        assertTrue(ScheduleUtil.isScheduledDay(task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 1 shl 3), day(thursday)))
        assertTrue(
            ScheduleUtil.isScheduledDay(
                task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 1 shl 6),
                day(monday.plusDays(6)) // Sunday
            )
        )
    }

    @Test
    fun `weekly fires on selected weekdays only`() {
        // Thursday + Friday
        val t = task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = (1 shl 3) or (1 shl 4))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(thursday)))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(friday)))
        assertFalse(ScheduleUtil.isScheduledDay(t, day(thursday.plusDays(2)))) // Saturday
        assertTrue(ScheduleUtil.isScheduledDay(t, day(nextThursday)))
    }

    @Test
    fun `weekly with an empty mask never fires`() {
        val t = task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 0)
        assertFalse(ScheduleUtil.isScheduledDay(t, day(thursday)))
    }

    @Test
    fun `date range is inclusive of both ends`() {
        val start = thursday
        val end = thursday.plusDays(3)
        val t = task(RepeatType.DATE_RANGE, rangeStart = start, rangeEnd = end)
        assertFalse(ScheduleUtil.isScheduledDay(t, day(start.minusDays(1))))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(start)))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(start.plusDays(2))))
        assertTrue(ScheduleUtil.isScheduledDay(t, day(end)))
        assertFalse(ScheduleUtil.isScheduledDay(t, day(end.plusDays(1))))
    }

    @Test
    fun `date range with a missing bound never fires`() {
        assertFalse(
            ScheduleUtil.isScheduledDay(
                task(RepeatType.DATE_RANGE, rangeStart = thursday, rangeEnd = null),
                day(thursday)
            )
        )
        assertFalse(
            ScheduleUtil.isScheduledDay(
                task(RepeatType.DATE_RANGE, rangeStart = null, rangeEnd = friday),
                day(thursday)
            )
        )
    }

    @Test
    fun `custom dates fire only on the picked days`() {
        val picked = listOf(thursday, thursday.plusDays(5), thursday.plusMonths(2))
        val t = task(RepeatType.CUSTOM_DATES, customDates = picked)
        picked.forEach { assertTrue(ScheduleUtil.isScheduledDay(t, day(it))) }
        assertFalse(ScheduleUtil.isScheduledDay(t, day(friday)))
    }

    @Test
    fun `custom dates with none picked never fires`() {
        val t = task(RepeatType.CUSTOM_DATES, customDates = emptyList())
        assertFalse(ScheduleUtil.isScheduledDay(t, day(thursday)))
    }

    // --- nextScheduledEpochDayOnOrAfter --------------------------------------------

    @Test
    fun `next scheduled day returns the same day when it already matches`() {
        assertEquals(day(thursday), ScheduleUtil.nextScheduledEpochDayOnOrAfter(task(RepeatType.DAILY), day(thursday)))
        assertEquals(
            day(thursday),
            ScheduleUtil.nextScheduledEpochDayOnOrAfter(task(RepeatType.ONCE, singleDate = thursday), day(thursday))
        )
    }

    @Test
    fun `next scheduled day skips forward to the next matching weekday`() {
        val t = task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 1 shl 3) // Thursdays
        assertEquals(day(nextThursday), ScheduleUtil.nextScheduledEpochDayOnOrAfter(t, day(friday)))
    }

    @Test
    fun `next scheduled day jumps to the start of a future date range`() {
        val start = thursday.plusDays(10)
        val t = task(RepeatType.DATE_RANGE, rangeStart = start, rangeEnd = start.plusDays(2))
        assertEquals(day(start), ScheduleUtil.nextScheduledEpochDayOnOrAfter(t, day(thursday)))
    }

    @Test
    fun `next scheduled day is null once the series has ended`() {
        assertNull(
            ScheduleUtil.nextScheduledEpochDayOnOrAfter(
                task(RepeatType.ONCE, singleDate = thursday),
                day(friday)
            )
        )
        assertNull(
            ScheduleUtil.nextScheduledEpochDayOnOrAfter(
                task(RepeatType.DATE_RANGE, rangeStart = thursday.minusDays(5), rangeEnd = thursday.minusDays(1)),
                day(thursday)
            )
        )
        assertNull(
            ScheduleUtil.nextScheduledEpochDayOnOrAfter(
                task(RepeatType.CUSTOM_DATES, customDates = listOf(thursday.minusDays(3))),
                day(thursday)
            )
        )
    }

    @Test
    fun `next scheduled day is null for a weekly task with no weekdays selected`() {
        assertNull(
            ScheduleUtil.nextScheduledEpochDayOnOrAfter(
                task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 0),
                day(thursday)
            )
        )
    }

    // --- nextTimeReminderMillis -----------------------------------------------------

    @Test
    fun `no fire time when no time of day is set`() {
        val t = task(RepeatType.DAILY, timeOfDayMinutes = null, isTimeReminderEnabled = true)
        assertNull(ScheduleUtil.nextTimeReminderMillis(t, millisAt(thursday, 9 * 60)))
    }

    @Test
    fun `fires later today when today is scheduled and the time has not passed`() {
        val t = task(RepeatType.DAILY, timeOfDayMinutes = 21 * 60) // 21:00
        val now = millisAt(thursday, 9 * 60) // 09:00
        assertEquals(millisAt(thursday, 21 * 60), ScheduleUtil.nextTimeReminderMillis(t, now))
    }

    @Test
    fun `rolls to the next day once today's time has passed`() {
        val t = task(RepeatType.DAILY, timeOfDayMinutes = 9 * 60)
        val now = millisAt(thursday, 10 * 60) // an hour late
        assertEquals(millisAt(friday, 9 * 60), ScheduleUtil.nextTimeReminderMillis(t, now))
    }

    @Test
    fun `a fire time exactly now is treated as passed and rolls forward`() {
        val t = task(RepeatType.DAILY, timeOfDayMinutes = 9 * 60)
        val now = millisAt(thursday, 9 * 60)
        assertEquals(millisAt(friday, 9 * 60), ScheduleUtil.nextTimeReminderMillis(t, now))
    }

    @Test
    fun `skips unscheduled days when finding the next fire time`() {
        val t = task(RepeatType.WEEKLY_CUSTOM, weekdaysMask = 1 shl 3, timeOfDayMinutes = 8 * 60) // Thursdays
        val now = millisAt(thursday, 10 * 60) // Thursday, already past 08:00
        assertEquals(millisAt(nextThursday, 8 * 60), ScheduleUtil.nextTimeReminderMillis(t, now))
    }

    @Test
    fun `no fire time once a one-off task's day has passed`() {
        val t = task(RepeatType.ONCE, singleDate = thursday, timeOfDayMinutes = 8 * 60)
        val now = millisAt(thursday, 10 * 60)
        assertNull(ScheduleUtil.nextTimeReminderMillis(t, now))
    }

    // --- isWithinWindowNow ----------------------------------------------------------

    @Test
    fun `location-only tasks are always within the window`() {
        val t = task(RepeatType.DAILY, isTimeReminderEnabled = false)
        assertTrue(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 3 * 60)))
    }

    @Test
    fun `a time reminder with no window end is always within the window`() {
        val t = task(RepeatType.DAILY, timeOfDayMinutes = 8 * 60, windowEndTimeOfDayMinutes = null)
        assertTrue(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 23 * 60)))
    }

    @Test
    fun `combined task is armed only inside its window, bounds included`() {
        // 08:00 - 10:30
        val t = task(RepeatType.DAILY, timeOfDayMinutes = 8 * 60, windowEndTimeOfDayMinutes = 10 * 60 + 30)
        assertFalse(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 7 * 60 + 59)))
        assertTrue(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 8 * 60)))
        assertTrue(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 9 * 60)))
        assertTrue(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 10 * 60 + 30)))
        assertFalse(ScheduleUtil.isWithinWindowNow(t, millisAt(thursday, 10 * 60 + 31)))
    }

    // --- customDatesList ------------------------------------------------------------

    @Test
    fun `custom dates round-trip through storage sorted, ignoring junk`() {
        val encoded = Task.encodeCustomDates(listOf(300L, 100L, 200L))
        assertEquals("100,200,300", encoded)
        assertEquals(listOf(100L, 200L, 300L), Task(title = "x", customDatesEpochDays = encoded).customDatesList())
        assertEquals(emptyList<Long>(), Task(title = "x", customDatesEpochDays = "").customDatesList())
        assertEquals(listOf(5L), Task(title = "x", customDatesEpochDays = "abc, 5 ,").customDatesList())
    }
}
