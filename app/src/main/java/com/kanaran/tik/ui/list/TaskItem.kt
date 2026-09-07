package com.kanaran.tik.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kanaran.tik.data.RepeatType
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.Task
import com.kanaran.tik.data.TriggerType
import com.kanaran.tik.ui.components.BrutalistCheckbox
import com.kanaran.tik.ui.components.HardShadow
import com.kanaran.tik.ui.components.formatEpochDay
import com.kanaran.tik.ui.components.formatMinutesOfDay
import com.kanaran.tik.ui.theme.Ink

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
    streakDays: Int = 0
) {
    val hasTime = task.isTimeReminderEnabled && task.timeOfDayMinutes != null
    val hasPlace = task.isLocationReminderEnabled
    val repeat = RepeatType.fromStorage(task.repeatType)

    val isChecked = if (repeat == RepeatType.ONCE) {
        !task.isActive
    } else {
        task.lastCompletedEpochDay == ScheduleUtil.todayEpochDay()
    }

    HardShadow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer { rotationZ = rotationDegrees },
        offset = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface))
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrutalistCheckbox(checked = isChecked, onCheckedChange = onToggleCompleted)

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                )
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasTime || hasPlace) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (hasTime) {
                            val timeText = if (task.isCombined && task.windowEndTimeOfDayMinutes != null) {
                                "${formatMinutesOfDay(task.timeOfDayMinutes!!)}-${formatMinutesOfDay(task.windowEndTimeOfDayMinutes)}"
                            } else {
                                formatMinutesOfDay(task.timeOfDayMinutes!!)
                            }
                            TagChip(icon = Icons.Filled.Schedule, text = timeText, filled = false)
                        }
                        if (hasPlace) {
                            val triggerLabel = when (TriggerType.fromStorage(task.triggerType)) {
                                TriggerType.ON_ARRIVE -> "ARRIVE"
                                TriggerType.ON_LEAVE -> "LEAVE"
                                TriggerType.BOTH -> "ARRIVE/LEAVE"
                            }
                            TagChip(
                                icon = Icons.Filled.LocationOn,
                                text = "$triggerLabel · ${(task.locationName ?: "LOCATION").uppercase()}",
                                filled = true
                            )
                        }
                    }
                }
                val repeatText = repeatSummary(task)
                if (repeatText != null || streakDays > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (repeatText != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.EventRepeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(repeatText.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (streakDays > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    "$streakDays DAY${if (streakDays == 1) "" else "S"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun repeatSummary(task: Task): String? = when (RepeatType.fromStorage(task.repeatType)) {
    RepeatType.ONCE -> task.singleDateEpochDay?.let { formatEpochDay(it) }
    RepeatType.DAILY -> "Every day"
    RepeatType.WEEKLY_CUSTOM -> {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val chosen = labels.filterIndexed { index, _ -> (task.weekdaysMask and (1 shl index)) != 0 }
        if (chosen.isEmpty()) null else chosen.joinToString(", ")
    }
    RepeatType.DATE_RANGE -> {
        val start = task.rangeStartEpochDay
        val end = task.rangeEndEpochDay
        if (start != null && end != null) "${formatEpochDay(start)} - ${formatEpochDay(end)}" else null
    }
    RepeatType.CUSTOM_DATES -> {
        val count = task.customDatesList().size
        if (count == 0) null else "$count chosen date${if (count == 1) "" else "s"}"
    }
}

@Composable
private fun TagChip(icon: ImageVector, text: String, filled: Boolean) {
    // On the accent fill, content stays literal ink (yellow+near-black always reads); on the
    // plain surface, it must follow the theme so it doesn't vanish in dark mode.
    val contentColor = if (filled) Ink else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .background(if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, if (filled) Ink else MaterialTheme.colorScheme.onSurface))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}
