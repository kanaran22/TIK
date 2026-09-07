package com.kanaran.tik.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kanaran.tik.MainActivity
import com.kanaran.tik.R
import com.kanaran.tik.data.Task

/** Builds and shows the "your reminder is due" notification for a task. */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val ACTION_MARK_DONE = "com.kanaran.tik.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.kanaran.tik.action.SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REASON_TEXT = "extra_reason_text"
        const val SNOOZE_MINUTES = 10L

        // A firm triple-buzz — noticeably stronger than a default notification tap.
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 250, 500, 250, 500)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Played on the alarm audio stream (not the notification stream) so it's louder
            // and cuts through a phone left on a table — without going full-screen or
            // overriding silent/Do Not Disturb the way a real alarm clock would.
            val alarmSound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(alarmSound, audioAttributes)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminder(task: Task, reasonText: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_REASON_TEXT, reasonText)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(reasonText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reasonText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.action_mark_done), markDonePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze, SNOOZE_MINUTES), snoozePendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
        }
    }

    fun dismiss(taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
