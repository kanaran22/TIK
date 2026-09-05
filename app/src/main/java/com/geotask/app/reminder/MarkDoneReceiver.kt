package com.geotask.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geotask.app.TikApplication
import com.geotask.app.data.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Mark as done" action button on a reminder notification. */
class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.getById(taskId)
                if (task != null) {
                    if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
                        app.repository.setSeriesActive(task, active = false)
                    } else {
                        app.repository.setCompletedToday(task, completed = true)
                    }
                }
                app.notificationHelper.dismiss(taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
