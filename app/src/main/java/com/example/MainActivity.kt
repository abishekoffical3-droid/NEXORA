package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import com.example.ai.RickyAiService
import com.example.data.repository.NexoraPreferences
import com.example.data.repository.NexoraRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.navigation.MainNavigation
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.attemptAutoSignIn
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.theme.LocalNexoraMotion
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraTheme
import com.example.ui.theme.rememberNexoraMotionConfig
import com.example.util.NotificationHelper
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private lateinit var stepTracker: com.example.util.StepTracker
    private lateinit var featuresManager: com.example.util.AdvancedFeaturesManager
    private lateinit var voiceManager: com.example.util.VoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandated: configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        NotificationHelper.initNotificationChannels(this)
        enableEdgeToEdge()

        com.example.util.MotivationalAlertScheduler.scheduleDailyAlerts(this)

        stepTracker = com.example.util.StepTracker.getInstance(this)
        stepTracker.startTracking()
        com.example.util.StepCounterService.start(this)
        featuresManager = com.example.util.AdvancedFeaturesManager(this)
        voiceManager = com.example.util.VoiceManager(this)

        val repository = NexoraRepository(this)
        val preferencesRepository = PreferencesRepository(this)
        val rickyAiService = RickyAiService()

        setContent {
            val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = NexoraPreferences())
            val motionConfig = rememberNexoraMotionConfig(preferences.reduceAnimations)
            CompositionLocalProvider(LocalNexoraMotion provides motionConfig) {
                NexoraTheme(themeMode = preferences.themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        AppRoot(
                            activity = this,
                            repository = repository,
                            preferencesRepository = preferencesRepository,
                            rickyAiService = rickyAiService,
                            stepTracker = stepTracker,
                            featuresManager = featuresManager,
                            voiceManager = voiceManager
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::stepTracker.isInitialized && !stepTracker.stepStats.value.isBackgroundServiceRunning) {
            stepTracker.stopTracking()
        }
        if (::voiceManager.isInitialized) {
            voiceManager.shutdown()
        }
    }
}

@Composable
fun AppRoot(
    activity: ComponentActivity,
    repository: NexoraRepository,
    preferencesRepository: PreferencesRepository,
    rickyAiService: RickyAiService,
    stepTracker: com.example.util.StepTracker,
    featuresManager: com.example.util.AdvancedFeaturesManager,
    voiceManager: com.example.util.VoiceManager
) {
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(activity) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    // Instant launch: if user is cached in Firebase, render immediately with 0 delay
    var isCheckingAuth by remember { mutableStateOf(false) }

    val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = NexoraPreferences())

    // Listen for Firebase Auth changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto sign-in once in background if no user is cached
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = activity,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Ready for manual sign-in on AuthScreen
                },
                scope = coroutineScope
            )
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NexoraPurplePrimary)
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else if (!preferences.onboardingCompleted) {
        OnboardingScreen(
            currentDisplayName = currentUser?.displayName.orEmpty(),
            currentEmail = currentUser?.email.orEmpty(),
            repository = repository,
            preferencesRepository = preferencesRepository,
            onComplete = {
                // Onboarding complete
            }
        )
    } else {
        MainNavigation(
            repository = repository,
            preferencesRepository = preferencesRepository,
            rickyAiService = rickyAiService,
            stepTracker = stepTracker,
            featuresManager = featuresManager,
            voiceManager = voiceManager,
            onSignOut = {
                currentUser = null
            }
        )
    }
}
