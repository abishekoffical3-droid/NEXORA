package com.example.ui.screens.steps

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNexoraMotion
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraWarningAmber
import com.example.util.StepCounterService
import com.example.util.StepStats
import com.example.util.StepTracker
import com.example.widget.NexoraStepWidgetProvider
import java.util.Locale

@Composable
fun StepDashboardScreen(
    stepTracker: StepTracker,
    onNavigateToFoodAi: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val stats by stepTracker.stepStats.collectAsState()
    val motion = LocalNexoraMotion.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var showPermissionDisclosure by remember { mutableStateOf(false) }

    var hasActivityPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            results[Manifest.permission.ACTIVITY_RECOGNITION] == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        } else true
        hasActivityPerm = granted
        if (granted) {
            StepCounterService.start(context)
        }
    }

    fun requestStepPermissionsAndStart() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPerm) {
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            StepCounterService.start(context)
        }
    }

    LaunchedEffect(hasActivityPerm) {
        if (hasActivityPerm && !stats.isBackgroundServiceRunning) {
            StepCounterService.start(context)
        }
    }

    val progress = if (stats.dailyGoal > 0) {
        (stats.stepsToday.toFloat() / stats.dailyGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (motion.reduceAnimations) 0 else 650),
        label = "step_progress"
    )

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step Tracker",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = if (stats.isBackgroundServiceRunning) "24/7 Live Background Tracking Active"
                        else "Daily activity and pedometer telemetry",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Sensor indicator badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (stats.isSensorAvailable) NexoraSuccessGreen.copy(alpha = 0.15f)
                            else NexoraWarningAmber.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (stats.isSensorAvailable) NexoraSuccessGreen
                                else NexoraWarningAmber
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stats.sensorTypeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stats.isSensorAvailable) NexoraSuccessGreen else NexoraWarningAmber
                    )
                }
            }
        }

        // Permission Action Card if Activity Recognition not yet granted
        if (!hasActivityPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NexoraWarningAmber.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Background Motion Sensor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Grant Physical Activity permission for accurate 24/7 background & home widget counting.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { showPermissionDisclosure = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraWarningAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_step_permission_btn")
                        ) {
                            Text("Allow", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Circular Main Progress Hero Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("step_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        CircularProgressRing(
                            progress = animatedProgress,
                            size = 210.dp,
                            strokeWidth = 16.dp,
                            gradientColors = listOf(NexoraPurplePrimary, NexoraPurpleHighlight, NexoraNeonPurpleGlow)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Walking",
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%,d", stats.stepsToday),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Goal: ${String.format(Locale.US, "%,d", stats.dailyGoal)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% completed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (progress >= 1f) NexoraSuccessGreen else NexoraPurpleHighlight,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showGoalDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraPurplePrimary.copy(alpha = 0.2f),
                                contentColor = NexoraPurpleHighlight
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("edit_step_goal_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Goal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { stepTracker.addManualSteps(500) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraPurplePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_500_steps_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Steps", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+500 Steps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Background Tracking & Live Home-Screen Widget Control Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NexoraPurplePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Background Step Service",
                                    tint = NexoraPurpleHighlight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Background Step Service",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = if (stats.isBackgroundServiceRunning) {
                                        "Counting even when NEXORA is closed"
                                    } else {
                                        "Turn on to track steps continuously in background"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = stats.isBackgroundServiceRunning,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    requestStepPermissionsAndStart()
                                } else {
                                    StepCounterService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NexoraPurplePrimary
                            ),
                            modifier = Modifier.testTag("background_step_service_switch")
                        )
                    }

                    Button(
                        onClick = {
                            NexoraStepWidgetProvider.updateAllWidgets(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val provider = ComponentName(context, NexoraStepWidgetProvider::class.java)
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Long-press Home Screen -> Widgets -> NEXORA Live Steps",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Long-press Home Screen -> Widgets -> NEXORA Live Steps",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexoraPurplePrimary.copy(alpha = 0.22f),
                            contentColor = NexoraPurpleHighlight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_step_widget_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Pin Home Widget",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Live Widget to Home Screen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Metrics Trio (Distance, Calories, Active Time)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricGlassCard(
                    title = "Distance",
                    value = String.format(Locale.US, "%.2f", stats.distanceKm),
                    unit = "km",
                    icon = Icons.Default.Speed,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                MetricGlassCard(
                    title = "Calories",
                    value = stats.caloriesKcal.toString(),
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
                MetricGlassCard(
                    title = "Active Time",
                    value = stats.activeMinutes.toString(),
                    unit = "mins",
                    icon = Icons.Default.Timer,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Day History Trend Bar Chart
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Activity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Daily Step Trends",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxSteps = stats.weeklyHistory.maxOfOrNull { it.second }?.coerceAtLeast(stats.dailyGoal) ?: 10000

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.weeklyHistory.forEach { (day, count) ->
                            val barHeightFraction = (count.toFloat() / maxSteps.toFloat()).coerceIn(0.08f, 1f)
                            val reachedGoal = count >= stats.dailyGoal

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (count >= 1000) "${count / 1000}k" else "$count",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((100 * barHeightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = if (reachedGoal) {
                                                    listOf(NexoraSuccessGreen, Color(0xFF059669))
                                                } else {
                                                    listOf(NexoraPurpleHighlight, NexoraPurplePrimary)
                                                }
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Food AI Analyzer Promo Banner
        if (onNavigateToFoodAi != null) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToFoodAi() }
                        .testTag("food_ai_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(NexoraPurplePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = "Food Analyzer",
                                    tint = NexoraPurpleHighlight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scan Meals with Food AI",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = "CameraX instantly detects calories, protein & vitamins",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToFoodAi,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraPurplePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Set Step Goal Dialog
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(stats.dailyGoal.toString()) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = "Set Daily Step Goal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your target number of steps per day:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("step_goal_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = goalInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            stepTracker.setDailyGoal(parsed)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showPermissionDisclosure) {
        AlertDialog(
            onDismissRequest = { showPermissionDisclosure = false },
            title = {
                Text("Activity & Motion Sensor Disclosure", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "NEXORA accesses your device's Physical Activity sensor and runs a low-power foreground health notification to count steps live even when the screen is off and to update your home-screen widget. Your motion sensor data stays on your device and is never sold or shared with advertisers.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDisclosure = false
                        requestStepPermissionsAndStart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
                ) {
                    Text("Continue & Allow", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDisclosure = false }) {
                    Text("Not Now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun MetricGlassCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
