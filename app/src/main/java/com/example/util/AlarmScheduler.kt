package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Schedules a task or habit alarm for a specific timestamp in epoch milliseconds.
     */
    fun scheduleAlarm(
        context: Context,
        id: String,
        title: String,
        note: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Prevent setting alarms in the past
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule alarm in the past: $triggerAtMillis")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTE", note)
            putExtra("EXTRA_NOTIFICATION_ID", id.hashCode())
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.i(TAG, "Scheduled alarm for '$title' at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm, falling back", e)
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Failed to schedule alarm fallback", fallbackError)
            }
        }
    }

    /**
     * Cancels an existing alarm for the given task ID.
     */
    fun cancelAlarm(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            flags
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled alarm for id: $id")
        }
    }
}
