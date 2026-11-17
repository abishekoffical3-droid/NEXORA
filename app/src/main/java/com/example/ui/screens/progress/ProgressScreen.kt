package com.example.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Achievement
import com.example.data.model.FocusSession
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraChipBackground
import com.example.ui.theme.NexoraGlassCardBorder
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import com.example.ui.theme.NexoraWarningAmber
import java.time.LocalDate

@Composable
fun ProgressScreen(
    profile: UserProfile?,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    goals: List<GoalItem>,
    focusSessions: List<FocusSession>
) {
    val currentXp = profile?.xp ?: 0
    val currentLevel = profile?.level ?: "Beginner"
    val streak = profile?.streak ?: 0

    val (nextTierXp, tierProgress) = when {
        currentXp >= 2000 -> Pair(2000, 1.0f)
        currentXp >= 1000 -> Pair(2000, (currentXp - 1000) / 1000f)
        currentXp >= 600 -> Pair(1000, (currentXp - 600) / 400f)
        currentXp >= 300 -> Pair(600, (currentXp - 300) / 300f)
        currentXp >= 100 -> Pair(300, (currentXp - 100) / 200f)
        else -> Pair(100, currentXp / 100f)
    }

    val totalTasksCompleted = tasks.count { it.completed }
    val totalHabitCheckins = habits.sumOf { it.completedDates.size }
    val totalFocusMins = profile?.focusMinutesTotal ?: 0
    val completedGoals = goals.count { it.completed }

    // Dynamic Achievements list computed accurately from real user data
    val achievements = listOf(
        Achievement(
            id = "first_step",
            title = "First Step",
            description = "Complete your first task in NEXORA",
            iconName = "check",
            unlocked = totalTasksCompleted >= 1,
            progress = minOf(totalTasksCompleted, 1),
            target = 1
        ),
        Achievement(
            id = "seven_day_streak",
            title = "Seven-Day Streak",
            description = "Maintain a 7-day daily activity streak",
            iconName = "fire",
            unlocked = streak >= 7,
            progress = minOf(streak, 7),
            target = 7
        ),
        Achievement(
            id = "focus_starter",
            title = "Focus Starter",
            description = "Complete at least 1 deep focus timer session",
            iconName = "timer",
            unlocked = focusSessions.isNotEmpty(),
            progress = minOf(focusSessions.size, 1),
            target = 1
        ),
        Achievement(
            id = "consistency_builder",
            title = "Consistency Builder",
            description = "Log 10 total habit check-ins",
            iconName = "bolt",
            unlocked = totalHabitCheckins >= 10,
            progress = minOf(totalHabitCheckins, 10),
            target = 10
        ),
        Achievement(
            id = "goal_achiever",
            title = "Goal Achiever",
            description = "Reach 100% on a personal goal",
            iconName = "trophy",
            unlocked = completedGoals >= 1,
            progress = minOf(completedGoals, 1),
            target = 1
        ),
        Achievement(
            id = "deep_worker",
            title = "Deep Worker",
            description = "Accumulate 120 total minutes of deep focus",
            iconName = "brain",
            unlocked = totalFocusMins >= 120,
            progress = minOf(totalFocusMins, 120),
            target = 120
        ),
        Achievement(
            id = "month_dedication",
            title = "30-Day Dedication",
            description = "Achieve a continuous 30-day streak",
            iconName = "star",
            unlocked = streak >= 30,
            progress = minOf(streak, 30),
            target = 30
        )
    )

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
                text = "PROGRESS & MASTERY",
                color = NexoraPurpleHighlight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Level & Analytics",
                color = NexoraTextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Current Level Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_level_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NexoraNeonPurpleGlow.copy(alpha = 0.3f))
                                    .border(1.dp, NexoraPurpleHighlight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = NexoraPurpleHighlight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentLevel.uppercase(),
                                    color = NexoraTextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currentXp Total XP",
                                    color = NexoraPurpleHighlight,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NexoraCardBackground)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Next: $nextTierXp XP",
                                color = NexoraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { tierProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NexoraPurplePrimary,
                        trackColor = NexoraCardBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Earn XP by completing daily tasks (+15), habits (+20), and focus sprints (+25).",
                        color = NexoraTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Aggregate Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Tasks Completed",
                    value = "$totalTasksCompleted",
                    icon = Icons.Default.CheckCircle,
                    color = NexoraPurpleHighlight,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Daily Streak",
                    value = "${streak}d",
                    icon = Icons.Default.LocalFireDepartment,
                    color = NexoraWarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Focus Minutes",
                    value = "${totalFocusMins}m",
                    icon = Icons.Default.Timer,
                    color = NexoraSuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Habit Check-ins",
                    value = "$totalHabitCheckins",
                    icon = Icons.Default.AutoAwesome,
                    color = NexoraNeonPurpleGlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Achievements Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACHIEVEMENTS",
                    color = NexoraTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${achievements.count { it.unlocked }} of ${achievements.size} Unlocked",
                    color = NexoraPurpleHighlight,
                    fontSize = 12.sp
                )
            }
        }

        items(achievements, key = { it.id }) { ach ->
            AchievementCard(achievement = ach)
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = NexoraTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = NexoraTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val progressFraction = if (achievement.target > 0) achievement.progress.toFloat() / achievement.target else 0f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_card_${achievement.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.unlocked) NexoraPurplePrimary.copy(alpha = 0.2f)
                        else NexoraCardBackground
                    )
                    .border(
                        1.dp,
                        if (achievement.unlocked) NexoraPurpleHighlight else NexoraGlassCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (achievement.unlocked) NexoraWarningAmber else NexoraTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.title,
                        color = NexoraTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (achievement.unlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NexoraSuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "UNLOCKED",
                                color = NexoraSuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "${achievement.progress}/${achievement.target}",
                            color = NexoraTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = achievement.description,
                    color = NexoraTextSecondary,
                    fontSize = 12.sp
                )

                if (!achievement.unlocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NexoraPurpleHighlight,
                        trackColor = NexoraCardBackground
                    )
                }
            }
        }
    }
}
