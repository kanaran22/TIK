package com.geotask.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.geotask.app.ui.components.SecondaryButton
import com.geotask.app.ui.components.SingleTimePickerDialog
import com.geotask.app.ui.components.formatMinutesOfDay

/**
 * The time-picking UI inside the "Remind me at a time" section. When a location
 * reminder is also enabled, this becomes a start–end arming window instead of a
 * single fire time.
 */
@Composable
fun TimeSection(
    isCombinedWithLocation: Boolean,
    timeOfDayMinutes: Int?,
    windowEndTimeOfDayMinutes: Int?,
    onTimeOfDayChange: (Int) -> Unit,
    onWindowEndChange: (Int) -> Unit
) {
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    if (!isCombinedWithLocation) {
        SecondaryButton(
            text = (timeOfDayMinutes?.let { formatMinutesOfDay(it) } ?: "Pick a time").uppercase(),
            onClick = { editingStart = true }
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Only checked for a place match between these two times.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = (timeOfDayMinutes?.let { "FROM ${formatMinutesOfDay(it)}" } ?: "FROM...").uppercase(),
                    onClick = { editingStart = true }
                )
                SecondaryButton(
                    text = (windowEndTimeOfDayMinutes?.let { "UNTIL ${formatMinutesOfDay(it)}" } ?: "UNTIL...").uppercase(),
                    onClick = { editingEnd = true }
                )
            }
            if (timeOfDayMinutes != null && windowEndTimeOfDayMinutes != null && windowEndTimeOfDayMinutes <= timeOfDayMinutes) {
                Text(
                    "! THE \"UNTIL\" TIME MUST BE AFTER THE \"FROM\" TIME.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (editingStart) {
        SingleTimePickerDialog(
            initialMinutes = timeOfDayMinutes,
            title = if (isCombinedWithLocation) "From" else "Select time",
            onDismissRequest = { editingStart = false },
            onConfirm = {
                onTimeOfDayChange(it)
                editingStart = false
            }
        )
    }
    if (editingEnd) {
        SingleTimePickerDialog(
            initialMinutes = windowEndTimeOfDayMinutes,
            title = "Until",
            onDismissRequest = { editingEnd = false },
            onConfirm = {
                onWindowEndChange(it)
                editingEnd = false
            }
        )
    }
}
