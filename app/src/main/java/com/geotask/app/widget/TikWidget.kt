package com.geotask.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.geotask.app.TikApplication
import com.geotask.app.MainActivity
import com.geotask.app.R
import com.geotask.app.data.RepeatType
import com.geotask.app.data.ScheduleUtil
import com.geotask.app.data.Task
import com.geotask.app.reminder.NotificationHelper
import com.geotask.app.ui.components.formatMinutesOfDay

/**
 * Home screen widget: a glanceable list of active reminders you can check off
 * without opening the app. Any task change made anywhere in the app (or from
 * a notification action) re-renders every placed instance, via the
 * [TaskRepository][com.geotask.app.data.TaskRepository] `widgetUpdater` hook.
 */
class TikWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TikApplication
        val tasks = app.repository.getActiveTasks().take(6)

        provideContent {
            WidgetContent(tasks)
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(R.color.widget_paper))
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.app_name).uppercase(),
                style = TextStyle(color = ColorProvider(R.color.widget_ink), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            AddTaskButton()
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "Nothing on your radar.",
                style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 13.sp)
            )
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                tasks.forEachIndexed { index, task ->
                    TaskRow(task)
                    if (index != tasks.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTaskButton() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_button_bg))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(actionStartActivity(openAppIntent(context, taskId = null)))
    ) {
        Text(
            text = "+ NEW",
            style = TextStyle(color = ColorProvider(R.color.widget_ink), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
    }
}

@Composable
private fun TaskRow(task: Task) {
    val context = LocalContext.current
    val repeat = RepeatType.fromStorage(task.repeatType)
    val isChecked = if (repeat == RepeatType.ONCE) {
        false // an active ONCE task in this list is by definition not done yet
    } else {
        task.lastCompletedEpochDay == ScheduleUtil.todayEpochDay()
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_card_bg))
            .padding(10.dp)
            .clickable(actionStartActivity(openAppIntent(context, taskId = task.id))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(22.dp)
                .background(ImageProvider(if (isChecked) R.drawable.widget_checkbox_checked else R.drawable.widget_checkbox_unchecked))
                .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to task.id))),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Text(text = "✓", style = TextStyle(color = ColorProvider(R.color.widget_ink), fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(if (isChecked) R.color.widget_muted else R.color.widget_ink),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                )
            )
            val subtitle = taskSubtitle(task)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 11.sp)
                )
            }
        }
    }
}

private fun taskSubtitle(task: Task): String? {
    val parts = mutableListOf<String>()
    if (task.isTimeReminderEnabled && task.timeOfDayMinutes != null) {
        parts += if (task.isCombined && task.windowEndTimeOfDayMinutes != null) {
            "${formatMinutesOfDay(task.timeOfDayMinutes)}-${formatMinutesOfDay(task.windowEndTimeOfDayMinutes)}"
        } else {
            formatMinutesOfDay(task.timeOfDayMinutes)
        }
    }
    if (task.isLocationReminderEnabled) {
        parts += task.locationName ?: "a place"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun openAppIntent(context: Context, taskId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (taskId != null) putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
    }

private val TaskIdKey = ActionParameters.Key<Long>("task_id")

/** Runs when a task row's checkbox is tapped from the widget, mirroring [com.geotask.app.viewmodel.TaskViewModel.toggleChecked]. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val app = context.applicationContext as TikApplication
        val task = app.repository.getById(taskId) ?: return

        val isChecked = if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
            false
        } else {
            task.lastCompletedEpochDay == ScheduleUtil.todayEpochDay()
        }
        val newChecked = !isChecked

        if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
            app.repository.setSeriesActive(task, active = !newChecked)
        } else {
            app.repository.setCompletedToday(task, completed = newChecked)
        }
        // TaskRepository's widgetUpdater already re-renders every placed widget instance.
    }
}
