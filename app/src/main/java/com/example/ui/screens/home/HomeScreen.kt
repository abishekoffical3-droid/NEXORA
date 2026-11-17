package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FocusSession
import com.example.data.model.HabitItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.repository.NexoraRepository
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.GlassCard
import com.example.ui.screens.profile.CreateGoalDialog
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraChipBackground
import com.example.ui.theme.NexoraErrorRed
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    profile: UserProfile?,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    focusSessions: List<FocusSession>,
    repository: NexoraRepository,
    onStartFocus: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProgress: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    val todayStr = LocalDate.now().toString()
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }

    val todayTasks = tasks.filter { it.dueDate == null || it.dueDate == todayStr }
    val completedCount = todayTasks.count { it.completed }
    val totalCount = todayTasks.size
    val dailyProgress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val percentInt = (dailyProgress * 100).toInt()

    // Focus stats today
    val todayFocusMinutes = focusSessions.filter { it.createdAt != null }.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting,",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = profile?.displayName?.ifBlank { "Achiever" } ?: "Achiever",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = todayFormatted,
                        color = NexoraPurpleHighlight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NexoraWarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile?.xp ?: 0} XP",
                                color = NexoraTextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NexoraCardBackground)
                            .border(1.5.dp, NexoraPurplePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.displayName?.take(1) ?: "A").uppercase(),
                            color = NexoraTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Daily Progress Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_progress_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAILY MOMENTUM",
                            color = NexoraPurpleHighlight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (totalCount == 0) "No tasks yet today" else "$completedCount of $totalCount tasks completed",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (percentInt == 100 && totalCount > 0) "Outstanding! All tasks achieved." else "+${completedCount * 15} XP accumulated today",
                            color = if (percentInt == 100) NexoraSuccessGreen else NexoraTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    CircularProgressRing(
                        progress = dailyProgress,
                        size = 84.dp,
                        strokeWidth = 8.dp,
                        centerText = "$percentInt%"
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.Add,
                    label = "Task",
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Habit",
                    onClick = { showAddHabitDialog = true },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.Flag,
                    label = "Goal",
                    onClick = { showAddGoalDialog = true },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.Timer,
                    label = "Focus",
                    onClick = onStartFocus,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.CalendarMonth,
                    label = "Plan",
                    onClick = onNavigateToPlanner,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Habit Streak Mini-Week
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = NexoraWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HABIT STREAK",
                                color = NexoraPurpleHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "Streak: ${profile?.streak ?: 0}d (Best: ${profile?.bestStreak ?: 0}d)",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7-day mini calendar display
                    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { date ->
                            val isToday = date == LocalDate.now()
                            val dateStr = date.toString()
                            val dayCompleted = habits.any { it.completedDates.contains(dateStr) }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = date.dayOfWeek.name.take(2),
                                    color = if (isToday) NexoraPurpleHighlight else NexoraTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                dayCompleted -> NexoraPurplePrimary
                                                isToday -> NexoraCardBackground
                                                else -> Color(0x33171722)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isToday) NexoraPurpleHighlight else NexoraGlassCardBorder,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NexoraTextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${date.dayOfMonth}",
                                            color = if (isToday) NexoraTextWhite else NexoraTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Deep Focus Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FOCUS PROTOCOL",
                                color = NexoraPurpleHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${profile?.focusMinutesTotal ?: 0} mins logged",
                            color = NexoraTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: 60 mins/day",
                            color = NexoraTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onStartFocus,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("home_start_focus_button")
                    ) {
                        Text("Start Timer", color = NexoraTextWhite, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Today's Tasks Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TASKS",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                IconButton(
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier.testTag("home_add_task_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = NexoraPurpleHighlight
                    )
                }
            }
        }

        if (todayTasks.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NexoraPurpleHighlight.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All clear for today!",
                            color = NexoraTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Add tasks or tap RICKY to plan your day.",
                            color = NexoraTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onToggle = {
                        scope.launch {
                            repository.toggleTaskCompletion(task)
                        }
                    },
                    onEdit = { taskToEdit = task },
                    onDelete = {
                        scope.launch {
                            repository.deleteTask(task.id)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category, priority, dueTime ->
                scope.launch {
                    repository.createTask(
                        title = title,
                        category = category,
                        priority = priority,
                        dueDate = todayStr,
                        dueTime = dueTime
                    )
                    showAddTaskDialog = false
                }
            }
        )
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { title, frequency ->
                scope.launch {
                    repository.createHabit(
                        title = title,
                        frequency = frequency
                    )
                    showAddHabitDialog = false
                }
            }
        )
    }

    if (showAddGoalDialog) {
        CreateGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, desc, targetDate, category, milestones ->
                scope.launch {
                    repository.createGoal(title, desc, targetDate, category, milestones)
                    showAddGoalDialog = false
                }
            }
        )
    }

    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, category, priority, dueTime ->
                scope.launch {
                    repository.updateTask(
                        taskId = task.id,
                        title = title,
                        category = category,
                        priority = priority,
                        dueDate = task.dueDate,
                        dueTime = dueTime
                    )
                    taskToEdit = null
                }
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    var showAlarmDialog by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority.uppercase()) {
        "URGENT" -> NexoraErrorRed
        "HIGH" -> NexoraWarningAmber
        "LOW" -> NexoraSuccessGreen
        else -> NexoraPurpleHighlight
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NexoraSuccessGreen,
                    uncheckedColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("task_attendance_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        color = if (task.completed) NexoraTextMuted else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.completed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NexoraSuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ATTENDANCE TICKED",
                                color = NexoraSuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            color = priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!task.dueTime.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = task.dueTime,
                                color = NexoraPurpleHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Alarm Clock Button
            IconButton(
                onClick = { showAlarmDialog = true },
                modifier = Modifier
                    .testTag("task_alarm_button_${task.id}")
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Set Task Alarm",
                    tint = NexoraPurpleHighlight,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = NexoraTextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showAlarmDialog) {
        com.example.ui.components.SetAlarmDialog(
            taskId = task.id,
            taskTitle = task.title,
            onDismiss = { showAlarmDialog = false }
        )
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NexoraCardBackground)
            .border(1.dp, NexoraGlassCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexoraPurpleHighlight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = NexoraTextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueTime by remember { mutableStateOf("") }

    val categories = listOf("Personal", "Work", "Study", "Health", "Code")
    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "New Task",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task title...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_task_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                        focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraPurplePrimary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { category = cat }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = NexoraTextWhite, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", color = NexoraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSel = priority == prio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraNeonPurpleGlow else NexoraCardBackground)
                                .clickable { priority = prio }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prio, color = NexoraTextWhite, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it },
                    placeholder = { Text("Time (optional, e.g. 14:30)", color = NexoraTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground)
                    ) {
                        Text("Cancel", color = NexoraTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, category, priority, dueTime.ifBlank { null })
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        modifier = Modifier.testTag("dialog_confirm_add_task_button")
                    ) {
                        Text("Create", color = NexoraTextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexoraMainBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraGlassCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "New Habit",
                    color = NexoraTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Read 20 pages, Code 1 hour", color = NexoraTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_habit_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Frequency", color = NexoraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DAILY", "WEEKLY").forEach { freq ->
                        val isSel = frequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraPurplePrimary else NexoraCardBackground)
                                .clickable { frequency = freq }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(freq, color = NexoraTextWhite, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground)
                    ) {
                        Text("Cancel", color = NexoraTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, frequency)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        modifier = Modifier.testTag("dialog_confirm_add_habit_button")
                    ) {
                        Text("Create", color = NexoraTextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var category by remember { mutableStateOf(task.category) }
    var priority by remember { mutableStateOf(task.priority) }
    var dueTime by remember { mutableStateOf(task.dueTime ?: "") }

    val categories = listOf("Personal", "Work", "Study", "Health", "Code")
    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexoraMainBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraGlassCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Task",
                    color = NexoraTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task title...", color = NexoraTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_edit_task_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", color = NexoraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraPurplePrimary else NexoraCardBackground)
                                .clickable { category = cat }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = NexoraTextWhite, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", color = NexoraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSel = priority == prio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraNeonPurpleGlow else NexoraCardBackground)
                                .clickable { priority = prio }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prio, color = NexoraTextWhite, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueTime,
                    onValueChange = { dueTime = it },
                    placeholder = { Text("Time (optional, e.g. 14:30)", color = NexoraTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = NexoraGlassCardBorder,
                        focusedTextColor = NexoraTextWhite,
                        unfocusedTextColor = NexoraTextWhite,
                        focusedContainerColor = NexoraCardBackground,
                        unfocusedContainerColor = NexoraCardBackground
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCardBackground)
                    ) {
                        Text("Cancel", color = NexoraTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, category, priority, dueTime.ifBlank { null })
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        modifier = Modifier.testTag("dialog_confirm_edit_task_button")
                    ) {
                        Text("Save Changes", color = NexoraTextWhite)
                    }
                }
            }
        }
    }
}

