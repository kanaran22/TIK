package com.kanaran.tik.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.Task

/** Wraps [AlarmManager] to schedule/cancel a time-only task's next occurrence. */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntentFor(task: Task, action: String? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Extras are not part of Intent.filterEquals, but the action is — so the two
            // alarms a task can own stay distinct PendingIntents despite sharing a request code.
            action?.let { setAction(it) }
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    private fun setAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Fall back to an inexact alarm if the user hasn't granted the
            // "Alarms & reminders" special permission on Android 12+.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    /** True if the app is allowed to schedule exact alarms (always true below API 31). */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    /** Computes the task's next occurrence from its repeat schedule and arms (or clears) the alarm accordingly. */
    @SuppressLint("MissingPermission")
    fun schedule(task: Task) {
        val triggerAt = ScheduleUtil.nextTimeReminderMillis(task)
        if (triggerAt == null) {
            cancel(task)
            return
        }

        setAlarm(triggerAt, pendingIntentFor(task))
    }

    /**
     * Combined time+place tasks fire from a geofence *transition*, so a user who is already
     * standing at the place when the window opens would never be reminded — no crossing, no
     * event. This alarm fires at the window start so [AlarmReceiver] can check whether they
     * are already inside the radius.
     */
    fun scheduleWindowStartCheck(task: Task) {
        val triggerAt = ScheduleUtil.nextTimeReminderMillis(task)
        if (triggerAt == null) {
            cancelWindowStartCheck(task)
            return
        }
        setAlarm(triggerAt, pendingIntentFor(task, ACTION_WINDOW_START_CHECK))
    }

    fun cancel(task: Task) {
        alarmManager.cancel(pendingIntentFor(task))
        cancelWindowStartCheck(task)
    }

    fun cancelWindowStartCheck(task: Task) {
        alarmManager.cancel(pendingIntentFor(task, ACTION_WINDOW_START_CHECK))
    }

    companion object {
        /** Marks the alarm that opens a combined task's window, rather than a plain time reminder. */
        const val ACTION_WINDOW_START_CHECK = "com.kanaran.tik.action.WINDOW_START_CHECK"
    }
}
