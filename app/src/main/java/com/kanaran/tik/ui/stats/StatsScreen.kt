package com.kanaran.tik.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kanaran.tik.data.RepeatType
import com.kanaran.tik.data.ScheduleUtil
import com.kanaran.tik.data.StreakUtil
import com.kanaran.tik.data.Task
import com.kanaran.tik.ui.components.HardShadow
import com.kanaran.tik.viewmodel.TaskViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: TaskViewModel, onBack: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val completions by viewModel.completions.collectAsState()
    val today = ScheduleUtil.todayEpochDay()

    val repeatingTasks = tasks.filter { RepeatType.fromStorage(it.repeatType) != RepeatType.ONCE }
    val streaks = repeatingTasks.associate { it.id to StreakUtil.currentStreak(it, completions[it.id] ?: emptySet(), today) }
    val bestStreak = streaks.values.maxOrNull() ?: 0
    val totalCompletions = completions.values.sumOf { it.size }
    val weeklyRates = repeatingTasks.filter { it.isActive }
        .map { StreakUtil.completionRate(it, completions[it.id] ?: emptySet(), today, windowDays = 7) }
    val weeklyAverage = if (weeklyRates.isEmpty()) 0 else (weeklyRates.average() * 100).roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("STATS", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(label = "ACTIVE TASKS", value = tasks.count { it.isActive }.toString(), modifier = Modifier.weight(1f))
                    StatTile(label = "BEST STREAK", value = "$bestStreak", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(label = "TOTAL DONE", value = "$totalCompletions", modifier = Modifier.weight(1f))
                    StatTile(label = "THIS WEEK", value = "$weeklyAverage%", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (repeatingTasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No repeating tasks yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Streaks only apply to tasks that repeat — Daily, Weekdays, a date range, or custom dates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(repeatingTasks, key = { it.id }) { task ->
                        TaskStreakCard(
                            task = task,
                            streak = streaks[task.id] ?: 0,
                            completedDays = completions[task.id] ?: emptySet(),
                            today = today
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    HardShadow(modifier = modifier, offset = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
                .padding(14.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskStreakCard(task: Task, streak: Int, completedDays: Set<Long>, today: Long) {
    HardShadow(offset = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (streak == 0) "—" else "$streak DAY${if (streak == 1) "" else "S"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (offset in 6 downTo 0) {
                    val day = today - offset
                    val scheduled = ScheduleUtil.isScheduledDay(task, day)
                    val completed = completedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background)
                            .let { m ->
                                if (scheduled) m.border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)) else m
                            }
                    )
                }
            }
        }
    }
}
