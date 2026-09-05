package com.geotask.app.data

import androidx.room.Entity

/**
 * One row per (task, day) the user marked that occurrence done. This is the history
 * [Task.lastCompletedEpochDay] doesn't keep — it's what streaks and stats are computed from.
 */
@Entity(tableName = "task_completions", primaryKeys = ["taskId", "epochDay"])
data class TaskCompletion(
    val taskId: Long,
    val epochDay: Long
)
