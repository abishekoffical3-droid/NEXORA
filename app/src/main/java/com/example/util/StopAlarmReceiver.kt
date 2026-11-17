package com.example.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AlarmSoundManager.stop()
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", -1)
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId)
        }
    }
}
