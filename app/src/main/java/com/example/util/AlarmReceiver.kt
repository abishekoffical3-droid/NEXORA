package com.example.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: "task_alarm"
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Scheduled Alarm"
        val note = intent.getStringExtra("EXTRA_NOTE") ?: "It's time to tackle your planned goal in NEXORA!"
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", taskId.hashCode())

        val alarmSettings = AlarmSettingsStore.getInstance(context).state.value

        // Play configured built-in alarm sound or local custom music
        AlarmSoundManager.playAlarmSound(
            context = context,
            sound = alarmSettings.selectedSound,
            customUriString = alarmSettings.customMusicUri,
            isPreview = false
        )

        // Vibrate if enabled
        if (alarmSettings.vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 700), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 500, 250, 500, 250, 700), -1)
                    }
                }
            } catch (_: Exception) {}
        }

        // Build high-priority notification with Stop Sound action
        if (!NotificationHelper.hasNotificationPermission(context)) return

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(context, notificationId + 1, openAppIntent, pendingFlags)

        val stopIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val stopPending = PendingIntent.getBroadcast(context, notificationId + 2, stopIntent, pendingFlags)

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $title")
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openPending)
            .addAction(0, "Stop Alarm", stopPending)
            .setDeleteIntent(stopPending)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }
}
