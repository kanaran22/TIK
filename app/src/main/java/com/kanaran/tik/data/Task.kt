package com.kanaran.tik.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single to-do item that can remind the user at a time, at a place, or both —
 * on a schedule that can repeat like an alarm clock's (once, daily, chosen
 * weekdays, a date range, or a hand-picked set of dates).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,
    val notes: String = "",

    /** Master on/off switch for the whole series. False = stopped/done. */
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),

    // --- Repeat schedule: which calendar days this task is armed on ---
    val repeatType: String = RepeatType.ONCE.name,
    /** Used when [repeatType] is ONCE. Epoch day (LocalDate.toEpochDay()). */
    val singleDateEpochDay: Long? = null,
    /** Used when [repeatType] is WEEKLY_CUSTOM. Bit 0 = Monday .. bit 6 = Sunday. */
    val weekdaysMask: Int = 0,
    /** Used when [repeatType] is DATE_RANGE. Inclusive epoch-day bounds. */
    val rangeStartEpochDay: Long? = null,
    val rangeEndEpochDay: Long? = null,
    /** Used when [repeatType] is CUSTOM_DATES. Comma-separated epoch days. */
    val customDatesEpochDays: String = "",

    // --- Time-based reminder ---
    val isTimeReminderEnabled: Boolean = false,
    /** Minutes since midnight. The fire time, or the arming-window start when combined with a place. */
    val timeOfDayMinutes: Int? = null,
    /** Minutes since midnight. Only set when both time and location reminders are enabled. */
    val windowEndTimeOfDayMinutes: Int? = null,

    // --- Location-based reminder ---
    val isLocationReminderEnabled: Boolean = false,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float = 150f,
    val triggerType: String = TriggerType.ON_ARRIVE.name,

    // --- Firing / completion bookkeeping ---
    /** Epoch day this task last actually sent a notification, to avoid firing twice in one day. */
    val lastFiredEpochDay: Long? = null,
    /** Epoch day the user last marked this occurrence done. */
    val lastCompletedEpochDay: Long? = null
) {
    /** Stable id used both as the Room primary key and as the geofence request id. */
    val geofenceRequestId: String get() = "task_geofence_$id"

    val isCombined: Boolean get() = isTimeReminderEnabled && isLocationReminderEnabled

    fun customDatesList(): List<Long> =
        customDatesEpochDays.split(",").mapNotNull { it.trim().toLongOrNull() }.sorted()

    companion object {
        fun encodeCustomDates(days: List<Long>): String = days.sorted().joinToString(",")
    }
}
