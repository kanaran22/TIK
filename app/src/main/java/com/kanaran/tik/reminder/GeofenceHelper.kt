package com.kanaran.tik.reminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kanaran.tik.data.Task
import com.kanaran.tik.data.TriggerType
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/** Wraps [GeofencingClient] to add/remove the geofence backing a task's location reminder. */
class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun add(task: Task) {
        val lat = task.latitude ?: return
        val lng = task.longitude ?: return

        val transitionTypes = when (TriggerType.fromStorage(task.triggerType)) {
            TriggerType.ON_ARRIVE -> Geofence.GEOFENCE_TRANSITION_ENTER
            TriggerType.ON_LEAVE -> Geofence.GEOFENCE_TRANSITION_EXIT
            TriggerType.BOTH -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val geofence = Geofence.Builder()
            .setRequestId(task.geofenceRequestId)
            .setCircularRegion(lat, lng, task.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(30_000)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnFailureListener { e -> Log.w(TAG, "Failed to add geofence for task ${task.id}", e) }
    }

    fun remove(task: Task) {
        geofencingClient.removeGeofences(listOf(task.geofenceRequestId))
    }

    companion object {
        private const val TAG = "GeofenceHelper"
    }
}
