package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.widget.NexoraStepWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.math.sqrt

data class StepStats(
    val stepsToday: Int = 0,
    val dailyGoal: Int = 10000,
    val distanceKm: Float = 0f,
    val caloriesKcal: Int = 0,
    val activeMinutes: Int = 0,
    val isSensorAvailable: Boolean = false,
    val isTracking: Boolean = false,
    val isBackgroundServiceRunning: Boolean = false,
    val sensorTypeLabel: String = "Hardware Pedometer",
    val weeklyHistory: List<Pair<String, Int>> = emptyList()
)

class StepTracker private constructor(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("nexora_steps", Context.MODE_PRIVATE)
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _stepStats = MutableStateFlow(loadTodayStats())
    val stepStats: StateFlow<StepStats> = _stepStats.asStateFlow()

    private var isRegistered = false
    @Volatile
    private var backgroundServiceRunning = false

    // Filtered Accelerometer peak-detection state (used only if dedicated pedometer hardware is unavailable)
    private var gravityMagnitude = SensorManager.GRAVITY_EARTH
    private var lastAccelPeakTimeMs = 0L
    private var aboveStepThreshold = false

    companion object {
        private const val TAG = "StepTracker"
        private const val KEY_LAST_RAW_BOOT_STEPS = "last_raw_boot_steps"
        private const val KEY_LAST_BOOT_DAY = "last_boot_day"

        @Volatile
        private var INSTANCE: StepTracker? = null

        fun getInstance(context: Context): StepTracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepTracker(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        checkDateRollover()
        updateStatsState()
    }

    @Synchronized
    fun startTracking() {
        if (isRegistered) return
        checkDateRollover()

        val sensorToUse = when {
            stepCounterSensor != null -> stepCounterSensor
            stepDetectorSensor != null -> stepDetectorSensor
            accelerometerSensor != null -> accelerometerSensor
            else -> null
        }

        if (sensorToUse != null && sensorManager != null) {
            val sampling = if (sensorToUse.type == Sensor.TYPE_ACCELEROMETER) {
                SensorManager.SENSOR_DELAY_GAME
            } else {
                SensorManager.SENSOR_DELAY_NORMAL
            }
            val registered = sensorManager.registerListener(this, sensorToUse, sampling)
            isRegistered = registered

            val label = when (sensorToUse.type) {
                Sensor.TYPE_STEP_COUNTER -> "Hardware Step Counter"
                Sensor.TYPE_STEP_DETECTOR -> "Hardware Step Detector"
                Sensor.TYPE_ACCELEROMETER -> "Smart Gait Accelerometer"
                else -> "Simulator Mode"
            }

            _stepStats.value = _stepStats.value.copy(
                isSensorAvailable = registered,
                isTracking = registered,
                sensorTypeLabel = label
            )
            Log.i(TAG, "StepTracker started using $label (registered=$registered)")
        } else {
            _stepStats.value = _stepStats.value.copy(
                isSensorAvailable = false,
                isTracking = false,
                sensorTypeLabel = "Simulator Mode"
            )
        }
    }

    @Synchronized
    fun stopTracking() {
        if (!isRegistered) return
        sensorManager?.unregisterListener(this)
        isRegistered = false
        _stepStats.value = _stepStats.value.copy(isTracking = false)
    }

    fun setBackgroundServiceRunning(running: Boolean) {
        backgroundServiceRunning = running
        _stepStats.value = _stepStats.value.copy(isBackgroundServiceRunning = running)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        checkDateRollover()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt().coerceAtLeast(0)
                val today = todayStr()
                val lastRaw = prefs.getInt(KEY_LAST_RAW_BOOT_STEPS, -1)
                val lastDay = prefs.getString(KEY_LAST_BOOT_DAY, "")

                val delta = when {
                    lastRaw < 0 -> 0 // First reading baseline
                    totalStepsSinceBoot >= lastRaw -> totalStepsSinceBoot - lastRaw
                    else -> totalStepsSinceBoot // Device rebooted cleanly
                }

                prefs.edit()
                    .putInt(KEY_LAST_RAW_BOOT_STEPS, totalStepsSinceBoot)
                    .putString(KEY_LAST_BOOT_DAY, today)
                    .apply()

                if (delta in 1..20000 && (lastDay == today || lastDay.isNullOrEmpty())) {
                    val currentToday = prefs.getInt("steps_$today", 0)
                    saveTodaySteps(currentToday + delta)
                } else if (delta > 0 && lastDay != today) {
                    // Only count steps detected after midnight rollover
                    val currentToday = prefs.getInt("steps_$today", 0)
                    saveTodaySteps(currentToday + delta.coerceAtMost(50))
                }
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    val current = prefs.getInt("steps_${todayStr()}", 0)
                    saveTodaySteps(current + 1)
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // High-accuracy gait peak detector: isolate gravity via low-pass filter
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                gravityMagnitude = 0.85f * gravityMagnitude + 0.15f * magnitude
                val linearAccel = magnitude - gravityMagnitude

                val nowMs = System.currentTimeMillis()
                // Walking step produces rhythmic peak > 2.3 m/s^2 with minimum 330ms stride cooldown
                if (linearAccel > 2.3f && !aboveStepThreshold) {
                    aboveStepThreshold = true
                    if (nowMs - lastAccelPeakTimeMs >= 330L) {
                        lastAccelPeakTimeMs = nowMs
                        val current = prefs.getInt("steps_${todayStr()}", 0)
                        saveTodaySteps(current + 1)
                    }
                } else if (linearAccel < 0.8f) {
                    aboveStepThreshold = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun addManualSteps(count: Int) {
        checkDateRollover()
        val current = prefs.getInt("steps_${todayStr()}", 0)
        saveTodaySteps((current + count).coerceAtLeast(0))
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("step_goal", goal.coerceAtLeast(500)).apply()
        updateStatsState()
        NexoraStepWidgetProvider.updateAllWidgets(appContext)
    }

    fun refreshFromPrefs() {
        checkDateRollover()
        updateStatsState()
    }

    private fun saveTodaySteps(steps: Int) {
        val today = todayStr()
        prefs.edit()
            .putInt("steps_$today", steps)
            .putInt("history_$today", steps)
            .apply()
        updateStatsState()
        NexoraStepWidgetProvider.updateAllWidgets(appContext)
    }

    private fun checkDateRollover() {
        val lastRecordedDate = prefs.getString("last_date", "")
        val today = todayStr()
        if (lastRecordedDate != today) {
            if (!lastRecordedDate.isNullOrBlank()) {
                val prevSteps = prefs.getInt("steps_$lastRecordedDate", 0)
                prefs.edit().putInt("history_$lastRecordedDate", prevSteps).apply()
            }
            prefs.edit().putString("last_date", today).apply()
        }
    }

    private fun loadTodayStats(): StepStats {
        val today = todayStr()
        val steps = prefs.getInt("steps_$today", 0)
        val goal = prefs.getInt("step_goal", 10000)
        val distance = (steps * 0.000762f) // km based on average stride (0.762m)
        val calories = (steps * 0.04f).toInt() // approx kcal per step
        val minutes = (steps / 100).coerceAtLeast(0)

        // Authentic recorded 7-day history (zero fake random numbers)
        val history = (6 downTo 0).map { daysAgo ->
            val d = LocalDate.now().minusDays(daysAgo.toLong())
            val dStr = d.toString()
            val dayName = d.dayOfWeek.name.take(3)
            val st = if (daysAgo == 0) {
                steps
            } else {
                prefs.getInt("steps_$dStr", prefs.getInt("history_$dStr", 0))
            }
            dayName to st
        }

        val hasAnySensor = (stepCounterSensor != null || stepDetectorSensor != null || accelerometerSensor != null)
        val defaultLabel = when {
            stepCounterSensor != null -> "Hardware Step Counter"
            stepDetectorSensor != null -> "Hardware Step Detector"
            accelerometerSensor != null -> "Smart Gait Accelerometer"
            else -> "Simulator Mode"
        }

        return StepStats(
            stepsToday = steps,
            dailyGoal = goal,
            distanceKm = distance,
            caloriesKcal = calories,
            activeMinutes = minutes,
            isSensorAvailable = hasAnySensor,
            isTracking = isRegistered,
            isBackgroundServiceRunning = backgroundServiceRunning,
            sensorTypeLabel = defaultLabel,
            weeklyHistory = history
        )
    }

    private fun updateStatsState() {
        val loaded = loadTodayStats()
        _stepStats.value = loaded.copy(
            isTracking = isRegistered,
            isBackgroundServiceRunning = backgroundServiceRunning
        )
    }

    private fun todayStr(): String = LocalDate.now().toString()
}
