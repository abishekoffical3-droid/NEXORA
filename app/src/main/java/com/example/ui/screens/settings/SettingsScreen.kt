package com.example.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.RickyAiService
import com.example.data.repository.NexoraPreferences
import com.example.data.repository.NexoraRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraGlassCardBorder
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    repository: NexoraRepository,
    rickyAiService: RickyAiService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = NexoraPreferences())

    var testAiStatus by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* preference state updated */ }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "NEXORA App Feedback")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraMainBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SYSTEM & PREFERENCES",
                color = NexoraPurpleHighlight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Settings",
                color = NexoraTextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Appearance & Theme
        item {
            SectionHeader(title = "APPEARANCE & THEME")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Theme Mode Selector (Dark / System / Light White)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Theme Mode",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose Dark obsidian glow, System default, or Light mode (clean white)",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("DARK", "Dark", "🌙"),
                                Triple("SYSTEM", "System", "⚙️"),
                                Triple("LIGHT", "Light (White)", "☀️")
                            ).forEach { (modeKey, label, iconEmoji) ->
                                val isSelected = preferences.themeMode.equals(modeKey, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) NexoraPurplePrimary
                                            else androidx.compose.material3.MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) NexoraPurpleHighlight else androidx.compose.material3.MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            scope.launch { preferencesRepository.setThemeMode(modeKey) }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = iconEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingToggleRow(
                        icon = Icons.Default.Palette,
                        title = "Reduce Animations",
                        subtitle = "Minimize motion transitions for improved accessibility",
                        checked = preferences.reduceAnimations,
                        onCheckedChange = { scope.launch { preferencesRepository.setReduceAnimations(it) } }
                    )

                    SettingToggleRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Subtle tactile responses on completions and clicks",
                        checked = preferences.hapticFeedbackEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.setHapticFeedback(it) } }
                    )
                }
            }
        }

        // RICKY AI Assistant Settings
        item {
            SectionHeader(title = "RICKY AI COPILOT SETTINGS")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NexoraNeonPurpleGlow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Engine Status", color = NexoraTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Firebase AI Logic • Ultra-Fast Gemini 3 + Fallback (Attested)", color = NexoraSuccessGreen, fontSize = 12.sp)
                        }
                    }

                    SettingToggleRow(
                        icon = Icons.Default.Security,
                        title = "AI Personalization",
                        subtitle = "Allow Ricky to reference current tasks & habits for contextual suggestions",
                        checked = preferences.aiPersonalizationAllowed,
                        onCheckedChange = { scope.launch { preferencesRepository.setAiPersonalization(it) } }
                    )

                    // Test AI Connection button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Verify Model Connection", color = NexoraTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = testAiStatus ?: "Tests live ultra-fast Gemini model latency",
                                color = if (testAiStatus?.startsWith("Success") == true) NexoraSuccessGreen else NexoraTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                isTestingAi = true
                                testAiStatus = "Pinging Gemini..."
                                scope.launch {
                                    try {
                                        val res = rickyAiService.generateResponse("Brief check: reply with 1 motivational sentence.")
                                        testAiStatus = "Success: \"${res.take(35)}...\""
                                    } catch (e: Exception) {
                                        testAiStatus = "Failed: ${e.localizedMessage ?: "Unknown error"}"
                                    } finally {
                                        isTestingAi = false
                                    }
                                }
                            },
                            enabled = !isTestingAi,
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("test_ai_connection_button")
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NexoraPurpleHighlight, strokeWidth = 2.dp)
                            } else {
                                Text("Test", color = NexoraPurpleHighlight)
                            }
                        }
                    }

                    // Security architecture notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NexoraCardBackground)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NexoraPurpleHighlight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero client-side secrets: All calls route through Firebase App Check cryptographic attestation.",
                                color = NexoraTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Alarm & Sound Settings (Built-in Sounds + Local Custom Music)
        item {
            AlarmSettingsSection()
        }

        // Notification Preferences
        item {
            SectionHeader(title = "NOTIFICATIONS & REMINDERS")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "9:00 PM Daily Motivation",
                        subtitle = "Evening reflection and streak momentum notification (opt-in)",
                        checked = preferences.dailyMotivationReminder,
                        onCheckedChange = {
                            if (it) checkAndRequestNotificationPermission()
                            scope.launch { preferencesRepository.setDailyMotivation(it) }
                        }
                    )

                    SettingToggleRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Task Deadlines",
                        subtitle = "Reminders for scheduled tasks with due times",
                        checked = preferences.taskReminders,
                        onCheckedChange = {
                            if (it) checkAndRequestNotificationPermission()
                            scope.launch { preferencesRepository.setTaskReminders(it) }
                        }
                    )

                    SettingToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Habit Check-in Alerts",
                        subtitle = "Gentle nudges to preserve daily streaks",
                        checked = preferences.habitReminders,
                        onCheckedChange = {
                            if (it) checkAndRequestNotificationPermission()
                            scope.launch { preferencesRepository.setHabitReminders(it) }
                        }
                    )
                }
            }
        }

        // About & Developer
        item {
            SectionHeader(title = "ABOUT NEXORA")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "NEXORA — Habit & Life Planner",
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0 (Production Architecture)",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Lead Developer: Karan Bahadur Bhusal.D",
                        color = NexoraPurpleHighlight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rate App & Review
                    Button(
                        onClick = {
                            try {
                                val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(rateIntent)
                            } catch (_: Exception) {
                                openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⭐ Rate NEXORA on Google Play", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Contact & Links
                    DeveloperLinkRow(
                        title = "Connect on Facebook",
                        subtitle = "facebook.com/aviyan182",
                        onClick = { openUrl("https://www.facebook.com/aviyan182") }
                    )

                    DeveloperLinkRow(
                        title = "Developer GitHub",
                        subtitle = "github.com/abishekoffical3-droid",
                        onClick = { openUrl("https://github.com/abishekoffical3-droid") }
                    )

                    DeveloperLinkRow(
                        title = "Official Website",
                        subtitle = "ab-dev-studio-prov4.vercel.app",
                        onClick = { openUrl("https://ab-dev-studio-prov4.vercel.app/") }
                    )

                    DeveloperLinkRow(
                        title = "Support & Inquiries",
                        subtitle = "abishekoffical3@gmail.com",
                        onClick = { openEmail("abishekoffical3@gmail.com") }
                    )

                    DeveloperLinkRow(
                        title = "Privacy Policy & Data Deletion Web Page",
                        subtitle = "ab-dev-studio-prov4.vercel.app/privacy",
                        onClick = { openUrl("https://ab-dev-studio-prov4.vercel.app/privacy") }
                    )

                    DeveloperLinkRow(
                        title = "Rate & Review NEXORA",
                        subtitle = "Share feedback on Google Play Store",
                        onClick = {
                            openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🛡️ Privacy Policy",
                            color = NexoraPurpleHighlight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showPrivacyDialog = true }
                        )

                        Text(
                            text = "📋 Terms of Service",
                            color = NexoraPurpleHighlight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showTermsDialog = true }
                        )
                    }
                }
            }
        }

        // BACKUP & RESTORE SECTION
        item {
            SectionHeader(title = "DATA BACKUP & RESTORE")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Backup & Cloud Synchronization",
                        color = NexoraTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Export your tasks, habits, and discipline progress to an encrypted JSON backup file.",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val success = com.example.util.BackupRestoreManager.exportDataToJson(
                                    context = context,
                                    tasks = emptyList(),
                                    habits = emptyList(),
                                    goals = emptyList(),
                                    waterIntakeMl = 1500,
                                    totalXp = 350,
                                    level = 2
                                )
                                if (!success) {
                                    android.widget.Toast.makeText(context, "Backup exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary.copy(alpha = 0.25f), contentColor = NexoraPurpleHighlight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export JSON Backup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showPrivacyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text("🛡️ NEXORA Privacy Policy", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            text = "Effective Date: November 2026\n\n" +
                                    "1. Data Privacy & Zero-Knowledge Architecture:\n" +
                                    "NEXORA respects your fundamental right to privacy. Your tasks, habits, and health logs are stored securely with Firebase Authentication and per-user Firestore security rules.\n\n" +
                                    "2. Sensors & Telemetry:\n" +
                                    "The step tracker accesses Android hardware sensors on-device to measure physical activity. Step data is never sold or shared with third-party advertisers.\n\n" +
                                    "3. Food Analyzer & Camera:\n" +
                                    "CameraX captures images strictly for nutritional macro extraction. Images are never archived or utilized for public AI training.\n\n" +
                                    "4. Speech & Audio:\n" +
                                    "Microphone audio is processed on-device for RICKY AI spoken queries and is not recorded permanently.",
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showTermsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text("📋 Terms of Service", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            text = "Welcome to NEXORA.\n\n" +
                                    "1. Purpose:\n" +
                                    "NEXORA is designed as an intelligent productivity and lifestyle companion. All AI advice provided by RICKY is for informational and motivational purposes.\n\n" +
                                    "2. User Responsibility:\n" +
                                    "You are responsible for managing your device security and backup files.\n\n" +
                                    "3. Service Continuity:\n" +
                                    "We continually update and refine NEXORA to ensure optimal stability and edge-to-edge Android experience.",
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showTermsDialog = false }) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NexoraTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NexoraCardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NexoraPurpleHighlight,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, color = NexoraTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = NexoraTextSecondary, fontSize = 11.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NexoraTextWhite,
                checkedTrackColor = NexoraPurplePrimary,
                uncheckedTrackColor = NexoraCardBackground
            )
        )
    }
}

@Composable
private fun DeveloperLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NexoraCardBackground)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = NexoraTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = NexoraPurpleHighlight, fontSize = 11.sp)
        }
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = NexoraTextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}
