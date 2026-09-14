package com.kanaran.tik.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanaran.tik.TikApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every reminder after the system events that invalidate them: a reboot (alarms and
 * geofences are cleared), an app update, the clock or time zone changing (fire times are
 * computed as absolute instants, so a 9:00 reminder would otherwise fire at the old zone's
 * 9:00), and the exact-alarm permission being granted again (revoking it cancels them all).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REARM_ACTIONS) return

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

    companion object {
        private val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON", // several OEM "fast boot" modes skip BOOT_COMPLETED
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
