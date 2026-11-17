package com.example.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.widget.NexoraStepWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var stepTracker: StepTracker

    companion object {
        const val CHANNEL_STEP_SERVICE = "nexora_live_steps"
        const val NOTIFICATION_ID = 4041
        const val ACTION_STOP_SERVICE = "com.example.util.STOP_STEP_SERVICE"

        fun start(context: Context) {
            // On Android 10+, Activity Recognition permission is required for health step tracking
            val hasActivityPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasActivityPerm) {
                // Still start in-app tracking without ForegroundService until permission is granted
                StepTracker.getInstance(context).startTracking()
                return
            }

            val intent = Intent(context, StepCounterService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                StepTracker.getInstance(context).startTracking()
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, StepCounterService::class.java))
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        stepTracker = StepTracker.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stepTracker.setBackgroundServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification(stepTracker.stepStats.value)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            stepTracker.setBackgroundServiceRunning(true)
        } catch (_: SecurityException) {
            stepTracker.setBackgroundServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        stepTracker.startTracking()

        serviceScope.launch {
            stepTracker.stepStats.collect { stats ->
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(stats))
                NexoraStepWidgetProvider.updateAllWidgets(this@StepCounterService)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::stepTracker.isInitialized) {
            stepTracker.setBackgroundServiceRunning(false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_STEP_SERVICE,
                "Background Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent live notification showing today's steps in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(stats: StepStats): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentPending = PendingIntent.getActivity(this, 10, openIntent, flags)

        val stopIntent = Intent(this, StepCounterService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPending = PendingIntent.getService(this, 11, stopIntent, flags)

        val stepsFormatted = String.format(Locale.US, "%,d", stats.stepsToday)
        val goalFormatted = String.format(Locale.US, "%,d", stats.dailyGoal)
        val distFormatted = String.format(Locale.US, "%.2f", stats.distanceKm)

        return NotificationCompat.Builder(this, CHANNEL_STEP_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("👟 Today: $stepsFormatted / $goalFormatted steps")
            .setContentText("$distFormatted km • ${stats.caloriesKcal} kcal • ${stats.activeMinutes} min active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentPending)
            .addAction(0, "Pause", stopPending)
            .build()
    }
}
