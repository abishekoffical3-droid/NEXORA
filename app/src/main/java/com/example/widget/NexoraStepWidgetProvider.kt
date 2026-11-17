package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.time.LocalDate
import java.util.Locale

class NexoraStepWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_STEPS"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NexoraStepWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (widgetId in widgetIds) {
                    updateSingleWidget(context, appWidgetManager, widgetId)
                }
            } catch (_: Exception) {}
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val today = LocalDate.now().toString()
            val stepPrefs = context.getSharedPreferences("nexora_steps", Context.MODE_PRIVATE)
            val hubPrefs = context.getSharedPreferences("nexora_adv_hub", Context.MODE_PRIVATE)

            val steps = stepPrefs.getInt("steps_$today", 0)
            val goal = stepPrefs.getInt("step_goal", 10000).coerceAtLeast(1)
            val distanceKm = steps * 0.000762f
            val caloriesKcal = (steps * 0.04f).toInt()
            val waterMl = hubPrefs.getInt("water_$today", 0)
            val percent = ((steps.toFloat() / goal.toFloat()) * 100).toInt().coerceIn(0, 100)

            val views = RemoteViews(context.packageName, R.layout.widget_nexora_steps)
            views.setTextViewText(R.id.widget_steps_count, String.format(Locale.US, "%,d", steps))
            views.setTextViewText(R.id.widget_steps_goal, "/ ${String.format(Locale.US, "%,d", goal)} steps")
            views.setTextViewText(R.id.widget_percent_text, "$percent%")
            views.setProgressBar(R.id.widget_progress_bar, 100, percent, false)
            views.setTextViewText(R.id.widget_distance_text, String.format(Locale.US, "%.2f km", distanceKm))
            views.setTextViewText(R.id.widget_calories_text, "$caloriesKcal kcal")
            views.setTextViewText(R.id.widget_water_text, "💧 $waterMl ml")

            // Open NEXORA on widget click
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)

            // Refresh action click
            val refreshIntent = Intent(context, NexoraStepWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 1, refreshIntent, pendingFlags)
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
