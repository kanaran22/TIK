package com.kanaran.tik.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named place the user has picked before, offered as a one-tap shortcut in the location picker. */
@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val createdAt: Long = System.currentTimeMillis()
)
