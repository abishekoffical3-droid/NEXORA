package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object NotificationHelper {

    const val CHANNEL_REMINDERS = "nexora_reminders"
    const val CHANNEL_FOCUS = "nexora_focus"
    const val CHANNEL_MOTIVATION = "nexora_motivation"
    const val CHANNEL_ALARMS = "nexora_alarms"
    const val CHANNEL_MISSED_TASKS = "nexora_missed_tasks"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Task & Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and wake alerts for tasks and habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val missedTasksChannel = NotificationChannel(
                CHANNEL_MISSED_TASKS,
                "Missed & Overdue Tasks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled tasks that were missed or overdue"
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders & Habits",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task deadlines and habit check-in reminders"
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Focus Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Focus timer and interval completion alerts"
            }

            val motivationChannel = NotificationChannel(
                CHANNEL_MOTIVATION,
                "Daily Motivation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Evening inspiration and streak reflection"
            }

            notificationManager.createNotificationChannels(
                listOf(alarmChannel, missedTasksChannel, reminderChannel, focusChannel, motivationChannel)
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSound(defaultSound)
            .setPriority(
                if (channelId == CHANNEL_ALARMS || channelId == CHANNEL_FOCUS || channelId == CHANNEL_MISSED_TASKS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
