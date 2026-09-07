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
}
