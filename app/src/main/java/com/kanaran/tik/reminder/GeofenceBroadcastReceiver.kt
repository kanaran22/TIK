package com.kanaran.tik.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kanaran.tik.TikApplication
import com.kanaran.tik.R
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.Task
import com.kanaran.tik.data.TriggerType
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives ENTER/EXIT transitions for every geofence registered by [GeofenceHelper]. */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofencing error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }

        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) return

        val triggeringIds = event.triggeringGeofences?.mapNotNull { it.requestId } ?: return
        if (triggeringIds.isEmpty()) return

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                triggeringIds.forEach { requestId ->
                    val taskId = requestId.removePrefix("task_geofence_").toLongOrNull() ?: return@forEach
                    val task = app.repository.getById(taskId) ?: return@forEach
                    handleTransition(context, app, task, transition)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTransition(context: Context, app: TikApplication, task: Task, transition: Int) {
        if (!task.isActive) return

        val today = ScheduleUtil.todayEpochDay()
        if (task.lastFiredEpochDay == today) return // already reminded today
        if (!ScheduleUtil.isScheduledDay(task, today)) return // not an armed day
        if (!ScheduleUtil.isWithinWindowNow(task)) return // outside the time window, if any — a miss stays silent

        val wanted = TriggerType.fromStorage(task.triggerType)
        val matches = when (wanted) {
            TriggerType.ON_ARRIVE -> transition == Geofence.GEOFENCE_TRANSITION_ENTER
            TriggerType.ON_LEAVE -> transition == Geofence.GEOFENCE_TRANSITION_EXIT
            TriggerType.BOTH -> true
        }
        if (!matches) return

        val reasonRes = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            R.string.reminder_reason_arrive
        } else {
            R.string.reminder_reason_leave
        }
        val place = task.locationName ?: context.getString(R.string.default_place_name)
        app.notificationHelper.showReminder(task, context.getString(reasonRes, place))
        app.repository.recordFired(task)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
