package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nexora_preferences")

data class NexoraPreferences(
    val dailyMotivationReminder: Boolean = true,
    val taskReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val focusNotifications: Boolean = true,
    val aiPersonalizationAllowed: Boolean = true,
    val reduceAnimations: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val themeMode: String = "DARK",
    val wakeTime: String = "07:00",
    val sleepTime: String = "23:00",
    val dailyFocusTargetMinutes: Int = 60,
    val onboardingCompleted: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DAILY_MOTIVATION = booleanPreferencesKey("daily_motivation")
        val TASK_REMINDERS = booleanPreferencesKey("task_reminders")
        val HABIT_REMINDERS = booleanPreferencesKey("habit_reminders")
        val FOCUS_NOTIFICATIONS = booleanPreferencesKey("focus_notifications")
        val AI_PERSONALIZATION = booleanPreferencesKey("ai_personalization")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WAKE_TIME = stringPreferencesKey("wake_time")
        val SLEEP_TIME = stringPreferencesKey("sleep_time")
        val FOCUS_TARGET = intPreferencesKey("focus_target")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val preferencesFlow: Flow<NexoraPreferences> = context.dataStore.data.map { prefs ->
        NexoraPreferences(
            dailyMotivationReminder = prefs[Keys.DAILY_MOTIVATION] ?: true,
            taskReminders = prefs[Keys.TASK_REMINDERS] ?: true,
            habitReminders = prefs[Keys.HABIT_REMINDERS] ?: true,
            focusNotifications = prefs[Keys.FOCUS_NOTIFICATIONS] ?: true,
            aiPersonalizationAllowed = prefs[Keys.AI_PERSONALIZATION] ?: true,
            reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
            wakeTime = prefs[Keys.WAKE_TIME] ?: "07:00",
            sleepTime = prefs[Keys.SLEEP_TIME] ?: "23:00",
            dailyFocusTargetMinutes = prefs[Keys.FOCUS_TARGET] ?: 60,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setDailyMotivation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_MOTIVATION] = enabled }
    }

    suspend fun setTaskReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TASK_REMINDERS] = enabled }
    }

    suspend fun setHabitReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HABIT_REMINDERS] = enabled }
    }

    suspend fun setFocusNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FOCUS_NOTIFICATIONS] = enabled }
    }

    suspend fun setAiPersonalization(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_PERSONALIZATION] = enabled }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_ANIMATIONS] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateSchedule(wake: String, sleep: String, focusTarget: Int) {
        context.dataStore.edit {
            it[Keys.WAKE_TIME] = wake
            it[Keys.SLEEP_TIME] = sleep
            it[Keys.FOCUS_TARGET] = focusTarget
        }
    }
}
