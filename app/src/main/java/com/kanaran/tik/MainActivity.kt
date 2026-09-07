package com.kanaran.tik

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kanaran.tik.reminder.NotificationHelper
import com.kanaran.tik.ui.navigation.NavGraph
import com.kanaran.tik.ui.theme.TikTheme
import com.kanaran.tik.util.PermissionUtils
import com.kanaran.tik.viewmodel.TaskViewModel
import com.kanaran.tik.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {

    private val app get() = application as TikApplication

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(app.repository, app.savedPlaceRepository)
    }

    // Re-read from the OS after every permission dialog closes so the UI banner updates.
    private var permissionRefreshTick by mutableStateOf(0)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRefreshTick++
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionRefreshTick++
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRefreshTick++
        }

    /**
     * A task id delivered by a notification tap or a widget row tap. Kept as state rather
     * than read once, because this activity is `singleTop`: when it's already running the
     * system calls [onNewIntent] instead of [onCreate], and a plain local would drop it.
     */
    private var pendingTaskId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingTaskId = intent.taskIdExtra()

        setContent {
            TikTheme {
                // Reading this makes the composition recompute the permission
                // flags below whenever a permission dialog result comes back.
                @Suppress("UNUSED_EXPRESSION") permissionRefreshTick

                NavGraph(
                    viewModel = viewModel,
                    openTaskId = pendingTaskId,
                    onTaskOpened = { pendingTaskId = null },
                    hasNotificationPermission = PermissionUtils.hasNotificationPermission(this),
                    hasForegroundLocationPermission = PermissionUtils.hasForegroundLocationPermission(this),
                    hasBackgroundLocationPermission = PermissionUtils.hasBackgroundLocationPermission(this),
                    canScheduleExactAlarms = app.alarmScheduler.canScheduleExactAlarms(),
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestLocationPermission = ::requestLocationPermission,
                    onRequestBackgroundLocationPermission = ::requestBackgroundLocationPermission,
                    onRequestExactAlarmPermission = ::openExactAlarmSettings
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep getIntent() in sync for anything that reads it later, then route the deep link.
        setIntent(intent)
        pendingTaskId = intent.taskIdExtra()
    }

    private fun Intent.taskIdExtra(): Long? =
        getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L).takeIf { it != -1L }

    override fun onResume() {
        super.onResume()
        // Catches permission/exact-alarm grants made from the system Settings screen.
        permissionRefreshTick++
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }
}
