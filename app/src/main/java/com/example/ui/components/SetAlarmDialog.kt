package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.util.AlarmScheduler
import com.example.util.AlarmSettingsStore
import com.example.util.AlarmSoundManager
import com.example.util.NexoraAlarmSound
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun SetAlarmDialog(
    taskId: String,
    taskTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val alarmStore = remember { AlarmSettingsStore.getInstance(context) }
    val alarmState by alarmStore.state.collectAsState()
    val playingSoundId by AlarmSoundManager.playingSoundId.collectAsState()

    var selectedHour by remember { mutableIntStateOf(LocalTime.now().plusHours(1).hour) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var isTomorrow by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { AlarmSoundManager.stop() }
    }

    val activeSoundLabel = if (alarmState.selectedSound == NexoraAlarmSound.CUSTOM_MUSIC) {
        alarmState.customMusicName ?: "Custom Local Music"
    } else {
        alarmState.selectedSound.title
    }
    val isPreviewPlaying = playingSoundId == alarmState.selectedSound.id

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = NexoraPurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Set Task Alarm",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = taskTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Alarm Sound Preview Bar (change in Settings -> Alarm & Sound)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NexoraPurpleHighlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sound: $activeSoundLabel",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            AlarmSoundManager.playPreview(
                                context,
                                alarmState.selectedSound,
                                alarmState.customMusicUri
                            )
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewPlaying) "Stop Preview" else "Preview Sound",
                            tint = if (isPreviewPlaying) NexoraErrorRed else NexoraPurpleHighlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets
                Text(
                    text = "Quick Presets:",
                    color = NexoraPurpleHighlight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "+15m", 30 to "+30m", 60 to "+1h", 120 to "+2h").forEach { (mins, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .clickable {
                                    val trigger = System.currentTimeMillis() + (mins * 60 * 1000L)
                                    AlarmScheduler.scheduleAlarm(
                                        context = context,
                                        id = taskId,
                                        title = taskTitle,
                                        note = "Time for your scheduled task: $taskTitle",
                                        triggerAtMillis = trigger
                                    )
                                    Toast.makeText(context, "Alarm set for $label from now! ⏰", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Day Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isTomorrow) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isTomorrow = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Today",
                            color = if (!isTomorrow) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTomorrow) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isTomorrow = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tomorrow",
                            color = if (isTomorrow) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Pickers (Hour and Minute simple selectors)
                Text(
                    text = "Pick Alarm Time (24h format):",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours selection
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", fontSize = 11.sp, color = NexoraPurpleHighlight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { selectedHour = (selectedHour + 23) % 24 },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("-", color = MaterialTheme.colorScheme.onSurface) }

                            Text(
                                text = String.format("%02d", selectedHour),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Button(
                                onClick = { selectedHour = (selectedHour + 1) % 24 },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("+", color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NexoraPurpleHighlight, modifier = Modifier.padding(horizontal = 10.dp))

                    // Minutes selection
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", fontSize = 11.sp, color = NexoraPurpleHighlight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { selectedMinute = (selectedMinute + 55) % 60 },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("-", color = MaterialTheme.colorScheme.onSurface) }

                            Text(
                                text = String.format("%02d", selectedMinute),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Button(
                                onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("+", color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Cancel Alarm / Set Alarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            AlarmScheduler.cancelAlarm(context, taskId)
                            Toast.makeText(context, "Alarm cleared", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val targetDate = if (isTomorrow) LocalDate.now().plusDays(1) else LocalDate.now()
                            val targetDateTime = LocalDateTime.of(targetDate, LocalTime.of(selectedHour, selectedMinute))
                            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                            if (triggerMillis <= System.currentTimeMillis()) {
                                Toast.makeText(context, "Please pick a future time today or tomorrow!", Toast.LENGTH_SHORT).show()
                            } else {
                                AlarmScheduler.scheduleAlarm(
                                    context = context,
                                    id = taskId,
                                    title = taskTitle,
                                    note = "Time for: $taskTitle",
                                    triggerAtMillis = triggerMillis
                                )
                                Toast.makeText(context, "Alarm scheduled for ${String.format("%02d:%02d", selectedHour, selectedMinute)}! ⏰", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_set_alarm_button")
                    ) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
