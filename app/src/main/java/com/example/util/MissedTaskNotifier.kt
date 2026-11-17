package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.TaskItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

object MissedTaskNotifier {

    private const val TAG = "MissedTaskNotifier"
    private val notifiedTaskIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Inspects active tasks and triggers overdue notifications for tasks whose scheduled deadline has passed.
     */
    fun checkAndNotifyMissedTasks(context: Context, tasks: List<TaskItem>) {
        val today = LocalDate.now()
        val nowTime = LocalTime.now()

        for (task in tasks) {
            if (task.completed) continue
            if (notifiedTaskIds.contains(task.id)) continue

            val dueDateStr = task.dueDate ?: continue
            val taskDueDate = try {
                LocalDate.parse(dueDateStr)
            } catch (_: Exception) {
                null
            } ?: continue

            var isOverdue = false
            var deadlineInfo = dueDateStr

            if (taskDueDate.isBefore(today)) {
                // Past day
                isOverdue = true
            } else if (taskDueDate.isEqual(today) && !task.dueTime.isNullOrBlank()) {
                // Today, check dueTime
                val parsedTime = parseTimeSafe(task.dueTime)
                if (parsedTime != null && parsedTime.isBefore(nowTime)) {
                    isOverdue = true
                    deadlineInfo = "Today at ${task.dueTime}"
                }
            }

            if (isOverdue) {
                notifiedTaskIds.add(task.id)
                Log.d(TAG, "Task '${task.title}' is overdue ($deadlineInfo). Sending missed task alert.")
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = task.id.hashCode() + 9999,
                    channelId = NotificationHelper.CHANNEL_MISSED_TASKS,
                    title = "⚠️ Missed Task: ${task.title}",
                    message = "Your scheduled task was due ($deadlineInfo). Open NEXORA to mark attendance or reschedule!"
                )
            }
        }
    }

    private fun parseTimeSafe(timeStr: String): LocalTime? {
        val trimmed = timeStr.trim()
        val formats = listOf(
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("hh:mm a"),
            DateTimeFormatter.ofPattern("h:mm a"),
            DateTimeFormatter.ofPattern("hh:mma"),
            DateTimeFormatter.ofPattern("h:mma")
        )
        for (format in formats) {
            try {
                return LocalTime.parse(trimmed, format)
            } catch (_: Exception) {}
        }
        return null
    }
}
