package com.geotask.app

import android.app.Application
import com.geotask.app.data.SavedPlaceRepository
import com.geotask.app.data.TaskDatabase
import com.geotask.app.data.TaskRepository
import com.geotask.app.reminder.AlarmScheduler
import com.geotask.app.reminder.GeofenceHelper
import com.geotask.app.reminder.NotificationHelper
import androidx.glance.appwidget.updateAll
import com.geotask.app.widget.TikWidget

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
