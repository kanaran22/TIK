package com.kanaran.tik.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanaran.tik.data.RepeatType
import com.kanaran.tik.ui.components.BrutalistToggleTag
import com.kanaran.tik.ui.components.SecondaryButton
import com.kanaran.tik.ui.components.SingleDatePickerDialog
import com.kanaran.tik.ui.components.formatEpochDay

private val WEEKDAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun RepeatPickerSection(
    repeatType: RepeatType,
    singleDateEpochDay: Long?,
    weekdaysMask: Int,
    rangeStartEpochDay: Long?,
    rangeEndEpochDay: Long?,
    customDates: List<Long>,
    onRepeatTypeChange: (RepeatType) -> Unit,
    onSingleDateChange: (Long) -> Unit,
    onWeekdaysMaskChange: (Int) -> Unit,
    onRangeChange: (start: Long?, end: Long?) -> Unit,
    onCustomDatesChange: (List<Long>) -> Unit
) {
    var showDatePickerFor by remember { mutableStateOf<DatePickerTarget?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepeatType.entries.forEach { type ->
                BrutalistToggleTag(
                    label = type.label(),
                    selected = repeatType == type,
                    onClick = { onRepeatTypeChange(type) }
                )
            }
        }

        when (repeatType) {
            RepeatType.ONCE -> {
                SecondaryButton(
                    text = (singleDateEpochDay?.let { formatEpochDay(it) } ?: "PICK A DATE").uppercase(),
                    onClick = { showDatePickerFor = DatePickerTarget.Single }
                )
            }
            RepeatType.DAILY -> {
                Text(
                    "Fires every day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RepeatType.WEEKLY_CUSTOM -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAY_LABELS.forEachIndexed { index, label ->
                        val bit = 1 shl index
                        val selected = (weekdaysMask and bit) != 0
                        BrutalistToggleTag(
                            label = label,
                            selected = selected,
                            onClick = {
                                onWeekdaysMaskChange(if (selected) weekdaysMask and bit.inv() else weekdaysMask or bit)
                            }
                        )
                    }
                }
                if (weekdaysMask == 0) {
                    Text(
                        "! PICK AT LEAST ONE DAY.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            RepeatType.DATE_RANGE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        text = (rangeStartEpochDay?.let { formatEpochDay(it) } ?: "START DATE").uppercase(),
                        onClick = { showDatePickerFor = DatePickerTarget.RangeStart }
                    )
                    SecondaryButton(
                        text = (rangeEndEpochDay?.let { formatEpochDay(it) } ?: "END DATE").uppercase(),
                        onClick = { showDatePickerFor = DatePickerTarget.RangeEnd }
                    )
                }
                if (rangeStartEpochDay != null && rangeEndEpochDay != null && rangeEndEpochDay < rangeStartEpochDay) {
                    Text(
                        "! END DATE MUST BE ON OR AFTER THE START DATE.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            RepeatType.CUSTOM_DATES -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customDates.forEach { day ->
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                                    .clickable { onCustomDatesChange(customDates - day) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(formatEpochDay(day).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    SecondaryButton(
                        text = "ADD DATE",
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp)) },
                        onClick = { showDatePickerFor = DatePickerTarget.AddCustom }
                    )
                    if (customDates.isEmpty()) {
                        Text(
                            "! ADD AT LEAST ONE DATE.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    showDatePickerFor?.let { target ->
        val initial = when (target) {
            DatePickerTarget.Single -> singleDateEpochDay
            DatePickerTarget.RangeStart -> rangeStartEpochDay
            DatePickerTarget.RangeEnd -> rangeEndEpochDay
            DatePickerTarget.AddCustom -> null
        }
        SingleDatePickerDialog(
            initialEpochDay = initial,
            onDismissRequest = { showDatePickerFor = null },
            onConfirm = { epochDay ->
                when (target) {
                    DatePickerTarget.Single -> onSingleDateChange(epochDay)
                    DatePickerTarget.RangeStart -> onRangeChange(epochDay, rangeEndEpochDay)
                    DatePickerTarget.RangeEnd -> onRangeChange(rangeStartEpochDay, epochDay)
                    DatePickerTarget.AddCustom -> {
                        if (epochDay !in customDates) onCustomDatesChange((customDates + epochDay).sorted())
                    }
                }
                showDatePickerFor = null
            }
        )
    }
}

private fun RepeatType.label(): String = when (this) {
    RepeatType.ONCE -> "Once"
    RepeatType.DAILY -> "Daily"
    RepeatType.WEEKLY_CUSTOM -> "Weekdays"
    RepeatType.DATE_RANGE -> "Date range"
    RepeatType.CUSTOM_DATES -> "Custom dates"
}

private enum class DatePickerTarget { Single, RangeStart, RangeEnd, AddCustom }
