package com.example.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Adaptive motion & graphics budget tuned for smooth 60fps performance across
 * all Android 8+ devices, including low-RAM / older GPUs and small screens.
 */
@Immutable
data class NexoraMotionConfig(
    val reduceAnimations: Boolean = false,
    val isLowEndDevice: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val maxParticles: Int = 22
) {
    val shouldAnimateContinuousOrbs: Boolean
        get() = !reduceAnimations && !isPowerSaveMode
}

val LocalNexoraMotion = compositionLocalOf { NexoraMotionConfig() }

@Composable
fun rememberNexoraMotionConfig(userReduceAnimations: Boolean): NexoraMotionConfig {
    val context = LocalContext.current
    return remember(context, userReduceAnimations) {
        detectMotionConfig(context, userReduceAnimations)
    }
}

fun detectMotionConfig(context: Context, userReduceAnimations: Boolean): NexoraMotionConfig {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    val isLowRam = activityManager?.isLowRamDevice == true ||
        (activityManager?.memoryClass ?: 256) <= 128 ||
        Runtime.getRuntime().availableProcessors() <= 4

    val isPowerSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        powerManager?.isPowerSaveMode == true
    } else false

    val systemAnimatorScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    } catch (_: Exception) {
        1f
    }

    val reduceMotion = userReduceAnimations || systemAnimatorScale == 0f
    val particles = when {
        reduceMotion || isPowerSave -> 0
        isLowRam -> 8
        else -> 18
    }

    return NexoraMotionConfig(
        reduceAnimations = reduceMotion,
        isLowEndDevice = isLowRam,
        isPowerSaveMode = isPowerSave,
        maxParticles = particles
    )
}
