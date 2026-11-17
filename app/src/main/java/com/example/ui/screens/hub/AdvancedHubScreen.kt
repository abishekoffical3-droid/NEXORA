package com.example.ui.screens.hub

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.RickyAiService
import com.example.data.model.HabitItem
import com.example.data.model.TaskItem
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraWarningAmber
import com.example.util.AdvancedFeaturesManager
import com.example.util.ReportExporter
import com.example.util.StepTracker
import com.example.util.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AdvancedHubScreen(
    featuresManager: AdvancedFeaturesManager,
    voiceManager: VoiceManager,
    stepTracker: StepTracker,
    rickyAiService: RickyAiService,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    onOpenRickyVoiceChat: () -> Unit
) {
    val context = LocalContext.current
    val hubState by featuresManager.hubState.collectAsState()
    val stepStats by stepTracker.stepStats.collectAsState()
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()

    var showSleepDialog by remember { mutableStateOf(false) }
    var voiceStatusMsg by remember { mutableStateOf<String?>(null) }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startListening(
                onResult = { query ->
                    voiceStatusMsg = "You said: \"$query\""
                    CoroutineScope(Dispatchers.Main).launch {
                        val reply = rickyAiService.askRickyQuickAdvice(query)
                        voiceManager.speak(reply)
                    }
                },
                onError = { err ->
                    voiceStatusMsg = err
                }
            )
        } else {
            Toast.makeText(context, "Microphone permission is needed for RICKY Voice Assistant.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Advanced Hub",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Voice AI, Hydration, Sleep, Audio & Reports",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Level Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraPurplePrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "XP Level",
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lvl ${hubState.currentLevel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraPurpleHighlight
                    )
                }
            }
        }

        // FEATURE 1: 🎙️ RICKY Live Voice Assistant Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ricky_voice_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListening || isSpeaking) NexoraPurpleHighlight.copy(alpha = 0.25f)
                                        else NexoraPurplePrimary.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.AutoAwesome,
                                    contentDescription = "Voice AI",
                                    tint = if (isListening) NexoraSuccessGreen else NexoraPurpleHighlight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "RICKY Voice Assistant",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = if (isListening) "Listening to you..." else if (isSpeaking) "Ricky is speaking..." else "Tap mic to speak with Ricky",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isListening) NexoraSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                } else if (isSpeaking) {
                                    voiceManager.stopSpeaking()
                                } else {
                                    val hasMic = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasMic) {
                                        voiceManager.startListening(
                                            onResult = { query ->
                                                voiceStatusMsg = "You said: \"$query\""
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    val reply = rickyAiService.askRickyQuickAdvice(query)
                                                    voiceManager.speak(reply)
                                                }
                                            },
                                            onError = { err ->
                                                voiceStatusMsg = err
                                            }
                                        )
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) NexoraSuccessGreen else NexoraPurplePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("voice_talk_btn")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mic",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListening) "Stop" else if (isSpeaking) "Mute" else "Talk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (voiceStatusMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = voiceStatusMsg!!,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NexoraPurpleHighlight,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // FEATURE 6: 🏆 Level & XP Hero Gamification Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Discipline Rank",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = hubState.levelTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(NexoraPurplePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val xpProgress = (hubState.totalXp.toFloat() % 300f) / 300f
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NexoraPurpleHighlight,
                        trackColor = NexoraPurplePrimary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${hubState.totalXp} Total XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(300 - (hubState.totalXp % 300))} XP to Next Level",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoraPurpleHighlight
                        )
                    }
                }
            }
        }

        // FEATURE 2: 💧 Water & Hydration Tracker
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hydration_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.WaterDrop,
                                    contentDescription = "Water",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hydration Tracker",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = "${hubState.waterIntakeMl} / ${hubState.waterGoalMl} ml",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Text(
                            text = "${((hubState.waterIntakeMl.toFloat() / hubState.waterGoalMl) * 100).toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val waterProgress = (hubState.waterIntakeMl.toFloat() / hubState.waterGoalMl).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF0284C7).copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Log Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { featuresManager.addWater(250) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+250ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { featuresManager.addWater(500) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+500ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { featuresManager.resetWater() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // FEATURE 3 & 5: 😴 Sleep Recovery & 🥗 Daily Meal Targets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleep Card
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSleepDialog = true }
                        .testTag("sleep_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NightlightRound,
                                contentDescription = "Sleep",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${hubState.sleepDurationHours} hrs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Sleep Quality: ${hubState.sleepQualityScore}%",
                            fontSize = 11.sp,
                            color = NexoraSuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bed ${hubState.bedtime} • Wake ${hubState.wakeTime}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Daily Meal Target Card
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nutrition_target_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = "Nutrition",
                                tint = Color(0xFFFB923C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${hubState.consumedCalories} / ${hubState.targetCalories}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Target Calories (kcal)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Protein: ${hubState.consumedProteinGrams}g / ${hubState.targetProteinGrams}g",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFB923C)
                        )
                    }
                }
            }
        }

        // FEATURE 4: 🧠 Pomodoro Study & Ambient Sounds
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ambient_sound_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NexoraPurplePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "Ambient Sounds",
                                    tint = NexoraPurpleHighlight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Deep Focus Ambience",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = if (hubState.isAmbientPlaying) "Playing: ${hubState.activeSoundTrack}" else "Calming background audio for study",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (hubState.isAmbientPlaying) NexoraSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sound Track Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Gentle Rain", "Deep Forest", "Binaural White").forEach { track ->
                            val isSelected = hubState.isAmbientPlaying && hubState.activeSoundTrack == track
                            Button(
                                onClick = { featuresManager.toggleAmbientSound(track) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NexoraPurpleHighlight else NexoraPurplePrimary.copy(alpha = 0.2f),
                                    contentColor = if (isSelected) Color.White else NexoraPurpleHighlight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(track, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // FEATURE 7: 📤 Export Weekly Health & Productivity Report
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_report_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Weekly Summary Report",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Generates high-res visual chart of your steps, habits, XP & diet",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val success = ReportExporter.generateAndShareWeeklyReport(
                                context = context,
                                steps = stepStats.stepsToday,
                                distanceKm = stepStats.distanceKm,
                                caloriesBurned = stepStats.caloriesKcal,
                                waterMl = hubState.waterIntakeMl,
                                tasksCompleted = tasks.count { it.completed },
                                habitsCompleted = habits.sumOf { it.completedDates.size },
                                currentStreak = habits.maxOfOrNull { it.currentStreak } ?: 0,
                                level = hubState.currentLevel,
                                xp = hubState.totalXp
                            )
                            if (success) {
                                featuresManager.addXp(30)
                            } else {
                                Toast.makeText(context, "Could not generate report", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // FEATURE 3: 🧘 Daily Mood & Emotional Energy Journal
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_journal_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Mood & Energy Check-in",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "Current State: ${hubState.currentMoodEmoji} ${hubState.currentMoodLabel}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NexoraPurpleHighlight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mood selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("⚡", "Energetic", "Ready to crush tasks"),
                            Triple("😊", "Happy", "Positive & steady flow"),
                            Triple("🥱", "Tired", "Need light pacing"),
                            Triple("🧘", "Calm", "Centered & mindful")
                        ).forEach { (emoji, label, note) ->
                            val isSelected = hubState.currentMoodLabel == label
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) NexoraPurplePrimary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NexoraPurpleHighlight else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        featuresManager.setMood(emoji, label, note)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 RICKY: \"${hubState.moodReflection}\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // FEATURE 4: ⏱️ Real-time Habit & Focus Lock (Anti-Distraction Zen Mode)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zen_focus_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Anti-Distraction Zen Focus",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "Full-screen distraction-free deep work session",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (hubState.isZenModeActive) {
                                    featuresManager.exitZenMode(completed = true)
                                } else {
                                    featuresManager.startZenMode(25)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hubState.isZenModeActive) NexoraSuccessGreen else NexoraPurplePrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (hubState.isZenModeActive) "Complete" else "Enter Zen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (hubState.isZenModeActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NexoraSuccessGreen.copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏳", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zen session active (25m Pomodoro) • Keep phone screen face-up and focus!",
                                fontSize = 11.sp,
                                color = NexoraSuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // FEATURE 5: 🏅 Interactive Badges & Achievement Showcase
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("badges_showcase_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Achievements & Badges (${hubState.unlockedBadges.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "+40 XP each",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoraPurpleHighlight
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val allBadges = listOf(
                        Pair("First Step", "👟 Completed first step recording"),
                        Pair("Hydration Starter", "💧 Logged daily water intake"),
                        Pair("Night Owl", "🌙 Logged nighttime recovery"),
                        Pair("Zen Master", "🧘 Finished 25m distraction lock"),
                        Pair("Hydration Master", "🌊 Reached 3,000ml water goal"),
                        Pair("Discipline Master", "⚡ Reached Level 5 rank")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allBadges.forEach { (badge, desc) ->
                            val isUnlocked = hubState.unlockedBadges.contains(badge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isUnlocked) NexoraPurplePrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isUnlocked) NexoraPurpleHighlight else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isUnlocked) "🏅" else "🔒", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = badge,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isUnlocked) NexoraPurpleHighlight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isUnlocked) {
                                    Text("UNLOCKED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = NexoraSuccessGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // FEATURE 2: 📱 Android Home-Screen Live Quick Widget Preview
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_widget_preview_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Home Screen Live Widget Preview",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "Quick 4x2 Dashboard: Steps, Water, & Daily Tasks",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Button(
                            onClick = {
                                featuresManager.syncWidgetTelemetry()
                                Toast.makeText(context, "Widget synced with live telemetry!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary.copy(alpha = 0.25f), contentColor = NexoraPurpleHighlight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Render simulated Android Home Widget
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1B4B))
                            .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("NEXORA QUICK WIDGET", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
                                Text("⚡ Lvl ${hubState.currentLevel}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NexoraPurpleHighlight)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("👟 Steps", fontSize = 10.sp, color = Color.LightGray)
                                    Text("${stepStats.stepsToday} / ${stepStats.dailyGoal}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("💧 Hydration", fontSize = 10.sp, color = Color.LightGray)
                                    Text("${hubState.waterIntakeMl} ml", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                                Column {
                                    Text("📋 Pending", fontSize = 10.sp, color = Color.LightGray)
                                    Text("${tasks.count { !it.completed }} tasks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NexoraSuccessGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Sleep Log Dialog
    if (showSleepDialog) {
        var bedInput by remember { mutableStateOf(hubState.bedtime) }
        var wakeInput by remember { mutableStateOf(hubState.wakeTime) }
        var hoursInput by remember { mutableStateOf(hubState.sleepDurationHours.toString()) }

        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = {
                Text("Log Sleep & Recovery", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bedInput,
                        onValueChange = { bedInput = it },
                        label = { Text("Bedtime (e.g. 23:00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = wakeInput,
                        onValueChange = { wakeInput = it },
                        label = { Text("Wake Time (e.g. 07:00)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it },
                        label = { Text("Total Hours (e.g. 8.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = hoursInput.toFloatOrNull() ?: 7.5f
                        val quality = ((hours / 8f) * 100).toInt().coerceIn(40, 100)
                        featuresManager.updateSleep(bedInput, wakeInput, hours, quality)
                        showSleepDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                ) {
                    Text("Save Sleep")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
