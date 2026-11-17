package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.RickyAiService
import com.example.data.model.FocusSession
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.RickyMessage
import com.example.data.model.RoutineItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.repository.NexoraPreferences
import com.example.data.repository.NexoraRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.components.RickyChatOverlay
import com.example.ui.components.RickyFloatingButton
import com.example.ui.screens.focus.FocusTimerDialog
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.hub.AdvancedHubScreen
import com.example.ui.screens.planner.PlannerScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraGlassCardBorder
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextWhite
import com.example.util.AdvancedFeaturesManager
import com.example.util.StepTracker
import com.example.util.VoiceManager

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    STEPS("Steps", Icons.Default.DirectionsRun),
    HUB("Hub", Icons.Default.Bolt),
    FOOD_AI("Food AI", Icons.Default.Fastfood),
    PLANNER("Planner", Icons.Default.CalendarMonth),
    PROGRESS("Progress", Icons.Default.TrendingUp),
    PROFILE("Profile", Icons.Default.Person),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainNavigation(
    repository: NexoraRepository,
    preferencesRepository: PreferencesRepository,
    rickyAiService: RickyAiService,
    stepTracker: StepTracker,
    featuresManager: AdvancedFeaturesManager,
    voiceManager: VoiceManager,
    onSignOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var showRickyOverlay by remember { mutableStateOf(false) }
    var showFocusTimerDialog by remember { mutableStateOf(false) }

    // Live streams from repository
    val profile by repository.observeUserProfile().collectAsState(initial = null)
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())
    val habits by repository.observeHabits().collectAsState(initial = emptyList())
    val goals by repository.observeGoals().collectAsState(initial = emptyList())
    val routines by repository.observeRoutines().collectAsState(initial = emptyList())
    val focusSessions by repository.observeFocusSessions().collectAsState(initial = emptyList())
    val rickyMessages by repository.observeRickyMessages().collectAsState(initial = emptyList())
    val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = NexoraPreferences())

    // Back handling: if on secondary tab, return to Home tab
    if (currentTab != MainTab.HOME) {
        BackHandler {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 560.dp) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NexoraTextWhite,
                                    selectedTextColor = NexoraPurpleHighlight,
                                    indicatorColor = NexoraPurplePrimary,
                                    unselectedIconColor = NexoraTextMuted,
                                    unselectedTextColor = NexoraTextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .widthIn(min = 66.dp)
                                        .heightIn(min = 52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) NexoraPurplePrimary else Color.Transparent)
                                        .clickable { currentTab = tab }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("nav_tab_${tab.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) NexoraTextWhite else NexoraTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else NexoraTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main tab content
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        profile = profile,
                        tasks = tasks,
                        habits = habits,
                        focusSessions = focusSessions,
                        repository = repository,
                        onStartFocus = { showFocusTimerDialog = true },
                        onNavigateToPlanner = { currentTab = MainTab.PLANNER },
                        onNavigateToProgress = { currentTab = MainTab.PROGRESS }
                    )
                }
                MainTab.STEPS -> {
                    com.example.ui.screens.steps.StepDashboardScreen(
                        stepTracker = stepTracker,
                        onNavigateToFoodAi = { currentTab = MainTab.FOOD_AI }
                    )
                }
                MainTab.HUB -> {
                    AdvancedHubScreen(
                        featuresManager = featuresManager,
                        voiceManager = voiceManager,
                        stepTracker = stepTracker,
                        rickyAiService = rickyAiService,
                        tasks = tasks,
                        habits = habits,
                        onOpenRickyVoiceChat = { showRickyOverlay = true }
                    )
                }
                MainTab.FOOD_AI -> {
                    com.example.ui.screens.food.FoodAnalyzerCameraScreen(
                        rickyAiService = rickyAiService,
                        onBack = { currentTab = MainTab.HOME }
                    )
                }
                MainTab.PLANNER -> {
                    PlannerScreen(
                        tasks = tasks,
                        routines = routines,
                        repository = repository
                    )
                }
                MainTab.PROGRESS -> {
                    ProgressScreen(
                        profile = profile,
                        tasks = tasks,
                        habits = habits,
                        goals = goals,
                        focusSessions = focusSessions
                    )
                }
                MainTab.PROFILE -> {
                    ProfileScreen(
                        profile = profile,
                        goals = goals,
                        repository = repository,
                        onSignOut = onSignOut
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        preferencesRepository = preferencesRepository,
                        repository = repository,
                        rickyAiService = rickyAiService
                    )
                }
            }

            // Floating RICKY Assistant Button on the LEFT side above the bottom bar
            RickyFloatingButton(
                onClick = { showRickyOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )

            // Expandable Ricky Chat Overlay
            if (showRickyOverlay) {
                RickyChatOverlay(
                    onDismiss = { showRickyOverlay = false },
                    repository = repository,
                    rickyAiService = rickyAiService,
                    messages = rickyMessages,
                    userProfile = profile,
                    tasks = tasks,
                    habits = habits,
                    goals = goals,
                    allowPersonalization = preferences.aiPersonalizationAllowed
                )
            }

            // Focus Timer Dialog
            if (showFocusTimerDialog) {
                FocusTimerDialog(
                    tasks = tasks,
                    repository = repository,
                    onDismiss = { showFocusTimerDialog = false }
                )
            }
        }
    }
}
