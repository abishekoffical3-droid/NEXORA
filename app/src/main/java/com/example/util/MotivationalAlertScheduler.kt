package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object MotivationalAlertScheduler {

    private const val MORNING_ALARM_REQ = 8001
    private const val EVENING_ALARM_REQ = 8002

    fun scheduleDailyAlerts(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Morning Briefing at 8:00 AM
        scheduleAtHour(
            context = context,
            alarmManager = alarmManager,
            requestCode = MORNING_ALARM_REQ,
            targetHour = 8,
            targetMinute = 0,
            title = "🌅 Good Morning! Ready to Conquer?",
            message = "Check today's task agenda, review your habits, and hit your 10,000 steps with Ricky."
        )

        // 2. Evening Check-in at 8:00 PM (20:00)
        scheduleAtHour(
            context = context,
            alarmManager = alarmManager,
            requestCode = EVENING_ALARM_REQ,
            targetHour = 20,
            targetMinute = 0,
            title = "🌙 Evening Reflection & Wind-Down",
            message = "Great effort today! Mark your habit attendance, hydrate, and log your bedtime recovery."
        )
    }

    private fun scheduleAtHour(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        targetHour: Int,
        targetMinute: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTE", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {}
    }
}
