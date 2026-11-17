package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class CustomAlarmItem(
    val id: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)

data class AlarmSettingsState(
    val selectedSound: NexoraAlarmSound = NexoraAlarmSound.COSMIC_PULSE,
    val customMusicUri: String? = null,
    val customMusicName: String? = null,
    val vibrationEnabled: Boolean = true,
    val customAlarms: List<CustomAlarmItem> = emptyList()
)

class AlarmSettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("nexora_alarm_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AlarmSettingsState> = _state.asStateFlow()

    companion object {
        private const val KEY_SOUND = "selected_sound"
        private const val KEY_CUSTOM_URI = "custom_music_uri"
        private const val KEY_CUSTOM_NAME = "custom_music_name"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_ALARMS_JSON = "custom_alarms_json"

        @Volatile
        private var INSTANCE: AlarmSettingsStore? = null

        fun getInstance(context: Context): AlarmSettingsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmSettingsStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun load(): AlarmSettingsState {
        val sound = NexoraAlarmSound.fromId(prefs.getString(KEY_SOUND, NexoraAlarmSound.COSMIC_PULSE.id))
        val uri = prefs.getString(KEY_CUSTOM_URI, null)
        val name = prefs.getString(KEY_CUSTOM_NAME, null)
        val vib = prefs.getBoolean(KEY_VIBRATION, true)

        val alarms = mutableListOf<CustomAlarmItem>()
        val jsonStr = prefs.getString(KEY_ALARMS_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    alarms.add(
                        CustomAlarmItem(
                            id = obj.getString("id"),
                            label = obj.getString("label"),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute"),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            } catch (_: Exception) {}
        } else {
            // Sensible starter alarm entries (disabled by default until user enables)
            alarms.add(CustomAlarmItem("wake_up_default", "Morning Wake-Up", 6, 30, false))
            alarms.add(CustomAlarmItem("study_sprint_default", "Deep Study Block", 19, 0, false))
        }

        return AlarmSettingsState(
            selectedSound = sound,
            customMusicUri = uri,
            customMusicName = name,
            vibrationEnabled = vib,
            customAlarms = alarms
        )
    }

    fun selectSound(sound: NexoraAlarmSound) {
        prefs.edit().putString(KEY_SOUND, sound.id).apply()
        _state.value = _state.value.copy(selectedSound = sound)
    }

    fun setCustomMusic(uri: String, displayName: String) {
        prefs.edit()
            .putString(KEY_SOUND, NexoraAlarmSound.CUSTOM_MUSIC.id)
            .putString(KEY_CUSTOM_URI, uri)
            .putString(KEY_CUSTOM_NAME, displayName)
            .apply()
        _state.value = _state.value.copy(
            selectedSound = NexoraAlarmSound.CUSTOM_MUSIC,
            customMusicUri = uri,
            customMusicName = displayName
        )
    }

    fun setVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _state.value = _state.value.copy(vibrationEnabled = enabled)
    }

    fun addOrUpdateAlarm(label: String, hour: Int, minute: Int) {
        val item = CustomAlarmItem(
            id = "alarm_${UUID.randomUUID()}",
            label = label.ifBlank { "NEXORA Alarm" },
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            enabled = true
        )
        val updated = _state.value.customAlarms + item
        saveAlarms(updated)
        scheduleItem(item)
    }

    fun toggleAlarm(alarmId: String, enabled: Boolean) {
        val updated = _state.value.customAlarms.map {
            if (it.id == alarmId) {
                val toggled = it.copy(enabled = enabled)
                if (enabled) scheduleItem(toggled) else AlarmScheduler.cancelAlarm(appContext, toggled.id)
                toggled
            } else it
        }
        saveAlarms(updated)
    }

    fun deleteAlarm(alarmId: String) {
        AlarmScheduler.cancelAlarm(appContext, alarmId)
        val updated = _state.value.customAlarms.filterNot { it.id == alarmId }
        saveAlarms(updated)
    }

    private fun scheduleItem(item: CustomAlarmItem) {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), LocalTime.of(item.hour, item.minute))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val triggerMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        AlarmScheduler.scheduleAlarm(
            context = appContext,
            id = item.id,
            title = item.label,
            note = "NEXORA Scheduled Alarm (${String.format("%02d:%02d", item.hour, item.minute)})",
            triggerAtMillis = triggerMs
        )
    }

    private fun saveAlarms(list: List<CustomAlarmItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("label", item.label)
                    put("hour", item.hour)
                    put("minute", item.minute)
                    put("enabled", item.enabled)
                }
            )
        }
        prefs.edit().putString(KEY_ALARMS_JSON, arr.toString()).apply()
        _state.value = _state.value.copy(customAlarms = list)
    }
}
