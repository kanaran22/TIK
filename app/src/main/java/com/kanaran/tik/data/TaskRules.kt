package com.kanaran.tik.data

/**
 * The rules every surface must agree on — the task list, the home screen widget, the
 * notification actions and the reminder receivers. These used to be re-derived in each
 * place, and the copies drifted apart (the widget treated a completed one-off task as
 * "not done" and then hid it, so it could never be unticked there). Keep them here.
 * Pure Kotlin, no Android dependencies.
 */
object TaskRules {

    /**
     * Whether a checkbox for [task] should show as ticked on [today]. A one-off task is
     * done for good once completed; a repeating task is only done for today's occurrence.
     */
    fun isCheckedToday(task: Task, today: Long): Boolean =
        if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
            !task.isActive
        } else {
            task.lastCompletedEpochDay == today
        }

    /**
     * Whether a reminder for [task] should actually be delivered on [today]. Already marking
     * today's occurrence done means you don't get nagged about it later the same day.
     */
    fun isDueToRemind(task: Task, today: Long): Boolean =
        task.isActive &&
            task.lastCompletedEpochDay != today &&
            ScheduleUtil.isScheduledDay(task, today)

    /**
     * Whether the widget lists [task] on [today]: anything still active, plus anything
     * completed today — so a task ticked by mistake stays visible and can be unticked.
     */
    fun isShownOnWidget(task: Task, today: Long): Boolean =
        task.isActive || task.lastCompletedEpochDay == today

    /**
     * Widget order: newest first, and deliberately independent of done state — a task stays
     * exactly where it was when ticked, so the same tap unticks it. Sinking done tasks to the
     * bottom looked tidy but pushed them out of the widget's few visible rows, which is the
     * "can't untick from the widget" bug all over again.
     */
    fun widgetOrder(tasks: List<Task>, today: Long): List<Task> =
        tasks.filter { isShownOnWidget(it, today) }.sortedByDescending { it.createdAt }
}
