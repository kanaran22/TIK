package com.geotask.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure calendar math for a task's repeat schedule: which days it's armed on,
 * and when its next time-based alarm should fire. No Android dependencies,
 * so this is easy to reason about (and to unit test).
 */
object ScheduleUtil {

    /** Safety cap so a malformed/never-matching schedule can't spin forever. */
    private const val MAX_LOOKAHEAD_DAYS = 3660L // ~10 years

    fun isScheduledDay(task: Task, epochDay: Long): Boolean {
        val date = LocalDate.ofEpochDay(epochDay)
        return when (RepeatType.fromStorage(task.repeatType)) {
            RepeatType.ONCE -> task.singleDateEpochDay == epochDay
            RepeatType.DAILY -> true
            RepeatType.WEEKLY_CUSTOM -> {
                val bit = 1 shl (date.dayOfWeek.value - 1)
                (task.weekdaysMask and bit) != 0
            }
            RepeatType.DATE_RANGE -> {
                val start = task.rangeStartEpochDay
                val end = task.rangeEndEpochDay
                start != null && end != null && epochDay in start..end
            }
            RepeatType.CUSTOM_DATES -> task.customDatesList().contains(epochDay)
        }
    }

    /** Whether this task can still match on or after [fromEpochDay] at all (cheap short-circuit). */
    private fun hasAnyOccurrenceOnOrAfter(task: Task, fromEpochDay: Long): Boolean =
        when (RepeatType.fromStorage(task.repeatType)) {
            RepeatType.ONCE -> (task.singleDateEpochDay ?: return false) >= fromEpochDay
            RepeatType.DAILY -> true
            RepeatType.WEEKLY_CUSTOM -> task.weekdaysMask != 0
            RepeatType.DATE_RANGE -> (task.rangeEndEpochDay ?: return false) >= fromEpochDay
            RepeatType.CUSTOM_DATES -> task.customDatesList().any { it >= fromEpochDay }
        }

    /** The next scheduled epoch day on or after [fromEpochDay], or null if the series has ended. */
    fun nextScheduledEpochDayOnOrAfter(task: Task, fromEpochDay: Long): Long? {
        if (!hasAnyOccurrenceOnOrAfter(task, fromEpochDay)) return null
        val limit = fromEpochDay + MAX_LOOKAHEAD_DAYS
        var day = fromEpochDay
        while (day <= limit) {
            if (isScheduledDay(task, day)) return day
            day++
        }
        return null
    }

    /**
     * The next moment (epoch millis) this task's time-based alarm should fire, or null if
     * [Task.isTimeReminderEnabled] is off or the repeat schedule has no more occurrences.
     */
    fun nextTimeReminderMillis(task: Task, nowMillis: Long = System.currentTimeMillis()): Long? {
        val timeOfDay = task.timeOfDayMinutes ?: return null
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate().toEpochDay()

        val todayFireMillis = LocalDate.ofEpochDay(today)
            .atStartOfDay(zone)
            .plusMinutes(timeOfDay.toLong())
            .toInstant()
            .toEpochMilli()

        if (isScheduledDay(task, today) && todayFireMillis > nowMillis) {
            return todayFireMillis
        }

        val nextDay = nextScheduledEpochDayOnOrAfter(task, today + 1) ?: return null
        return LocalDate.ofEpochDay(nextDay)
            .atStartOfDay(zone)
            .plusMinutes(timeOfDay.toLong())
            .toInstant()
            .toEpochMilli()
    }

    /** True if [nowMillis] falls within this task's arming window on the given day (time-only tasks: always true). */
    fun isWithinWindowNow(task: Task, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!task.isTimeReminderEnabled) return true
        val start = task.timeOfDayMinutes ?: return true
        val end = task.windowEndTimeOfDayMinutes ?: return true
        val nowMinutes = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).let {
            LocalTime.of(it.hour, it.minute).toSecondOfDay() / 60
        }
        return nowMinutes in start..end
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
}
