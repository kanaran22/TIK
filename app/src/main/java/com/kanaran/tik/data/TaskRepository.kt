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
        if (task.isCombined) {
            // The geofence only reports crossings, so this covers "already there when the
            // window opened" — see AlarmReceiver.handleWindowStartCheck.
            alarmScheduler.scheduleWindowStartCheck(task)
        }
        if (task.isLocationReminderEnabled) {
            geofenceHelper.add(task)
        }
    }

    private fun disarm(task: Task) {
        alarmScheduler.cancel(task)
        geofenceHelper.remove(task)
    }

    /**
     * Ticks or unticks [taskId] for today. The one entry point for the list checkbox, the
     * widget checkbox and the notification's "Mark as done" — each used to carry its own copy
     * of this branching, and the copies disagreed.
     *
     * A one-off task is finished for good once done, so its reminders are disarmed (and come
     * back if it's unticked). A repeating task stays armed: its receivers skip today's
     * reminder via [TaskRules.isDueToRemind] and fire normally tomorrow.
     */
    suspend fun setDoneToday(taskId: Long, done: Boolean) {
        val task = dao.getById(taskId) ?: return
        val today = ScheduleUtil.todayEpochDay()

        if (done) {
            dao.setLastCompleted(taskId, today)
            completionDao.insert(TaskCompletion(taskId = taskId, epochDay = today))
        } else {
            // Undo the day it was actually recorded against (a one-off may have been ticked earlier).
            completionDao.delete(taskId, task.lastCompletedEpochDay ?: today)
            dao.setLastCompleted(taskId, null)
        }

        if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
            dao.setActive(taskId, !done)
            val fresh = dao.getById(taskId) ?: return
            if (done) disarm(fresh) else arm(fresh)
        }
        widgetUpdater()
    }

    /** Called once a reminder occurrence has been handled, so it isn't re-sent the same day and
     *  the next occurrence gets scheduled. */
    suspend fun recordFired(task: Task) {
        dao.setLastFired(task.id, ScheduleUtil.todayEpochDay())
        val fresh = dao.getById(task.id) ?: return
        if (!fresh.isActive) return
        if (fresh.isTimeReminderEnabled && !fresh.isLocationReminderEnabled) {
            alarmScheduler.schedule(fresh)
        }
        if (fresh.isCombined) {
            alarmScheduler.scheduleWindowStartCheck(fresh)
        }
    }

    suspend fun delete(task: Task) {
        disarm(task)
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
        dao.getActiveTimeReminders()
            .filter { it.isCombined }
            .forEach { alarmScheduler.scheduleWindowStartCheck(it) }
        dao.getActiveLocationReminders().forEach { geofenceHelper.add(it) }
    }
}
