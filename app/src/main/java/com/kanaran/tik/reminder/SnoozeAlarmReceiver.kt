package com.kanaran.tik.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanaran.tik.TikApplication
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.TaskRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires [NotificationHelper.SNOOZE_MINUTES] after the user tapped Snooze, and re-shows the reminder. */
class SnoozeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val reasonText = intent.getStringExtra(NotificationHelper.EXTRA_REASON_TEXT) ?: return
        if (taskId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.getById(taskId)
                // Don't re-nag if the task was deleted, or marked done in the meantime — for a
                // repeating task "done" means today's occurrence, which isActive alone can't see.
                if (task != null && TaskRules.isDueToRemind(task, ScheduleUtil.todayEpochDay())) {
                    app.notificationHelper.showReminder(task, reasonText)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
