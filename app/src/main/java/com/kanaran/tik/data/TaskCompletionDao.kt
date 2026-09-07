package com.kanaran.tik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: TaskCompletion)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND epochDay = :epochDay")
    suspend fun delete(taskId: Long, epochDay: Long)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)

    @Query("SELECT epochDay FROM task_completions WHERE taskId = :taskId ORDER BY epochDay DESC")
    suspend fun getCompletedDays(taskId: Long): List<Long>

    /** All completions, for the overview stats screen (kept small — completion rows, not full tasks). */
    @Query("SELECT * FROM task_completions")
    fun observeAll(): Flow<List<TaskCompletion>>
}
