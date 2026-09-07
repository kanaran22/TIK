package com.kanaran.tik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, SavedPlace::class, TaskCompletion::class],
    version = 3,
    // Schemas are written to app/schemas and committed — they're what migrations get written against.
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun taskCompletionDao(): TaskCompletionDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "tik.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Versions 1 and 2 only ever existed on dev machines mid-build, and their
                    // schemas weren't recorded, so those alone are still allowed to reset.
                    // Version 3 onward carries real user data: a missing migration must fail
                    // loudly rather than quietly wipe someone's tasks and streaks.
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .build().also { INSTANCE = it }
            }
    }
}
