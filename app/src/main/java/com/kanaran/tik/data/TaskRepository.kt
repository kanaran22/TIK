package com.kanaran.tik.data

import com.kanaran.tik.reminder.AlarmScheduler
import com.kanaran.tik.reminder.GeofenceHelper
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for tasks. Besides talking to Room, it keeps the
 * scheduled alarms and geofences in sync whenever a task is created, edited,
 * completed or deleted.
 *
 * Scheduling split:
 *  - Time-only tasks get an exact [AlarmScheduler] alarm for their next occurrence.
 *  - Any task with a location reminder (alone or combined with time) keeps a
 *    geofence continuously registered; day/time-window gating happens when the
 *    transition is received (see GeofenceBroadcastReceiver), not by toggling
 *    the geofence itself. So combined tasks need no alarm at all.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val completionDao: TaskCompletionDao,
    private val alarmScheduler: AlarmScheduler,
    private val geofenceHelper: GeofenceHelper,
    /** Notified after any change a home screen widget would need to reflect. */
    private val widgetUpdater: suspend () -> Unit = {}
) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    suspend fun getById(id: Long): Task? = dao.getById(id)

    /** Active tasks for the home screen widget, cheapest read that doesn't need a Flow collector. */
    suspend fun getActiveTasks(): List<Task> = dao.getAllActive()

    /** Insert or update a task, then (re)arm whichever reminders it has enabled. */
    suspend fun save(task: Task): Long {
        if (task.id != 0L) {
            alarmScheduler.cancel(task)
            geofenceHelper.remove(task)
        }

        val id = dao.upsert(task)
        val saved = task.copy(id = if (task.id == 0L) id else task.id)
        arm(saved)
        widgetUpdater()
        return id
    }

    private fun arm(task: Task) {
        if (!task.isActive) return

        if (task.isTimeReminderEnabled && !task.isLocationReminderEnabled) {
            alarmScheduler.schedule(task)
        }
        if (task.isLocationReminderEnabled) {
            geofenceHelper.add(task)
        }
    }

    suspend fun setSeriesActive(task: Task, active: Boolean) {
        val updated = task.copy(isActive = active)
        dao.update(updated)
        if (active) {
            arm(updated)
        } else {
            alarmScheduler.cancel(updated)
            geofenceHelper.remove(updated)
        }
        widgetUpdater()
    }

    /** Marks *today's* occurrence done (a repeating task stays armed for its next one). */
    suspend fun setCompletedToday(task: Task, completed: Boolean) {
        val today = ScheduleUtil.todayEpochDay()
        val updated = task.copy(lastCompletedEpochDay = if (completed) today else null)
        dao.update(updated)
        if (completed) {
            completionDao.insert(TaskCompletion(taskId = task.id, epochDay = today))
        } else {
            completionDao.delete(task.id, today)
        }
        widgetUpdater()
    }

    /** Called when a reminder actually fires, so it isn't re-sent the same day and (for
     *  time-only tasks) so the next occurrence gets scheduled. */
    suspend fun recordFired(task: Task) {
        val today = ScheduleUtil.todayEpochDay()
        val updated = task.copy(lastFiredEpochDay = today)
        dao.update(updated)
        if (updated.isTimeReminderEnabled && !updated.isLocationReminderEnabled) {
            alarmScheduler.schedule(updated)
        }
        widgetUpdater()
    }

    suspend fun delete(task: Task) {
        alarmScheduler.cancel(task)
        geofenceHelper.remove(task)
        dao.delete(task)
        completionDao.deleteAllForTask(task.id)
        widgetUpdater()
    }

    /** Undoes a just-performed [delete]: puts the task and its completion history back, then re-arms it. */
    suspend fun restore(task: Task, completedDays: Set<Long>) {
        dao.upsert(task)
        completedDays.forEach { day -> completionDao.insert(TaskCompletion(taskId = task.id, epochDay = day)) }
        arm(task)
        widgetUpdater()
    }

    /** Completed-day history for one task, most recent first — the basis for streaks/stats. */
    suspend fun getCompletedDays(taskId: Long): Set<Long> = completionDao.getCompletedDays(taskId).toSet()

    fun observeAllCompletions(): Flow<List<TaskCompletion>> = completionDao.observeAll()

    /** Called once on app/device startup to re-arm reminders that survived a restart. */
    suspend fun rearmAllActiveReminders() {
        dao.getActiveTimeReminders()
            .filter { !it.isLocationReminderEnabled }
            .forEach { alarmScheduler.schedule(it) }
        dao.getActiveLocationReminders().forEach { geofenceHelper.add(it) }
    }
}
