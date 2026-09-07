package com.kanaran.tik.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kanaran.tik.ui.editor.AddEditTaskScreen
import com.kanaran.tik.ui.list.TaskListScreen
import com.kanaran.tik.ui.stats.StatsScreen
import com.kanaran.tik.viewmodel.TaskViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_EDITOR = "editor"
private const val ROUTE_STATS = "stats"
private const val ARG_TASK_ID = "taskId"

@Composable
fun NavGraph(
    viewModel: TaskViewModel,
    openTaskId: Long?,
    onTaskOpened: () -> Unit,
    hasNotificationPermission: Boolean,
    hasForegroundLocationPermission: Boolean,
    hasBackgroundLocationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundLocationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            TaskListScreen(
                viewModel = viewModel,
                hasNotificationPermission = hasNotificationPermission,
                hasForegroundLocationPermission = hasForegroundLocationPermission,
                hasBackgroundLocationPermission = hasBackgroundLocationPermission,
                canScheduleExactAlarms = canScheduleExactAlarms,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestBackgroundLocationPermission = onRequestBackgroundLocationPermission,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                onAddTask = { navController.navigate("$ROUTE_EDITOR/-1") },
                onEditTask = { taskId -> navController.navigate("$ROUTE_EDITOR/$taskId") },
                onOpenStats = { navController.navigate(ROUTE_STATS) }
            )
        }
        composable(
            route = "$ROUTE_EDITOR/{$ARG_TASK_ID}",
            arguments = listOf(navArgument(ARG_TASK_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong(ARG_TASK_ID) ?: -1L
            AddEditTaskScreen(
                viewModel = viewModel,
                taskId = if (taskId == -1L) null else taskId,
                hasForegroundLocationPermission = hasForegroundLocationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onDone = { navController.popBackStack() }
            )
        }
        composable(ROUTE_STATS) {
            StatsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }

    // Opens the task a notification or widget row pointed at. Runs for both a cold start and
    // a tap while the app is already open, so there's a single path for either. Popping back
    // to the list first keeps repeated taps from stacking editors, and leaves Back going to
    // the list rather than straight out of the app.
    LaunchedEffect(openTaskId) {
        val taskId = openTaskId ?: return@LaunchedEffect
        navController.navigate("$ROUTE_EDITOR/$taskId") {
            popUpTo(ROUTE_LIST)
            launchSingleTop = true
        }
        onTaskOpened()
    }
}
