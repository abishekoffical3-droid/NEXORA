package com.example.ui.screens.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import com.example.ui.theme.NexoraWarningAmber
import com.example.util.AlarmSettingsStore
import com.example.util.AlarmSoundManager
import com.example.util.CustomAlarmItem
import com.example.util.NexoraAlarmSound
import java.util.Locale

@Composable
fun AlarmSettingsSection() {
    val context = LocalContext.current
    val alarmStore = remember { AlarmSettingsStore.getInstance(context) }
    val alarmState by alarmStore.state.collectAsState()
    val playingSoundId by AlarmSoundManager.playingSoundId.collectAsState()

    var showAddAlarmDialog by remember { mutableStateOf(false) }

    val alarmManager = remember(context) { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    val canExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() ?: true
    } else true

    DisposableEffect(Unit) {
        onDispose {
            AlarmSoundManager.stop()
        }
    }

    val pickMusicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Provider may not support persistable flags, URI still works for active access
            }
            val displayName = resolveAudioDisplayName(context, uri)
            alarmStore.setCustomMusic(uri.toString(), displayName)
            AlarmSoundManager.playPreview(context, NexoraAlarmSound.CUSTOM_MUSIC, uri.toString())
            Toast.makeText(context, "Custom alarm song set: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    Column {
        Text(
            text = "ALARM & SOUND SETTINGS",
            color = NexoraTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexoraWarningAmber.copy(alpha = 0.14f))
                            .border(1.dp, NexoraWarningAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Exact alarm permission",
                                tint = NexoraWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Allow Exact Alarms so wake-up alarms ring on the exact minute.",
                                color = NexoraTextWhite,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Allow", color = NexoraWarningAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                // Scheduled Alarms Header + Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = NexoraPurpleHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Scheduled Alarms",
                                color = NexoraTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Manage wake-up & study alarms",
                                color = NexoraTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showAddAlarmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexoraPurplePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_custom_alarm_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlarm,
                            contentDescription = "Add Alarm",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (alarmState.customAlarms.isEmpty()) {
                    Text(
                        text = "No alarms yet. Tap Add to schedule one.",
                        color = NexoraTextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alarmState.customAlarms.forEach { alarm ->
                            CustomAlarmRow(
                                alarm = alarm,
                                onToggle = { enabled ->
                                    alarmStore.toggleAlarm(alarm.id, enabled)
                                    if (enabled) {
                                        Toast.makeText(
                                            context,
                                            "Alarm enabled for ${String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onDelete = { alarmStore.deleteAlarm(alarm.id) }
                            )
                        }
                    }
                }

                // Ringtone & Sound Selection (4 Built-In Tones)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alarm Sound & Ringtone",
                        color = NexoraTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val builtInSounds = listOf(
                    NexoraAlarmSound.COSMIC_PULSE,
                    NexoraAlarmSound.MORNING_ZEN,
                    NexoraAlarmSound.FOCUS_RADAR,
                    NexoraAlarmSound.SYSTEM_SUNRISE
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    builtInSounds.forEach { sound ->
                        val isSelected = alarmState.selectedSound == sound
                        val isPlaying = playingSoundId == sound.id
                        SoundOptionRow(
                            emoji = sound.iconEmoji,
                            title = sound.title,
                            subtitle = sound.subtitle,
                            isSelected = isSelected,
                            isPlaying = isPlaying,
                            onSelect = { alarmStore.selectSound(sound) },
                            onTogglePreview = {
                                alarmStore.selectSound(sound)
                                AlarmSoundManager.playPreview(context, sound)
                            }
                        )
                    }

                    // 5th option: Local Custom Music Picker via SAF
                    val isCustomSelected = alarmState.selectedSound == NexoraAlarmSound.CUSTOM_MUSIC
                    val isCustomPlaying = playingSoundId == NexoraAlarmSound.CUSTOM_MUSIC.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCustomSelected) NexoraPurplePrimary.copy(alpha = 0.22f)
                                else NexoraCardBackground
                            )
                            .border(
                                1.dp,
                                if (isCustomSelected) NexoraPurpleHighlight else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!alarmState.customMusicUri.isNullOrBlank()) {
                                    alarmStore.selectSound(NexoraAlarmSound.CUSTOM_MUSIC)
                                } else {
                                    pickMusicLauncher.launch(arrayOf("audio/*"))
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "🎵", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Custom Local Music",
                                            color = NexoraTextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = alarmState.customMusicName
                                                ?: "Choose any MP3 / audio song from your phone",
                                            color = if (alarmState.customMusicName != null) NexoraSuccessGreen else NexoraTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!alarmState.customMusicUri.isNullOrBlank()) {
                                        IconButton(
                                            onClick = {
                                                alarmStore.selectSound(NexoraAlarmSound.CUSTOM_MUSIC)
                                                AlarmSoundManager.playPreview(
                                                    context,
                                                    NexoraAlarmSound.CUSTOM_MUSIC,
                                                    alarmState.customMusicUri
                                                )
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCustomPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (isCustomPlaying) "Stop Preview" else "Preview Song",
                                                tint = if (isCustomPlaying) NexoraErrorRed else NexoraPurpleHighlight
                                            )
                                        }
                                    }
                                    if (isCustomSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NexoraSuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { pickMusicLauncher.launch(arrayOf("audio/*")) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NexoraPurplePrimary.copy(alpha = 0.25f),
                                    contentColor = NexoraPurpleHighlight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_custom_alarm_music_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Pick Local Music",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (alarmState.customMusicName == null) "Pick Music From Phone" else "Change Custom Song",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Alarm Vibration Toggle
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
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Vibrate on Alarm", color = NexoraTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Pulse vibration pattern when alarms ring", color = NexoraTextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = alarmState.vibrationEnabled,
                        onCheckedChange = { alarmStore.setVibration(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraTextWhite,
                            checkedTrackColor = NexoraPurplePrimary,
                            uncheckedTrackColor = NexoraCardBackground
                        ),
                        modifier = Modifier.testTag("alarm_vibration_switch")
                    )
                }
            }
        }
    }

    if (showAddAlarmDialog) {
        AddCustomAlarmDialog(
            onDismiss = { showAddAlarmDialog = false },
            onSave = { label, hour, minute ->
                alarmStore.addOrUpdateAlarm(label, hour, minute)
                Toast.makeText(
                    context,
                    "Alarm set for ${String.format(Locale.US, "%02d:%02d", hour, minute)}",
                    Toast.LENGTH_SHORT
                ).show()
                showAddAlarmDialog = false
            }
        )
    }
}

@Composable
private fun SoundOptionRow(
    emoji: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) NexoraPurplePrimary.copy(alpha = 0.22f)
                else NexoraCardBackground
            )
            .border(
                1.dp,
                if (isSelected) NexoraPurpleHighlight else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = NexoraTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = NexoraTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onTogglePreview,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop Preview" else "Preview Sound",
                    tint = if (isPlaying) NexoraErrorRed else NexoraPurpleHighlight
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = NexoraSuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomAlarmRow(
    alarm: CustomAlarmItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexoraCardBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute),
                color = if (alarm.enabled) NexoraTextWhite else NexoraTextMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = alarm.label,
                color = if (alarm.enabled) NexoraPurpleHighlight else NexoraTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NexoraTextWhite,
                    checkedTrackColor = NexoraPurplePrimary
                )
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alarm",
                    tint = NexoraErrorRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCustomAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit
) {
    var label by remember { mutableStateOf("Morning Discipline") }
    var hour by remember { mutableIntStateOf(6) }
    var minute by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Scheduled Alarm", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Pick Time (24-hour)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", fontSize = 11.sp, color = NexoraPurpleHighlight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { hour = (hour + 23) % 24 },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("-") }
                            Text(
                                text = String.format(Locale.US, "%02d", hour),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Button(
                                onClick = { hour = (hour + 1) % 24 },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+") }
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraPurpleHighlight,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", fontSize = 11.sp, color = NexoraPurpleHighlight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { minute = (minute + 55) % 60 },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("-") }
                            Text(
                                text = String.format(Locale.US, "%02d", minute),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Button(
                                onClick = { minute = (minute + 5) % 60 },
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label, hour, minute) },
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
            ) {
                Text("Save Alarm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun resolveAudioDisplayName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                cursor.getString(idx)
            } else null
        } ?: uri.lastPathSegment ?: "Selected Audio Track"
    } catch (_: Exception) {
        uri.lastPathSegment ?: "Selected Audio Track"
    }
}
