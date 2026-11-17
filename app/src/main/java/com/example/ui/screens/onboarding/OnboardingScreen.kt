package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NexoraRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraChipBackground
import com.example.ui.theme.NexoraGlassCardBorder
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    currentDisplayName: String,
    currentEmail: String,
    repository: NexoraRepository,
    preferencesRepository: PreferencesRepository,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(currentDisplayName.ifBlank { "Achiever" }) }
    var wakeTime by remember { mutableStateOf("07:00") }
    var sleepTime by remember { mutableStateOf("23:00") }
    var focusTargetMinutes by remember { mutableIntStateOf(60) }
    var enableNotifications by remember { mutableStateOf(true) }

    val goalOptions = listOf(
        "Career & Coding",
        "Deep Focus & Flow",
        "Fitness & Nutrition",
        "Morning Routine Mastery",
        "Reading & Study",
        "Mindfulness"
    )
    val selectedGoals = remember { mutableStateListOf("Career & Coding", "Deep Focus & Flow") }

    fun finishOnboarding() {
        scope.launch {
            repository.getOrCreateUserProfile(displayName, currentEmail)
            preferencesRepository.updateSchedule(wakeTime, sleepTime, focusTargetMinutes)
            preferencesRepository.setTaskReminders(enableNotifications)
            preferencesRepository.setHabitReminders(enableNotifications)
            preferencesRepository.setDailyMotivation(enableNotifications)
            preferencesRepository.setOnboardingCompleted(true)
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraMainBackground)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome to NEXORA",
                    color = NexoraTextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { finishOnboarding() },
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("Skip", color = NexoraPurpleHighlight)
                }
            }

            Text(
                text = "Let's calibrate your growth dashboard and daily rhythm.",
                color = NexoraTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step 1: Name
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "1. How should NEXORA & RICKY address you?",
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraPurplePrimary,
                            unfocusedBorderColor = NexoraGlassCardBorder,
                            focusedTextColor = NexoraTextWhite,
                            unfocusedTextColor = NexoraTextWhite,
                            focusedContainerColor = NexoraCardBackground,
                            unfocusedContainerColor = NexoraCardBackground
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Primary Goals
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "2. Select your main priorities",
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalOptions.forEach { goal ->
                            val isSelected = selectedGoals.contains(goal)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NexoraPurplePrimary else NexoraCardBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) NexoraPurpleHighlight else NexoraGlassCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedGoals.remove(goal)
                                        else selectedGoals.add(goal)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NexoraTextWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = goal,
                                        color = NexoraTextWhite,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Daily schedule
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "3. Preferred Daily Schedule",
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake time", color = NexoraTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = wakeTime,
                                onValueChange = { wakeTime = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NexoraPurplePrimary,
                                    unfocusedBorderColor = NexoraGlassCardBorder,
                                    focusedTextColor = NexoraTextWhite,
                                    unfocusedTextColor = NexoraTextWhite,
                                    focusedContainerColor = NexoraCardBackground,
                                    unfocusedContainerColor = NexoraCardBackground
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sleep time", color = NexoraTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = sleepTime,
                                onValueChange = { sleepTime = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NexoraPurplePrimary,
                                    unfocusedBorderColor = NexoraGlassCardBorder,
                                    focusedTextColor = NexoraTextWhite,
                                    unfocusedTextColor = NexoraTextWhite,
                                    focusedContainerColor = NexoraCardBackground,
                                    unfocusedContainerColor = NexoraCardBackground
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 4: Daily focus target
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "4. Daily Deep Focus Target",
                        color = NexoraTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 60, 90, 120).forEach { mins ->
                            val isSelected = focusTargetMinutes == mins
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
                                    .clickable { focusTargetMinutes = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = NexoraTextWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 5: Notifications
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Smart Reminders",
                            color = NexoraTextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Habit check-ins, focus intervals, and 9 PM motivation",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = enableNotifications,
                        onCheckedChange = { enableNotifications = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraTextWhite,
                            checkedTrackColor = NexoraPurplePrimary,
                            uncheckedTrackColor = NexoraCardBackground
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { finishOnboarding() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("finish_onboarding_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enter NEXORA",
                        color = NexoraTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
