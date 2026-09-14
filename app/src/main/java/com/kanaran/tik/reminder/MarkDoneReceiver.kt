package com.kanaran.tik.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanaran.tik.TikApplication
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
                app.repository.setDoneToday(taskId, done = true)
                app.notificationHelper.dismiss(taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
