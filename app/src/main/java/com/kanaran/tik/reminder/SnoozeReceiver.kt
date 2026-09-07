package com.kanaran.tik.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.kanaran.tik.TikApplication
import java.util.concurrent.TimeUnit

/** Handles the "Snooze" action on a reminder notification: dismiss it, re-show it in [NotificationHelper.SNOOZE_MINUTES]. */
class SnoozeReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val reasonText = intent.getStringExtra(NotificationHelper.EXTRA_REASON_TEXT) ?: return
        if (taskId == -1L) return

        val app = context.applicationContext as TikApplication
        app.notificationHelper.dismiss(taskId)

        val fireIntent = Intent(context, SnoozeAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_REASON_TEXT, reasonText)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(NotificationHelper.SNOOZE_MINUTES)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }
}
