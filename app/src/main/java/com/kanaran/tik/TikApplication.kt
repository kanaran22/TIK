package com.kanaran.tik

import android.app.Application
import com.kanaran.tik.data.SavedPlaceRepository
import com.kanaran.tik.data.TaskDatabase
import com.kanaran.tik.data.TaskRepository
import com.kanaran.tik.reminder.AlarmScheduler
import com.kanaran.tik.reminder.GeofenceHelper
import com.kanaran.tik.reminder.NotificationHelper
import androidx.glance.appwidget.updateAll
import com.kanaran.tik.widget.TikWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple hand-rolled service locator: builds every singleton the app needs
 * (database, repository, reminder helpers) once and hands them out from here.
 */
class TikApplication : Application() {

    val database: TaskDatabase by lazy { TaskDatabase.getInstance(this) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(this) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val geofenceHelper: GeofenceHelper by lazy { GeofenceHelper(this) }

    val repository: TaskRepository by lazy {
        TaskRepository(
            database.taskDao(),
            database.taskCompletionDao(),
            alarmScheduler,
            geofenceHelper,
            widgetUpdater = { TikWidget().updateAll(this) }
        )
    }

    val savedPlaceRepository: SavedPlaceRepository by lazy {
        SavedPlaceRepository(database.savedPlaceDao())
    }

    /** Process-lifetime scope for work that must not be tied to any one screen. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // A force-stop wipes every alarm and geofence this app registered, and so do the
        // "clean up" / battery-boost buttons many Android skins ship. Nothing used to restore
        // them short of a reboot, so reminders just stopped. Re-arm whenever the process
        // starts — opening the app, touching the widget, anything. It's idempotent: each
        // alarm and geofence is keyed per task, so re-arming replaces rather than duplicates.
        appScope.launch { repository.rearmAllActiveReminders() }
    }
}
