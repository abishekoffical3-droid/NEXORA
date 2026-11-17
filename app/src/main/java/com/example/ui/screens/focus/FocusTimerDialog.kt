package com.example.ui.screens.focus

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TaskItem
import com.example.data.repository.NexoraRepository
import com.example.ui.components.CircularProgressRing
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
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusTimerDialog(
    tasks: List<TaskItem>,
    repository: NexoraRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMinutes by remember { mutableIntStateOf(25) }
    var selectedType by remember { mutableStateOf("POMODORO") }
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    val selectedTaskTitle = tasks.find { it.id == selectedTaskId }?.title

    // Timer loop
    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        } else if (isRunning && secondsRemaining == 0) {
            isRunning = false
            hasCompleted = true
            scope.launch {
                repository.logFocusSession(
                    durationMinutes = selectedMinutes,
                    sessionType = selectedType,
                    taskTitle = selectedTaskTitle
                )
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = 1001,
                    channelId = NotificationHelper.CHANNEL_FOCUS,
                    title = "Focus Session Finished! 🎯",
                    message = "Great work! You completed $selectedMinutes minutes of deep focus (+25 XP)."
                )
            }
        }
    }

    fun setTimer(minutes: Int, type: String) {
        selectedMinutes = minutes
        selectedType = type
        totalSeconds = minutes * 60
        secondsRemaining = minutes * 60
        isRunning = false
        hasCompleted = false
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) (totalSeconds - secondsRemaining).toFloat() / totalSeconds else 0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, NexoraGlassCardBorder, RoundedCornerShape(28.dp)),
            color = NexoraMainBackground
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEEP FOCUS TIMER",
                        color = NexoraPurpleHighlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NexoraTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("25m Focus", 25, "POMODORO"),
                        Triple("50m Deep", 50, "POMODORO"),
                        Triple("5m Break", 5, "SHORT_BREAK"),
                        Triple("15m Break", 15, "LONG_BREAK")
                    ).forEach { (label, mins, type) ->
                        val isSelected = selectedMinutes == mins && selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NexoraPurplePrimary else NexoraCardBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) NexoraPurpleHighlight else NexoraGlassCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { setTimer(mins, type) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = NexoraTextWhite,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "LINK TO ACTIVE TASK",
                        color = NexoraPurpleHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isNone = selectedTaskId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isNone) NexoraPurplePrimary.copy(alpha = 0.3f) else NexoraCardBackground)
                                    .border(1.dp, if (isNone) NexoraPurpleHighlight else NexoraGlassCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedTaskId = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("General Focus", color = NexoraTextWhite, fontSize = 11.sp)
                            }
                        }
                        items(tasks.filter { !it.completed }) { task ->
                            val isSel = selectedTaskId == task.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NexoraPurplePrimary else NexoraCardBackground)
                                    .border(1.dp, if (isSel) NexoraPurpleHighlight else NexoraGlassCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedTaskId = if (isSel) null else task.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = task.title.take(24),
                                    color = NexoraTextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Visual Ring
                CircularProgressRing(
                    progress = progress,
                    size = 200.dp,
                    strokeWidth = 14.dp,
                    centerText = timeFormatted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        hasCompleted -> "Session Complete! +25 XP Awarded"
                        isRunning -> "Stay fully focused in the zone..."
                        else -> "Ready to immerse"
                    },
                    color = if (hasCompleted) NexoraSuccessGreen else NexoraTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (hasCompleted) FontWeight.Bold else FontWeight.Normal
                )

                if (selectedTaskTitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Focusing on: $selectedTaskTitle",
                        color = NexoraPurpleHighlight,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            secondsRemaining = totalSeconds
                            isRunning = false
                            hasCompleted = false
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NexoraCardBackground)
                            .border(1.dp, NexoraGlassCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = NexoraTextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            if (secondsRemaining == 0) {
                                secondsRemaining = totalSeconds
                                hasCompleted = false
                            }
                            isRunning = !isRunning
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp)
                            .testTag("focus_start_pause_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) NexoraCardBackground else NexoraPurplePrimary
                        ),
                        border = if (isRunning) BorderStroke(1.dp, NexoraPurplePrimary) else null
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = NexoraTextWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "PAUSE" else "START",
                            color = NexoraTextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
