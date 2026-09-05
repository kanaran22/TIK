package com.geotask.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geotask.app.data.RepeatType
import com.geotask.app.data.Task
import com.geotask.app.data.TriggerType
import com.geotask.app.ui.components.ConfirmDialog
import com.geotask.app.ui.components.PrimaryButton
import com.geotask.app.ui.components.SectionCard
import com.geotask.app.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: TaskViewModel,
    taskId: Long?,
    hasForegroundLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onDone: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var existingTask by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isTimeReminderEnabled by remember { mutableStateOf(false) }
    var timeOfDayMinutes by remember { mutableStateOf<Int?>(null) }
    var windowEndTimeOfDayMinutes by remember { mutableStateOf<Int?>(null) }

    val savedPlaces by viewModel.savedPlaces.collectAsState()

    var isLocationReminderEnabled by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var radiusMeters by remember { mutableStateOf(150f) }
    var triggerType by remember { mutableStateOf(TriggerType.ON_ARRIVE) }

    var repeatType by remember { mutableStateOf(RepeatType.ONCE) }
    var singleDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var weekdaysMask by remember { mutableStateOf(0) }
    var rangeStartEpochDay by remember { mutableStateOf<Long?>(null) }
    var rangeEndEpochDay by remember { mutableStateOf<Long?>(null) }
    var customDates by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getById(taskId)?.let { task ->
                existingTask = task
                title = task.title
                notes = task.notes
                isTimeReminderEnabled = task.isTimeReminderEnabled
                timeOfDayMinutes = task.timeOfDayMinutes
                windowEndTimeOfDayMinutes = task.windowEndTimeOfDayMinutes
                isLocationReminderEnabled = task.isLocationReminderEnabled
                locationName = task.locationName
                latitude = task.latitude
                longitude = task.longitude
                radiusMeters = task.radiusMeters
                triggerType = TriggerType.fromStorage(task.triggerType)
                repeatType = RepeatType.fromStorage(task.repeatType)
                singleDateEpochDay = task.singleDateEpochDay
                weekdaysMask = task.weekdaysMask
                rangeStartEpochDay = task.rangeStartEpochDay
                rangeEndEpochDay = task.rangeEndEpochDay
                customDates = task.customDatesList()
            }
        }
    }

    val isCombined = isTimeReminderEnabled && isLocationReminderEnabled
    val hasAnyReminder = isTimeReminderEnabled || isLocationReminderEnabled

    val repeatValid = when (repeatType) {
        RepeatType.ONCE -> singleDateEpochDay != null
        RepeatType.DAILY -> true
        RepeatType.WEEKLY_CUSTOM -> weekdaysMask != 0
        RepeatType.DATE_RANGE -> rangeStartEpochDay != null && rangeEndEpochDay != null && rangeEndEpochDay!! >= rangeStartEpochDay!!
        RepeatType.CUSTOM_DATES -> customDates.isNotEmpty()
    }
    val timeValid = !isTimeReminderEnabled || (
        if (isCombined) {
            timeOfDayMinutes != null && windowEndTimeOfDayMinutes != null && windowEndTimeOfDayMinutes!! > timeOfDayMinutes!!
        } else {
            timeOfDayMinutes != null
        }
        )
    val locationValid = !isLocationReminderEnabled || (latitude != null && longitude != null)
    val canSave = title.isNotBlank() && (!hasAnyReminder || repeatValid) && timeValid && locationValid

    fun save() {
        val task = Task(
            id = existingTask?.id ?: 0L,
            title = title.trim(),
            notes = notes.trim(),
            isActive = existingTask?.isActive ?: true,
            createdAt = existingTask?.createdAt ?: System.currentTimeMillis(),
            repeatType = repeatType.name,
            singleDateEpochDay = singleDateEpochDay,
            weekdaysMask = weekdaysMask,
            rangeStartEpochDay = rangeStartEpochDay,
            rangeEndEpochDay = rangeEndEpochDay,
            customDatesEpochDays = Task.encodeCustomDates(customDates),
            isTimeReminderEnabled = isTimeReminderEnabled,
            timeOfDayMinutes = timeOfDayMinutes,
            windowEndTimeOfDayMinutes = if (isCombined) windowEndTimeOfDayMinutes else null,
            isLocationReminderEnabled = isLocationReminderEnabled && latitude != null && longitude != null,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            triggerType = triggerType.name,
            // A schedule/time/place edit starts the occurrence bookkeeping fresh.
            lastFiredEpochDay = null,
            lastCompletedEpochDay = null
        )
        viewModel.save(task)
        onDone()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "NEW TASK" else "EDIT TASK", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingTask != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                shape = RoundedCornerShape(0.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            SectionCard(
                title = "Remind me at a time",
                checked = isTimeReminderEnabled,
                onCheckedChange = { isTimeReminderEnabled = it }
            ) {
                TimeSection(
                    isCombinedWithLocation = isCombined,
                    timeOfDayMinutes = timeOfDayMinutes,
                    windowEndTimeOfDayMinutes = windowEndTimeOfDayMinutes,
                    onTimeOfDayChange = { timeOfDayMinutes = it },
                    onWindowEndChange = { windowEndTimeOfDayMinutes = it }
                )
            }

            SectionCard(
                title = "Remind me at a place",
                checked = isLocationReminderEnabled,
                onCheckedChange = { isLocationReminderEnabled = it }
            ) {
                if (!hasForegroundLocationPermission) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "! LOCATION PERMISSION IS REQUIRED TO PICK A PLACE.",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall
                        )
                        PrimaryButton(text = "Grant permission", onClick = onRequestLocationPermission)
                    }
                } else {
                    LocationPickerSection(
                        locationName = locationName,
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radiusMeters,
                        triggerType = triggerType,
                        savedPlaces = savedPlaces,
                        onLocationPicked = { name, lat, lng ->
                            locationName = name
                            latitude = lat
                            longitude = lng
                        },
                        onRadiusChange = { radiusMeters = it },
                        onTriggerTypeChange = { triggerType = it },
                        onSavePlace = { name, lat, lng, radius -> viewModel.savePlace(name, lat, lng, radius) },
                        onDeletePlace = { place -> viewModel.deletePlace(place) }
                    )
                }
            }

            if (hasAnyReminder) {
                SectionCard(title = "Repeat") {
                    RepeatPickerSection(
                        repeatType = repeatType,
                        singleDateEpochDay = singleDateEpochDay,
                        weekdaysMask = weekdaysMask,
                        rangeStartEpochDay = rangeStartEpochDay,
                        rangeEndEpochDay = rangeEndEpochDay,
                        customDates = customDates,
                        onRepeatTypeChange = { repeatType = it },
                        onSingleDateChange = { singleDateEpochDay = it },
                        onWeekdaysMaskChange = { weekdaysMask = it },
                        onRangeChange = { start, end -> rangeStartEpochDay = start; rangeEndEpochDay = end },
                        onCustomDatesChange = { customDates = it }
                    )
                }
            }

            PrimaryButton(
                text = "Save",
                onClick = { save() },
                enabled = canSave,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete task?",
            message = "\"${title.ifBlank { "This task" }}\" and its reminders will be removed for good.",
            confirmLabel = "DELETE",
            onConfirm = {
                existingTask?.let { viewModel.delete(it) }
                showDeleteConfirm = false
                onDone()
            },
            onDismissRequest = { showDeleteConfirm = false }
        )
    }
}
