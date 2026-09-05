package com.geotask.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geotask.app.TikApplication
import com.geotask.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives the exact alarm fired by [AlarmScheduler] and shows the reminder notification. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.getById(taskId)
                if (task != null && task.isActive) {
                    app.notificationHelper.showReminder(
                        task,
                        context.getString(R.string.reminder_reason_time)
                    )
                    // Also arms the task's next occurrence, if its repeat schedule has one.
                    app.repository.recordFired(task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
