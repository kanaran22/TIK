package com.kanaran.tik.data

/** When a location-based reminder should fire relative to its geofence. */
enum class TriggerType {
    ON_ARRIVE,
    ON_LEAVE,
    BOTH;

    companion object {
        fun fromStorage(value: String): TriggerType =
            entries.firstOrNull { it.name == value } ?: ON_ARRIVE
    }
}
