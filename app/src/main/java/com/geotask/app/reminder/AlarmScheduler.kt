package com.geotask.app.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.geotask.app.data.ScheduleUtil
import com.geotask.app.data.Task

/** Wraps [AlarmManager] to schedule/cancel a time-only task's next occurrence. */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntentFor(task: Task): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

        val pendingIntent = pendingIntentFor(task)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Fall back to an inexact alarm if the user hasn't granted the
            // "Alarms & reminders" special permission on Android 12+.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(task: Task) {
        alarmManager.cancel(pendingIntentFor(task))
    }
}
