package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class NexoraAlarmSound(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    COSMIC_PULSE("COSMIC_PULSE", "Cosmic Pulse", "Futuristic neon chime synth", "🌌"),
    MORNING_ZEN("MORNING_ZEN", "Morning Zen", "Warm harmonic crystal bells", "🔔"),
    FOCUS_RADAR("FOCUS_RADAR", "Focus Radar", "Crisp rhythmic productivity alert", "⚡"),
    SYSTEM_SUNRISE("SYSTEM_SUNRISE", "Sunrise Default", "Phone default alarm ringtone", "🌅"),
    CUSTOM_MUSIC("CUSTOM_MUSIC", "Custom Local Music", "Pick any song or audio on your phone", "🎵");

    companion object {
        fun fromId(id: String?): NexoraAlarmSound {
            return entries.firstOrNull { it.id == id } ?: COSMIC_PULSE
        }
    }
}

object AlarmSoundManager {

    private const val TAG = "AlarmSoundManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    private val _playingSoundId = MutableStateFlow<String?>(null)
    val playingSoundId: StateFlow<String?> = _playingSoundId.asStateFlow()

    fun playPreview(context: Context, sound: NexoraAlarmSound, customUriString: String? = null) {
        if (_playingSoundId.value == sound.id) {
            stop()
            return
        }
        playAlarmSound(context, sound, customUriString, isPreview = true)
    }

    fun playAlarmSound(
        context: Context,
        sound: NexoraAlarmSound,
        customUriString: String? = null,
        isPreview: Boolean = false
    ) {
        stop()
        _playingSoundId.value = sound.id
        val appContext = context.applicationContext
        val maxDurationMs = if (isPreview) 6_000L else 45_000L

        activeJob = scope.launch {
            try {
                when (sound) {
                    NexoraAlarmSound.CUSTOM_MUSIC -> {
                        if (!customUriString.isNullOrBlank()) {
                            val played = playUriViaMediaPlayer(appContext, Uri.parse(customUriString), maxDurationMs)
                            if (!played) {
                                playSystemAlarmTone(appContext, maxDurationMs)
                            }
                        } else {
                            playSystemAlarmTone(appContext, maxDurationMs)
                        }
                    }

                    NexoraAlarmSound.SYSTEM_SUNRISE -> {
                        playSystemAlarmTone(appContext, maxDurationMs)
                    }

                    NexoraAlarmSound.COSMIC_PULSE,
                    NexoraAlarmSound.MORNING_ZEN,
                    NexoraAlarmSound.FOCUS_RADAR -> {
                        playSynthesizedToneLoop(sound, maxDurationMs)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing alarm sound", e)
            } finally {
                stopInternal()
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        stopInternal()
    }

    private fun stopInternal() {
        _playingSoundId.value = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            audioTrack?.apply {
                pause()
                flush()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    private suspend fun playUriViaMediaPlayer(context: Context, uri: Uri, durationMs: Long): Boolean {
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
            delay(durationMs)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Custom audio URI playback failed, falling back to default ringtone", e)
            false
        }
    }

    private suspend fun playSystemAlarmTone(context: Context, durationMs: Long) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        playUriViaMediaPlayer(context, alarmUri, durationMs)
    }

    private suspend fun CoroutineScope.playSynthesizedToneLoop(sound: NexoraAlarmSound, maxDurationMs: Long) {
        val sampleRate = 22050
        val notes = when (sound) {
            NexoraAlarmSound.COSMIC_PULSE -> listOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25) // C5-E5-G5-C6 cosmic arpeggio
            NexoraAlarmSound.MORNING_ZEN -> listOf(440.0, 554.37, 659.25, 880.0) // A major calm chime
            NexoraAlarmSound.FOCUS_RADAR -> listOf(880.0, 0.0, 880.0, 1174.66, 0.0) // Crisp productivity pulse
            else -> listOf(523.25, 659.25, 783.99)
        }

        val noteDurationSec = when (sound) {
            NexoraAlarmSound.COSMIC_PULSE -> 0.22
            NexoraAlarmSound.MORNING_ZEN -> 0.42
            NexoraAlarmSound.FOCUS_RADAR -> 0.16
            else -> 0.25
        }

        val samplesPerNote = (sampleRate * noteDurationSec).toInt()
        val totalSamples = samplesPerNote * notes.size
        val pcmBuffer = ShortArray(totalSamples)

        for (nIdx in notes.indices) {
            val freq = notes[nIdx]
            val offset = nIdx * samplesPerNote
            for (i in 0 until samplesPerNote) {
                if (freq <= 0.0) {
                    pcmBuffer[offset + i] = 0
                } else {
                    val t = i.toDouble() / sampleRate
                    // Smooth attack + exponential bell decay envelope
                    val attack = (i.toDouble() / (sampleRate * 0.015)).coerceAtMost(1.0)
                    val decay = exp(-3.5 * t)
                    val fundamental = sin(2.0 * PI * freq * t)
                    val harmonic = 0.28 * sin(2.0 * PI * (freq * 2.0) * t)
                    val sample = ((fundamental + harmonic) * attack * decay * Short.MAX_VALUE * 0.55).toInt()
                    pcmBuffer[offset + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(totalSamples * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        val endTime = System.currentTimeMillis() + maxDurationMs
        while (isActive && System.currentTimeMillis() < endTime) {
            track.write(pcmBuffer, 0, pcmBuffer.size)
            delay(350L)
        }
    }
}
