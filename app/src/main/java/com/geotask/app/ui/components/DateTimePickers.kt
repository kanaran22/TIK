package com.geotask.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A date-only picker dialog, working in epoch days rather than a date+time instant. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
    initialEpochDay: Long?,
    onDismissRequest: () -> Unit,
    onConfirm: (epochDay: Long) -> Unit
) {
    val initialMillis = (initialEpochDay ?: LocalDate.now().toEpochDay())
        .let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val epochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    onConfirm(epochDay)
                } else {
                    onDismissRequest()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}

/** A time-of-day-only picker dialog, returning minutes since midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleTimePickerDialog(
    initialMinutes: Int?,
    title: String = "Select time",
    onDismissRequest: () -> Unit,
    onConfirm: (minutesSinceMidnight: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes ?: 9 * 60) / 60,
        initialMinute = (initialMinutes ?: 9 * 60) % 60,
        is24Hour = false
    )
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.padding(vertical = 16.dp)) { TimePicker(state = state) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
                }
            }
        }
    }
}

fun formatMinutesOfDay(minutes: Int): String {
    val hour24 = minutes / 60
    val minute = minutes % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (hour24 % 12) { 0 -> 12; else -> hour24 % 12 }
    return "%d:%02d %s".format(hour12, minute, amPm)
}

fun formatEpochDay(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}, ${date.year}"
}
