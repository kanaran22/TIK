package com.geotask.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geotask.app.TikApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms and geofences don't survive a reboot, so re-arm them once the device is back up. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.rearmAllActiveReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
