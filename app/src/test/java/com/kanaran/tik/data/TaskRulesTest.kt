package com.kanaran.tik.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The rules the list, the widget and the reminder receivers all share. Several of these
 * cases are regressions for bugs that shipped: a one-off task ticked on the widget could
 * never be unticked there, and a task already marked done still fired its reminder.
 */
class TaskRulesTest {

    private val today = LocalDate.of(2026, 9, 14).toEpochDay()
    private val yesterday = today - 1

    private fun once(isActive: Boolean = true, completedDay: Long? = null) = Task(
        title = "once",
        repeatType = RepeatType.ONCE.name,
        singleDateEpochDay = today,
        isActive = isActive,
        lastCompletedEpochDay = completedDay
    )

    private fun daily(isActive: Boolean = true, completedDay: Long? = null) = Task(
        title = "daily",
        repeatType = RepeatType.DAILY.name,
        isActive = isActive,
        lastCompletedEpochDay = completedDay
    )

    // --- isCheckedToday ------------------------------------------------------------

    @Test
    fun `a one-off task is checked once it is completed, and not before`() {
        assertFalse(TaskRules.isCheckedToday(once(isActive = true), today))
        assertTrue(TaskRules.isCheckedToday(once(isActive = false, completedDay = today), today))
    }

    @Test
    fun `a completed one-off task stays checked on later days`() {
        assertTrue(TaskRules.isCheckedToday(once(isActive = false, completedDay = yesterday), today))
    }

    @Test
    fun `a repeating task is checked only for the day it was completed`() {
        assertTrue(TaskRules.isCheckedToday(daily(completedDay = today), today))
        assertFalse(TaskRules.isCheckedToday(daily(completedDay = yesterday), today))
        assertFalse(TaskRules.isCheckedToday(daily(completedDay = null), today))
    }

    // --- isDueToRemind -------------------------------------------------------------

    @Test
    fun `an active task on a scheduled day is due`() {
        assertTrue(TaskRules.isDueToRemind(daily(), today))
    }

    @Test
    fun `a task already marked done today is not reminded again`() {
        // Regression: the alarm used to fire regardless.
        assertFalse(TaskRules.isDueToRemind(daily(completedDay = today), today))
    }

    @Test
    fun `yesterday's completion does not suppress today's reminder`() {
        assertTrue(TaskRules.isDueToRemind(daily(completedDay = yesterday), today))
    }

    @Test
    fun `an inactive task is never due`() {
        assertFalse(TaskRules.isDueToRemind(daily(isActive = false), today))
        assertFalse(TaskRules.isDueToRemind(once(isActive = false), today))
    }

    @Test
    fun `a task is not due on a day its schedule skips`() {
        val thursdaysOnly = Task(title = "thu", repeatType = RepeatType.WEEKLY_CUSTOM.name, weekdaysMask = 1 shl 3)
        // 2026-09-14 is a Monday.
        assertFalse(TaskRules.isDueToRemind(thursdaysOnly, today))
        assertTrue(TaskRules.isDueToRemind(thursdaysOnly, LocalDate.of(2026, 9, 17).toEpochDay()))
    }

    // --- widget visibility -----------------------------------------------------------

    @Test
    fun `the widget keeps a one-off task visible for the rest of the day it was ticked`() {
        // Regression: it used to vanish from the widget the moment it was ticked.
        assertTrue(TaskRules.isShownOnWidget(once(isActive = false, completedDay = today), today))
    }

    @Test
    fun `the widget drops a completed one-off task the next day`() {
        assertFalse(TaskRules.isShownOnWidget(once(isActive = false, completedDay = yesterday), today))
    }

    @Test
    fun `the widget always shows active tasks, done today or not`() {
        assertTrue(TaskRules.isShownOnWidget(daily(), today))
        assertTrue(TaskRules.isShownOnWidget(daily(completedDay = today), today))
    }

    @Test
    fun `widget order is newest first and hides yesterday's finished one-offs`() {
        val a = daily().copy(title = "a", createdAt = 400)
        val doneB = daily(completedDay = today).copy(title = "b", createdAt = 300)
        val c = once().copy(title = "c", createdAt = 200)
        val doneD = once(isActive = false, completedDay = today).copy(title = "d", createdAt = 100)
        val gone = once(isActive = false, completedDay = yesterday).copy(title = "gone", createdAt = 500)

        val ordered = TaskRules.widgetOrder(listOf(doneD, gone, c, a, doneB), today)

        assertEquals(listOf("a", "b", "c", "d"), ordered.map { it.title })
    }

    @Test
    fun `ticking a task does not move it on the widget`() {
        // Regression: ticked tasks sank below the widget's visible rows, out of reach to untick.
        val tasks = listOf(
            daily().copy(title = "top", createdAt = 3),
            daily().copy(title = "mid", createdAt = 2),
            daily().copy(title = "low", createdAt = 1)
        )
        val before = TaskRules.widgetOrder(tasks, today).map { it.title }
        val afterTickingTop = TaskRules.widgetOrder(
            tasks.map { if (it.title == "top") it.copy(lastCompletedEpochDay = today) else it }, today
        ).map { it.title }

        assertEquals(before, afterTickingTop)
    }
}
