package com.geotask.app.data

/** How a task's schedule repeats — which calendar days it is "armed" on. */
enum class RepeatType {
    /** Fires on a single specific date. */
    ONCE,

    /** Fires every day, indefinitely. */
    DAILY,

    /** Fires every week on a chosen set of weekdays (e.g. Mon/Wed/Fri). */
    WEEKLY_CUSTOM,

    /** Fires every day within a start–end date range, then stops automatically. */
    DATE_RANGE,

    /** Fires on a hand-picked set of individual, not-necessarily-recurring dates. */
    CUSTOM_DATES;

    companion object {
        fun fromStorage(value: String): RepeatType =
            entries.firstOrNull { it.name == value } ?: ONCE
    }
}
