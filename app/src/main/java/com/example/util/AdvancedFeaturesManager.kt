package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class AdvancedHubState(
    // Water
    val waterIntakeMl: Int = 0,
    val waterGoalMl: Int = 3000,
    // Sleep
    val bedtime: String = "23:00",
    val wakeTime: String = "07:00",
    val sleepDurationHours: Float = 8.0f,
    val sleepQualityScore: Int = 85, // 0-100
    // Gamification XP
    val totalXp: Int = 150,
    val currentLevel: Int = 1,
    val levelTitle: String = "Nexora Novice",
    val xpForNextLevel: Int = 300,
    // Daily Nutrition Target
    val consumedCalories: Int = 0,
    val targetCalories: Int = 2200,
    val consumedProteinGrams: Int = 0,
    val targetProteinGrams: Int = 120,
    // Ambient Study Sound
    val isAmbientPlaying: Boolean = false,
    val activeSoundTrack: String = "None",
    // 🧘 Feature 3: Mood & Emotional Energy
    val currentMoodEmoji: String = "⚡",
    val currentMoodLabel: String = "Energetic",
    val moodReflection: String = "Feeling charged and ready to hit high-impact goals.",
    // ⏱️ Feature 4: Anti-Distraction Zen Focus Mode
    val isZenModeActive: Boolean = false,
    val zenDurationMinutes: Int = 25,
    val zenSecondsRemaining: Int = 25 * 60,
    // 🏅 Feature 5: Badges & Achievements
    val unlockedBadges: List<String> = listOf("First Step", "Hydration Starter", "Night Owl"),
    // 📱 Feature 2: Quick Widget Refresh Timestamp
    val lastWidgetSyncTime: Long = System.currentTimeMillis()
)

class AdvancedFeaturesManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nexora_adv_hub", Context.MODE_PRIVATE)

    private val _hubState = MutableStateFlow(loadState())
    val hubState: StateFlow<AdvancedHubState> = _hubState.asStateFlow()

    init {
        checkDateRollover()
        _hubState.value = loadState()
    }

    private fun todayKey(): String = LocalDate.now().toString()

    private fun checkDateRollover() {
        val today = todayKey()
        val lastDate = prefs.getString("last_hub_date", "")
        if (lastDate != today) {
            // New day reset for water and food calories
            prefs.edit()
                .putString("last_hub_date", today)
                .putInt("water_$today", 0)
                .putInt("food_cals_$today", 0)
                .putInt("food_prot_$today", 0)
                .apply()
        }
    }

    private fun loadState(): AdvancedHubState {
        val today = todayKey()
        val water = prefs.getInt("water_$today", 1250)
        val waterGoal = prefs.getInt("water_goal", 3000)
        val bedtime = prefs.getString("bedtime", "23:00") ?: "23:00"
        val wakeTime = prefs.getString("waketime", "07:00") ?: "07:00"
        val sleepHours = prefs.getFloat("sleep_hours", 7.5f)
        val sleepQuality = prefs.getInt("sleep_quality", 88)

        val xp = prefs.getInt("total_xp", 250)
        val level = (xp / 300) + 1
        val levelTitle = when (level) {
            1 -> "Nexora Novice"
            2 -> "Habit Apprentice"
            3 -> "Focus Specialist"
            4 -> "Productivity Warrior"
            5 -> "Discipline Master"
            else -> "Nexora Legend"
        }

        val cals = prefs.getInt("food_cals_$today", 1450)
        val targetCals = prefs.getInt("target_cals", 2200)
        val protein = prefs.getInt("food_prot_$today", 75)
        val targetProtein = prefs.getInt("target_protein", 120)

        val moodEmoji = prefs.getString("mood_emoji", "⚡") ?: "⚡"
        val moodLabel = prefs.getString("mood_label", "Energetic") ?: "Energetic"
        val moodNote = prefs.getString("mood_note", "Feeling charged and ready to hit high-impact goals.") ?: ""
        val savedBadges = prefs.getStringSet("badges", setOf("First Step", "Hydration Starter", "Night Owl"))?.toList()
            ?: listOf("First Step", "Hydration Starter", "Night Owl")

        return AdvancedHubState(
            waterIntakeMl = water,
            waterGoalMl = waterGoal,
            bedtime = bedtime,
            wakeTime = wakeTime,
            sleepDurationHours = sleepHours,
            sleepQualityScore = sleepQuality,
            totalXp = xp,
            currentLevel = level,
            levelTitle = levelTitle,
            xpForNextLevel = level * 300,
            consumedCalories = cals,
            targetCalories = targetCals,
            consumedProteinGrams = protein,
            targetProteinGrams = targetProtein,
            isAmbientPlaying = false,
            activeSoundTrack = "None",
            currentMoodEmoji = moodEmoji,
            currentMoodLabel = moodLabel,
            moodReflection = moodNote,
            unlockedBadges = savedBadges
        )
    }

    fun setMood(emoji: String, label: String, reflection: String) {
        prefs.edit()
            .putString("mood_emoji", emoji)
            .putString("mood_label", label)
            .putString("mood_note", reflection)
            .apply()
        addXp(15) // bonus XP for mindful reflection
        _hubState.value = _hubState.value.copy(
            currentMoodEmoji = emoji,
            currentMoodLabel = label,
            moodReflection = reflection
        )
    }

    fun startZenMode(minutes: Int = 25) {
        _hubState.value = _hubState.value.copy(
            isZenModeActive = true,
            zenDurationMinutes = minutes,
            zenSecondsRemaining = minutes * 60
        )
    }

    fun exitZenMode(completed: Boolean = false) {
        if (completed) {
            addXp(50) // reward for completing zen deep work session
            unlockBadge("Zen Master")
        }
        _hubState.value = _hubState.value.copy(isZenModeActive = false)
    }

    fun unlockBadge(badgeName: String) {
        val current = _hubState.value.unlockedBadges.toMutableSet()
        if (current.add(badgeName)) {
            prefs.edit().putStringSet("badges", current).apply()
            addXp(40)
            _hubState.value = _hubState.value.copy(unlockedBadges = current.toList())
        }
    }

    fun syncWidgetTelemetry() {
        _hubState.value = _hubState.value.copy(lastWidgetSyncTime = System.currentTimeMillis())
        com.example.widget.NexoraStepWidgetProvider.updateAllWidgets(context)
    }

    fun addWater(amountMl: Int) {
        val today = todayKey()
        val current = _hubState.value.waterIntakeMl
        val updated = (current + amountMl).coerceAtLeast(0)
        prefs.edit().putInt("water_$today", updated).apply()
        addXp(10) // reward for staying hydrated
        if (updated >= 3000) {
            unlockBadge("Hydration Master")
        }
        _hubState.value = _hubState.value.copy(waterIntakeMl = updated)
        com.example.widget.NexoraStepWidgetProvider.updateAllWidgets(context)
    }

    fun resetWater() {
        val today = todayKey()
        prefs.edit().putInt("water_$today", 0).apply()
        _hubState.value = _hubState.value.copy(waterIntakeMl = 0)
        com.example.widget.NexoraStepWidgetProvider.updateAllWidgets(context)
    }

    fun updateSleep(bed: String, wake: String, hours: Float, quality: Int) {
        prefs.edit()
            .putString("bedtime", bed)
            .putString("waketime", wake)
            .putFloat("sleep_hours", hours)
            .putInt("sleep_quality", quality)
            .apply()
        addXp(25) // reward for logging sleep
        if (hours >= 8.0f) {
            unlockBadge("Deep Restorer")
        }
        _hubState.value = _hubState.value.copy(
            bedtime = bed,
            wakeTime = wake,
            sleepDurationHours = hours,
            sleepQualityScore = quality
        )
    }

    fun logFoodMeal(calories: Int, proteinGrams: Int) {
        val today = todayKey()
        val curCals = _hubState.value.consumedCalories + calories
        val curProt = _hubState.value.consumedProteinGrams + proteinGrams
        prefs.edit()
            .putInt("food_cals_$today", curCals)
            .putInt("food_prot_$today", curProt)
            .apply()
        addXp(20) // reward for logging meal
        _hubState.value = _hubState.value.copy(
            consumedCalories = curCals,
            consumedProteinGrams = curProt
        )
    }

    fun addXp(amount: Int) {
        val newXp = _hubState.value.totalXp + amount
        val newLevel = (newXp / 300) + 1
        val levelTitle = when (newLevel) {
            1 -> "Nexora Novice"
            2 -> "Habit Apprentice"
            3 -> "Focus Specialist"
            4 -> "Productivity Warrior"
            5 -> "Discipline Master"
            else -> "Nexora Legend"
        }
        prefs.edit().putInt("total_xp", newXp).apply()
        if (newLevel >= 3) {
            unlockBadge("Focus Specialist")
        }
        if (newLevel >= 5) {
            unlockBadge("Discipline Master")
        }
        _hubState.value = _hubState.value.copy(
            totalXp = newXp,
            currentLevel = newLevel,
            levelTitle = levelTitle,
            xpForNextLevel = newLevel * 300
        )
    }

    fun toggleAmbientSound(soundName: String) {
        if (_hubState.value.isAmbientPlaying && _hubState.value.activeSoundTrack == soundName) {
            _hubState.value = _hubState.value.copy(isAmbientPlaying = false, activeSoundTrack = "None")
        } else {
            _hubState.value = _hubState.value.copy(isAmbientPlaying = true, activeSoundTrack = soundName)
        }
    }
}
