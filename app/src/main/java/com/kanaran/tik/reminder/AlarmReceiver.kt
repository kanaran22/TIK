package com.kanaran.tik.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.kanaran.tik.R
import com.kanaran.tik.TikApplication
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.Task
import com.kanaran.tik.util.LocationUtil
import com.kanaran.tik.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the exact alarms fired by [AlarmScheduler]:
 *  - a plain time reminder, which notifies straight away, and
 *  - a combined task's window-start check, which notifies only if the user is already
 *    standing inside the task's radius (see [handleWindowStartCheck]).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return
        val isWindowStartCheck = intent.action == AlarmScheduler.ACTION_WINDOW_START_CHECK

        val pendingResult = goAsync()
        val app = context.applicationContext as TikApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.getById(taskId) ?: return@launch
                if (!task.isActive) return@launch

                if (isWindowStartCheck) {
                    handleWindowStartCheck(context, app, task)
                } else {
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

    /**
     * A combined task's geofence only speaks up on a crossing. If the user was already inside
     * the radius when the window opened, there is no crossing to hear — so check position once,
     * here, and remind if they're already there. If they're not, stay quiet: the geofence still
     * covers them arriving later in the window.
     */
    private suspend fun handleWindowStartCheck(context: Context, app: TikApplication, task: Task) {
        val today = ScheduleUtil.todayEpochDay()
        val alreadyRemindedToday = task.lastFiredEpochDay == today
        val armedToday = ScheduleUtil.isScheduledDay(task, today) && ScheduleUtil.isWithinWindowNow(task)

        if (alreadyRemindedToday || !armedToday || !isInsideRadius(context, task)) {
            // Nothing to fire now; make sure the next day's check is still armed.
            app.alarmScheduler.scheduleWindowStartCheck(task)
            return
        }

        val place = task.locationName ?: context.getString(R.string.default_place_name)
        app.notificationHelper.showReminder(task, context.getString(R.string.reminder_reason_arrive, place))
        // Records the fire (so the geofence won't double-remind today) and re-arms the next check.
        app.repository.recordFired(task)
    }

    /** Null-safe "is the device currently within [Task.radiusMeters] of the task's place?". */
    private suspend fun isInsideRadius(context: Context, task: Task): Boolean {
        val lat = task.latitude ?: return false
        val lng = task.longitude ?: return false
        if (!PermissionUtils.hasBackgroundLocationPermission(context)) {
            Log.d(TAG, "No background location permission; leaving task ${task.id} to its geofence")
            return false
        }

        val here = (LocationUtil.getCurrentLocation(context) as? LocationUtil.LocationResult.Found)
            ?.location ?: return false

        val distance = FloatArray(1)
        Location.distanceBetween(here.latitude, here.longitude, lat, lng, distance)
        return distance[0] <= task.radiusMeters
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
