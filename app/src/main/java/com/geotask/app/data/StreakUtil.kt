package com.geotask.app.data

import java.time.Instant
import java.time.ZoneId

/** Computes streaks and completion rates for a repeating task from its [TaskCompletion] history. */
object StreakUtil {

    /**
     * Consecutive scheduled days, walking backward from [fromEpochDay], that were completed.
     * A scheduled day that hasn't happened yet (today, before its reminder fires) doesn't break
     * the streak; a scheduled day in the past that was missed does.
     */
    fun currentStreak(task: Task, completedDays: Set<Long>, fromEpochDay: Long): Int {
        var streak = 0
        var day = fromEpochDay
        while (fromEpochDay - day <= 3660) { // safety cap, ~10 years
            if (ScheduleUtil.isScheduledDay(task, day)) {
                when {
                    completedDays.contains(day) -> streak++
                    day == fromEpochDay -> Unit // today's occurrence just hasn't happened yet — not a miss
                    else -> break // a past scheduled day with no completion ends the streak
                }
            }
            day--
        }
        return streak
    }

    /**
     * Completion rate over the last [windowDays] scheduled days, as a 0f..1f fraction. Never looks
     * further back than the task's creation day — a brand-new task isn't penalized for the days
     * before it existed.
     */
    fun completionRate(task: Task, completedDays: Set<Long>, fromEpochDay: Long, windowDays: Int = 30): Float {
        val createdEpochDay = Instant.ofEpochMilli(task.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        var scheduled = 0
        var completed = 0
        for (offset in 0 until windowDays) {
            val day = fromEpochDay - offset
            if (day < 0 || day < createdEpochDay) break
            if (ScheduleUtil.isScheduledDay(task, day)) {
                scheduled++
                if (completedDays.contains(day)) completed++
            }
        }
        return if (scheduled == 0) 0f else completed.toFloat() / scheduled
    }

    fun totalCompletions(completedDays: Set<Long>): Int = completedDays.size
}
