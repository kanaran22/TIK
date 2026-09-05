package com.geotask.app.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geotask.app.data.RepeatType
import com.geotask.app.data.ScheduleUtil
import com.geotask.app.data.StreakUtil
import com.geotask.app.data.Task
import com.geotask.app.ui.components.BrutalistSnackbar
import com.geotask.app.ui.components.BrutalistToggleTag
import com.geotask.app.ui.components.HardShadow
import com.geotask.app.ui.components.SecondaryButton
import com.geotask.app.ui.theme.Ink
import com.geotask.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private enum class SortOption(val label: String) { NEXT("Next"), NAME("Name"), NEWEST("Newest") }
private enum class FilterOption(val label: String) { ALL("All"), TIME("Time"), PLACE("Place"), BOTH("Both") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    hasNotificationPermission: Boolean,
    hasForegroundLocationPermission: Boolean,
    hasBackgroundLocationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundLocationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onOpenStats: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val completions by viewModel.completions.collectAsState()
    val activeCount = tasks.count { it.isActive }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NEXT) }
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }

    val visibleTasks = remember(tasks, searchQuery, sortOption, filterOption) {
        filterAndSort(tasks, searchQuery, sortOption, filterOption)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> BrutalistSnackbar(data) } },
        floatingActionButton = {
            HardShadow(offset = 5.dp) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(BorderStroke(3.dp, Ink))
                        .clickable(onClick = onAddTask)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Ink)
                        Text("NEW TASK", color = Ink, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            HeroHeader(activeCount = activeCount, onOpenStats = onOpenStats)

            PermissionBanners(
                hasNotificationPermission = hasNotificationPermission,
                hasForegroundLocationPermission = hasForegroundLocationPermission,
                hasBackgroundLocationPermission = hasBackgroundLocationPermission,
                canScheduleExactAlarms = canScheduleExactAlarms,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestBackgroundLocationPermission = onRequestBackgroundLocationPermission,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission
            )

            if (tasks.isNotEmpty()) {
                SearchSortFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    sortOption = sortOption,
                    onSortOptionChange = { sortOption = it },
                    filterOption = filterOption,
                    onFilterOptionChange = { filterOption = it }
                )
            }

            if (tasks.isEmpty()) {
                EmptyState()
            } else if (visibleTasks.isEmpty()) {
                NoMatchesState()
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)) {
                    itemsIndexed(visibleTasks, key = { _, task -> task.id }) { index, task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart ||
                                    value == SwipeToDismissBoxValue.StartToEnd
                                ) {
                                    val completedDays = completions[task.id] ?: emptySet()
                                    viewModel.delete(task)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${task.title}\" deleted",
                                            actionLabel = "UNDO"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restore(task, completedDays)
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                        .border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.padding(end = 28.dp)
                                    )
                                }
                            }
                        ) {
                            val streak = if (RepeatType.fromStorage(task.repeatType) != RepeatType.ONCE) {
                                StreakUtil.currentStreak(task, completions[task.id] ?: emptySet(), ScheduleUtil.todayEpochDay())
                            } else {
                                0
                            }
                            TaskItem(
                                task = task,
                                streakDays = streak,
                                rotationDegrees = if (index % 2 == 0) -0.6f else 0.6f,
                                onClick = { onEditTask(task.id) },
                                onToggleCompleted = { checked -> viewModel.toggleChecked(task, checked) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun filterAndSort(
    tasks: List<Task>,
    query: String,
    sort: SortOption,
    filter: FilterOption
): List<Task> {
    val q = query.trim()
    var result = tasks.filter { task ->
        val matchesQuery = q.isEmpty() ||
            task.title.contains(q, ignoreCase = true) ||
            task.notes.contains(q, ignoreCase = true) ||
            (task.locationName?.contains(q, ignoreCase = true) == true)
        val matchesFilter = when (filter) {
            FilterOption.ALL -> true
            FilterOption.TIME -> task.isTimeReminderEnabled && !task.isLocationReminderEnabled
            FilterOption.PLACE -> task.isLocationReminderEnabled && !task.isTimeReminderEnabled
            FilterOption.BOTH -> task.isCombined
        }
        matchesQuery && matchesFilter
    }
    result = when (sort) {
        SortOption.NEXT -> result.sortedWith(
            compareByDescending<Task> { it.isActive }
                .thenBy { nextOccurrenceMillis(it) ?: Long.MAX_VALUE }
        )
        SortOption.NAME -> result.sortedWith(compareByDescending<Task> { it.isActive }.thenBy { it.title.lowercase() })
        SortOption.NEWEST -> result.sortedWith(compareByDescending<Task> { it.isActive }.thenByDescending { it.createdAt })
    }
    return result
}

/** An approximate "when does this next matter" instant, used purely for sorting. */
private fun nextOccurrenceMillis(task: Task): Long? {
    if (task.isTimeReminderEnabled) return ScheduleUtil.nextTimeReminderMillis(task)
    val today = ScheduleUtil.todayEpochDay()
    val nextDay = ScheduleUtil.nextScheduledEpochDayOnOrAfter(task, today) ?: return null
    return LocalDate.ofEpochDay(nextDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
private fun SearchSortFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    filterOption: FilterOption,
    onFilterOptionChange: (FilterOption) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search tasks") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) },
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterOption.entries.forEach { option ->
                BrutalistToggleTag(
                    label = option.label,
                    selected = filterOption == option,
                    onClick = { onFilterOptionChange(option) }
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(2.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            SortOption.entries.forEach { option ->
                BrutalistToggleTag(
                    label = "↕ ${option.label}",
                    selected = sortOption == option,
                    onClick = { onSortOptionChange(option) }
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(activeCount: Int, onOpenStats: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "TIK",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.graphicsLayer { rotationZ = -1.6f }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(BorderStroke(2.5.dp, Ink))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .graphicsLayer { rotationZ = 1.4f }
                ) {
                    Text(
                        if (activeCount == 0) "ALL CAUGHT UP" else "$activeCount ACTIVE",
                        color = Ink,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onBackground))
                    .clickable(onClick = onOpenStats),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.BarChart, contentDescription = "Stats", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(MaterialTheme.colorScheme.onBackground)
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HardShadow(offset = 4.dp) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface))
                    .graphicsLayer { rotationZ = -3f },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            "NOTHING ON\nYOUR RADAR",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap New Task to set a reminder for a time, a place, or both.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoMatchesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "NO MATCHES",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Try a different search or filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionBanners(
    hasNotificationPermission: Boolean,
    hasForegroundLocationPermission: Boolean,
    hasBackgroundLocationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundLocationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val banners = buildList {
        if (!hasNotificationPermission) {
            add(Triple(Icons.Filled.NotificationsActive, "Allow notifications so reminders can alert you.", onRequestNotificationPermission))
        }
        if (!hasForegroundLocationPermission) {
            add(Triple(Icons.Filled.Place, "Allow location access to use place-based reminders.", onRequestLocationPermission))
        } else if (!hasBackgroundLocationPermission) {
            add(Triple(Icons.Filled.Place, "Allow \"all the time\" location access so place reminders fire in the background.", onRequestBackgroundLocationPermission))
        }
        if (!canScheduleExactAlarms) {
            add(Triple(Icons.Filled.NotificationsActive, "Allow exact alarms so time reminders fire precisely on time.", onRequestExactAlarmPermission))
        }
    }
    if (banners.isEmpty()) return

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        banners.forEach { (icon, message, action) ->
            HardShadow(offset = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.secondary)
                                .border(BorderStroke(2.dp, Ink)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            message,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(text = "ALLOW", onClick = action)
                }
            }
        }
    }
}
