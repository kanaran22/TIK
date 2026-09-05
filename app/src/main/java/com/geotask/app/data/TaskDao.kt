package com.geotask.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isActive DESC, createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE isLocationReminderEnabled = 1 AND isActive = 1")
    suspend fun getActiveLocationReminders(): List<Task>

    @Query("SELECT * FROM tasks WHERE isTimeReminderEnabled = 1 AND isActive = 1")
    suspend fun getActiveTimeReminders(): List<Task>

    /** Active tasks for the home screen widget, most recently created first. */
    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAllActive(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
