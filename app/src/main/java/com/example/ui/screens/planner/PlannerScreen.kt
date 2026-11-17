package com.example.ui.screens.planner

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.RoutineItem
import com.example.data.model.TaskItem
import com.example.data.repository.NexoraRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.RealCalendarView
import com.example.ui.screens.home.AddTaskDialog
import com.example.ui.screens.home.EditTaskDialog
import com.example.ui.screens.home.TaskItemCard
import com.example.ui.theme.NexoraChipBackground
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraWarningAmber
import com.example.util.MissedTaskNotifier
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PlannerScreen(
    tasks: List<TaskItem>,
    routines: List<RoutineItem>,
    repository: NexoraRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("CALENDAR") } // "CALENDAR", "SCHEDULE", "ROUTINES"
    var filterMode by remember { mutableStateOf("TODAY") } // "TODAY", "UPCOMING", "COMPLETED", "ALL"
    var searchQuery by remember { mutableStateOf("") }
    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    val todayStr = LocalDate.now().toString()
    val selectedDateStr = selectedCalendarDate.toString()

    // Automatically check for overdue/missed tasks and send notification
    LaunchedEffect(tasks) {
        MissedTaskNotifier.checkAndNotifyMissedTasks(context, tasks)
    }

    // Overdue tasks
    val overdueTasks = tasks.filter { !it.completed && it.dueDate != null && it.dueDate < todayStr }

    // Completed dates set for calendar indicators
    val completedDates = tasks.filter { it.completed && it.dueDate != null }.map { it.dueDate!! }.toSet()
    val taskDueDates = tasks.mapNotNull { it.dueDate }.toSet()

    // Tasks for the selected calendar date
    val tasksForSelectedCalendarDate = tasks.filter {
        val matchesDate = it.dueDate == selectedDateStr || (selectedDateStr == todayStr && it.dueDate == null)
        val matchesQuery = searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
        matchesDate && matchesQuery
    }

    // Filter tasks based on mode & query in Schedule tab
    val filteredTasks = tasks.filter { task ->
        val matchesQuery = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterMode) {
            "TODAY" -> task.dueDate == null || task.dueDate == todayStr
            "UPCOMING" -> !task.completed && task.dueDate != null && task.dueDate > todayStr
            "COMPLETED" -> task.completed
            else -> true
        }
        matchesQuery && matchesFilter
    }

    // Default template routines if none exist yet
    val displayRoutines = if (routines.isEmpty()) {
        listOf(
            RoutineItem(
                id = "default_morning",
                userId = "",
                title = "Optimal Morning Routine",
                category = "Mind & Energy",
                timeOfDay = "Morning",
                items = listOf("Hydrate 500ml", "10-min sunlight exposure", "Review NEXORA priorities", "Cold splash or shower"),
                active = true
            ),
            RoutineItem(
                id = "default_deep_work",
                userId = "",
                title = "Software & Deep Work Routine",
                category = "Productivity",
                timeOfDay = "Afternoon",
                items = listOf("Silence phone notifications", "Clear desk & full-screen IDE", "Set 50m Pomodoro timer", "Commit clean code"),
                active = true
            ),
            RoutineItem(
                id = "default_evening",
                userId = "",
                title = "Evening Wind-Down",
                category = "Recovery",
                timeOfDay = "Evening",
                items = listOf("Check off daily habits", "Review tomorrow's top 3 tasks", "Blue-light filter on", "Read 15 mins"),
                active = true
            )
        )
    } else {
        routines
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PLANNER & CALENDAR",
                        color = NexoraPurpleHighlight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Daily Architecture",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        if (selectedTab == "ROUTINES") showAddRoutineDialog = true
                        else showAddTaskDialog = true
                    },
                    modifier = Modifier.testTag("planner_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overdue Missed Tasks Alert Banner
            if (overdueTasks.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NexoraErrorRed.copy(alpha = 0.15f),
                    borderColor = NexoraErrorRed.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NexoraErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${overdueTasks.size} Missed / Overdue Task${if (overdueTasks.size > 1) "s" else ""}",
                                    color = NexoraErrorRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scheduled deadlines have passed. Tap to reschedule all to today.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    overdueTasks.forEach { task ->
                                        repository.updateTask(
                                            taskId = task.id,
                                            title = task.title,
                                            category = task.category,
                                            priority = task.priority,
                                            dueDate = todayStr,
                                            dueTime = task.dueTime
                                        )
                                    }
                                    Toast.makeText(context, "Rescheduled ${overdueTasks.size} task(s) to today!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reschedule_overdue_tasks_button")
                        ) {
                            Text("Reschedule", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Tab Selector (Calendar, Schedule, Routines)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                listOf(
                    Triple("CALENDAR", "Calendar", Icons.Default.CalendarMonth),
                    Triple("SCHEDULE", "Schedule (${tasks.size})", Icons.Default.CheckCircle),
                    Triple("ROUTINES", "Routines (${displayRoutines.size})", Icons.Default.Repeat)
                ).forEach { (tabKey, label, _) ->
                    val isSelected = selectedTab == tabKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NexoraPurplePrimary else Color.Transparent)
                            .clickable { selectedTab = tabKey }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // TAB 1: REAL CALENDAR
        if (selectedTab == "CALENDAR") {
            item {
                RealCalendarView(
                    selectedDate = selectedCalendarDate,
                    onDateSelected = { selectedCalendarDate = it },
                    completedDates = completedDates,
                    taskDueDates = taskDueDates
                )
            }

            item {
                // Selected Date Summary & Attendance Header
                val completedOnDate = tasksForSelectedCalendarDate.count { it.completed }
                val totalOnDate = tasksForSelectedCalendarDate.size
                val attendanceRate = if (totalOnDate > 0) (completedOnDate.toFloat() / totalOnDate * 100).toInt() else 0

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCalendarDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalOnDate == 0) "No tasks scheduled for this day"
                                else "Attendance: $completedOnDate of $totalOnDate done ($attendanceRate%)",
                                color = if (attendanceRate == 100 && totalOnDate > 0) NexoraSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_task_for_selected_date_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Task", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (tasksForSelectedCalendarDate.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No tasks for this date",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Add Task' above to plan your day and mark attendance!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(tasksForSelectedCalendarDate, key = { it.id }) { task ->
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
        } else if (selectedTab == "SCHEDULE") {
            // TAB 2: SCHEDULE
            item {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your tasks...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planner_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TODAY", "UPCOMING", "COMPLETED", "ALL").forEach { mode ->
                        val isSel = filterMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSel) NexoraPurpleHighlight else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { filterMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No tasks found",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Tap '+' to schedule a task and mark attendance.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
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
        } else {
            // TAB 3: ROUTINES VIEW
            items(displayRoutines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    onToggleStep = { step ->
                        scope.launch {
                            if (routine.id.startsWith("default_")) {
                                val res = repository.createRoutine(
                                    routine.title,
                                    routine.category,
                                    routine.timeOfDay,
                                    routine.items
                                )
                                val newId = res.getOrNull()
                                if (newId != null) {
                                    repository.toggleRoutineStep(newId, step)
                                }
                            } else {
                                repository.toggleRoutineStep(routine.id, step)
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repository.deleteRoutine(routine.id)
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
        val taskDueDate = if (selectedTab == "CALENDAR") selectedDateStr else todayStr
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category, priority, dueTime ->
                scope.launch {
                    repository.createTask(
                        title = title,
                        category = category,
                        priority = priority,
                        dueDate = taskDueDate,
                        dueTime = dueTime
                    )
                    showAddTaskDialog = false
                    Toast.makeText(context, "Task created for $taskDueDate! ⏰", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAddRoutineDialog) {
        AddRoutineDialog(
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { title, cat, timeOfDay, routineItems ->
                scope.launch {
                    repository.createRoutine(
                        title = title,
                        category = cat,
                        timeOfDay = timeOfDay,
                        items = routineItems
                    )
                    showAddRoutineDialog = false
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
fun RoutineCard(
    routine: RoutineItem,
    onToggleStep: (String) -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_card_${routine.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = routine.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${routine.timeOfDay} • ${routine.category}",
                        color = NexoraPurpleHighlight,
                        fontSize = 12.sp
                    )
                }

                if (!routine.id.startsWith("default_")) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Routine",
                            tint = NexoraTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            routine.items.forEach { step ->
                val isChecked = routine.completedItems.contains(step)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onToggleStep(step) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleStep(step) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NexoraPurplePrimary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = step,
                        color = if (isChecked) NexoraTextMuted else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Productivity") }
    var timeOfDay by remember { mutableStateOf("Morning") }
    var itemInput by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "New Routine",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Routine title (e.g. Morning Sprint)...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraPurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Time of Day", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Morning", "Afternoon", "Evening", "Night").forEach { tod ->
                        val isSel = timeOfDay == tod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { timeOfDay = tod }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tod, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Steps / Actions", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = itemInput,
                        onValueChange = { itemInput = it },
                        placeholder = { Text("Add step...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraPurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (itemInput.isNotBlank()) {
                                items.add(itemInput.trim())
                                itemInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                items.forEachIndexed { idx, itm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}. $itm", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        IconButton(onClick = { items.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = NexoraErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, category, timeOfDay, items.toList())
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                    ) {
                        Text("Save Routine", color = Color.White)
                    }
                }
            }
        }
    }
}
