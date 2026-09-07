package com.kanaran.tik.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Covers streak counting and completion rate, including the "don't look back before the task existed" rule. */
class StreakUtilTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 9, 3) // a Thursday

    private fun day(date: LocalDate) = date.toEpochDay()

    private fun createdAtMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun dailyTask(createdOn: LocalDate = today.minusYears(1)) = Task(
        title = "Daily",
        repeatType = RepeatType.DAILY.name,
        createdAt = createdAtMillis(createdOn)
    )

    /** Thursdays only. */
    private fun weeklyTask(createdOn: LocalDate = today.minusYears(1)) = Task(
        title = "Weekly",
        repeatType = RepeatType.WEEKLY_CUSTOM.name,
        weekdaysMask = 1 shl 3,
        createdAt = createdAtMillis(createdOn)
    )

    private fun daysOf(vararg dates: LocalDate): Set<Long> = dates.map { it.toEpochDay() }.toSet()

    // --- currentStreak --------------------------------------------------------------

    @Test
    fun `no completions is a zero streak`() {
        assertEquals(0, StreakUtil.currentStreak(dailyTask(), emptySet(), day(today)))
    }

    @Test
    fun `completing today alone is a streak of one`() {
        assertEquals(1, StreakUtil.currentStreak(dailyTask(), daysOf(today), day(today)))
    }

    @Test
    fun `consecutive completed days accumulate`() {
        val completed = daysOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(4, StreakUtil.currentStreak(dailyTask(), completed, day(today)))
    }

    @Test
    fun `today being unfinished does not break a running streak`() {
        // Today's reminder simply hasn't happened yet — yesterday and before are done.
        val completed = daysOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakUtil.currentStreak(dailyTask(), completed, day(today)))
    }

    @Test
    fun `a missed day in the past ends the streak`() {
        // today + yesterday done, the day before that skipped, then more done days behind it.
        val completed = daysOf(
            today,
            today.minusDays(1),
            // today.minusDays(2) missed
            today.minusDays(3),
            today.minusDays(4)
        )
        assertEquals(2, StreakUtil.currentStreak(dailyTask(), completed, day(today)))
    }

    @Test
    fun `unscheduled days between occurrences do not break the streak`() {
        // A Thursdays-only task completed three Thursdays running: the six off-days
        // between each pair must not count as misses.
        val completed = daysOf(today, today.minusWeeks(1), today.minusWeeks(2))
        assertEquals(3, StreakUtil.currentStreak(weeklyTask(), completed, day(today)))
    }

    @Test
    fun `a missed occurrence ends the streak for a weekly task`() {
        val completed = daysOf(today, today.minusWeeks(1), /* missed */ today.minusWeeks(3))
        assertEquals(2, StreakUtil.currentStreak(weeklyTask(), completed, day(today)))
    }

    // --- completionRate -------------------------------------------------------------

    @Test
    fun `a fully completed window is a rate of one`() {
        val completed = (0 until 30).map { today.minusDays(it.toLong()) }.toTypedArray()
        assertEquals(1f, StreakUtil.completionRate(dailyTask(), daysOf(*completed), day(today)), 0.0001f)
    }

    @Test
    fun `an untouched window is a rate of zero`() {
        assertEquals(0f, StreakUtil.completionRate(dailyTask(), emptySet(), day(today)), 0.0001f)
    }

    @Test
    fun `half a window completed is a rate of one half`() {
        val completed = (0 until 15).map { today.minusDays(it.toLong()) }.toTypedArray()
        assertEquals(0.5f, StreakUtil.completionRate(dailyTask(), daysOf(*completed), day(today)), 0.0001f)
    }

    @Test
    fun `a brand new task is not penalised for days before it existed`() {
        // Regression: a task created today and completed today is 100%, not 1/30.
        val task = dailyTask(createdOn = today)
        assertEquals(1f, StreakUtil.completionRate(task, daysOf(today), day(today)), 0.0001f)
    }

    @Test
    fun `a task created part way through the window only counts days since creation`() {
        // Created 4 days ago, completed 2 of those 5 scheduled days (today included).
        val task = dailyTask(createdOn = today.minusDays(4))
        val completed = daysOf(today, today.minusDays(2))
        assertEquals(2f / 5f, StreakUtil.completionRate(task, completed, day(today)), 0.0001f)
    }

    @Test
    fun `a task with no scheduled days in the window reports zero rather than dividing by zero`() {
        val neverFires = Task(
            title = "Never",
            repeatType = RepeatType.WEEKLY_CUSTOM.name,
            weekdaysMask = 0,
            createdAt = createdAtMillis(today.minusYears(1))
        )
        assertEquals(0f, StreakUtil.completionRate(neverFires, emptySet(), day(today)), 0.0001f)
    }

    @Test
    fun `weekly task rate counts only its own occurrences`() {
        // Over a 30-day window there are 4-5 Thursdays; completing every one is 100%.
        val thursdays = (0 until 5).map { today.minusWeeks(it.toLong()) }.toTypedArray()
        assertEquals(1f, StreakUtil.completionRate(weeklyTask(), daysOf(*thursdays), day(today)), 0.0001f)
    }

    // --- totalCompletions -----------------------------------------------------------

    @Test
    fun `total completions is the number of recorded days`() {
        assertEquals(0, StreakUtil.totalCompletions(emptySet()))
        assertEquals(3, StreakUtil.totalCompletions(daysOf(today, today.minusDays(1), today.minusDays(9))))
    }
}
