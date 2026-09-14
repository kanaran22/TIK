package com.kanaran.tik.data

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

    // Targeted single-column writes. Mutations used to rewrite the whole row from a Task copy,
    // which could silently undo a concurrent write — e.g. an alarm recording that it fired
    // while the user was ticking the same task. Each of these touches only its own column.

    @Query("UPDATE tasks SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE tasks SET lastCompletedEpochDay = :epochDay WHERE id = :id")
    suspend fun setLastCompleted(id: Long, epochDay: Long?)

    @Query("UPDATE tasks SET lastFiredEpochDay = :epochDay WHERE id = :id")
    suspend fun setLastFired(id: Long, epochDay: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
