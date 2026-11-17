package com.example.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNexoraMotion
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraMainBackground
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraTextMuted
import com.example.ui.theme.NexoraTextSecondary
import com.example.ui.theme.NexoraTextWhite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "AuthScreen"

private data class LiveFeatureHighlight(
    val icon: ImageVector,
    val badge: String,
    val headline: String,
    val accent: Color
)

private data class StarParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var tickerIndex by remember { mutableIntStateOf(0) }

    val highlights = remember {
        listOf(
            LiveFeatureHighlight(
                Icons.Default.AutoAwesome,
                "RICKY AI • ULTRA FAST",
                "Smart coaching in Discipline, Quotes, Health, Study & Feelings",
                Color(0xFFC084FC)
            ),
            LiveFeatureHighlight(
                Icons.Default.DirectionsRun,
                "24/7 STEP TELEMETRY",
                "Accurate background pedometer & live home-screen widget",
                Color(0xFF34D399)
            ),
            LiveFeatureHighlight(
                Icons.Default.PhotoCamera,
                "FOOD AI CAMERA",
                "Scan any meal with CameraX for calories, vitamins & protein",
                Color(0xFF38BDF8)
            ),
            LiveFeatureHighlight(
                Icons.Default.MusicNote,
                "CUSTOM ALARMS",
                "Wake up to 4 built-in studio chimes or your own local music",
                Color(0xFFFBBF24)
            )
        )
    }

    LaunchedEffect(Unit) {
        visible = true
        while (true) {
            delay(2800L)
            tickerIndex = (tickerIndex + 1) % highlights.size
        }
    }

    val motion = LocalNexoraMotion.current
    val animateOrbs = motion.shouldAnimateContinuousOrbs

    val infiniteTransition = rememberInfiniteTransition(label = "auth_live_bg")
    val cosmicPhase by if (animateOrbs) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cosmic_phase"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }
    val orbitRotation by if (animateOrbs) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbit_rotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val emblemFloat by if (animateOrbs && !motion.isLowEndDevice) {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "emblem_float"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val shimmerOffset by if (animateOrbs && !motion.isLowEndDevice) {
        infiniteTransition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "button_shimmer"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val particleLimit = motion.maxParticles
    val particles = remember(particleLimit) {
        val colors = listOf(
            Color(0xFFA855F7),
            Color(0xFF38BDF8),
            Color(0xFFE879F9),
            Color(0xFF818CF8)
        )
        List(particleLimit) { i ->
            StarParticle(
                xFraction = ((i * 41) % 100) / 100f,
                yFraction = ((i * 59 + 17) % 100) / 100f,
                radius = 2f + (i % 3) * 1.3f,
                speed = 0.6f + (i % 4) * 0.25f,
                phase = (i * 0.9f),
                color = colors[i % colors.size]
            )
        }
    }

    // Cached gradient brushes so 60fps frames allocate ZERO Shader/Brush objects on Android 8+ GPUs
    val outerSweepBrush = remember {
        Brush.sweepGradient(
            listOf(Color.Transparent, NexoraNeonPurpleGlow, NexoraPurpleHighlight, Color.Transparent)
        )
    }
    val innerSweepBrush = remember {
        Brush.sweepGradient(
            listOf(Color(0xFF38BDF8), Color.Transparent, NexoraPurplePrimary, Color.Transparent)
        )
    }
    val logoGlowBrush = remember {
        Brush.radialGradient(
            listOf(NexoraNeonPurpleGlow.copy(alpha = 0.5f), Color(0xFF1E103C))
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070512), Color(0xFF140B2D), Color(0xFF090616))
                )
            )
    ) {
        val isCompactPhone = maxHeight < 680.dp || maxWidth < 360.dp
        val emblemBoxSize = if (isCompactPhone) 104.dp else 132.dp
        val innerRingSize = if (isCompactPhone) 84.dp else 106.dp
        val logoCircleSize = if (isCompactPhone) 70.dp else 88.dp
        val logoImgSize = if (isCompactPhone) 58.dp else 72.dp
        val titleSize = if (isCompactPhone) 28.sp else 36.sp

        // Hardware-layered Aurora Orbs (drawWithCache + graphicsLayer translation = 0 per-frame allocation)
        Box(
            modifier = Modifier
                .size(if (isCompactPhone) 240.dp else 320.dp)
                .graphicsLayer {
                    translationX = size.width * (0.05f + 0.14f * cos(cosmicPhase))
                    translationY = size.height * (0.04f + 0.08f * sin(cosmicPhase))
                }
                .drawWithCache {
                    val orbBrush = Brush.radialGradient(
                        colors = listOf(Color(0x527C3AED), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 2f
                    )
                    onDrawBehind {
                        drawCircle(brush = orbBrush)
                    }
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(if (isCompactPhone) 220.dp else 300.dp)
                .graphicsLayer {
                    translationX = size.width * (-0.05f + 0.12f * sin(cosmicPhase * 0.8f))
                    translationY = size.height * (-0.04f + 0.10f * cos(cosmicPhase * 0.9f))
                }
                .drawWithCache {
                    val orbBrush = Brush.radialGradient(
                        colors = listOf(Color(0x400EA5E9), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 2f
                    )
                    onDrawBehind {
                        drawCircle(brush = orbBrush)
                    }
                }
        )

        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                particles.forEach { p ->
                    val yOffset = ((p.yFraction - (cosmicPhase / (2f * PI.toFloat())) * p.speed) % 1f + 1f) % 1f
                    val xWobble = p.xFraction + 0.02f * sin(cosmicPhase * 2f + p.phase)
                    val alpha = 0.25f + 0.42f * (0.5f + 0.5f * sin(cosmicPhase * 3f + p.phase))
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.radius,
                        center = Offset(xWobble * w, yOffset * h)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isCompactPhone) 16.dp else 24.dp, vertical = if (isCompactPhone) 12.dp else 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
        ) {
            // Live Status Pill
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(if (motion.reduceAnimations) 0 else 450)) +
                    slideInVertically(tween(if (motion.reduceAnimations) 0 else 450)) { -20 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NexoraPurplePrimary.copy(alpha = 0.18f))
                        .border(1.dp, NexoraPurpleHighlight.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NexoraSuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT-GEN AI LIFE OS",
                        color = NexoraPurpleHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactPhone) 10.dp else 18.dp))

            // Levitating Emblem with GPU-Rotated Cached Orbital Rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(emblemBoxSize)
                    .graphicsLayer { translationY = emblemFloat }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = orbitRotation }
                ) {
                    val stroke = 3.dp.toPx()
                    drawArc(
                        brush = outerSweepBrush,
                        startAngle = 0f,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(innerRingSize)
                        .graphicsLayer { rotationZ = -orbitRotation * 1.4f }
                        .border(
                            width = 1.5.dp,
                            brush = innerSweepBrush,
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(logoCircleSize)
                        .clip(CircleShape)
                        .background(logoGlowBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nexora_logo),
                        contentDescription = "NEXORA Logo",
                        modifier = Modifier
                            .size(logoImgSize)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactPhone) 10.dp else 14.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(if (motion.reduceAnimations) 0 else 550)) +
                    slideInVertically(tween(if (motion.reduceAnimations) 0 else 550)) { 24 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEXORA",
                        color = NexoraTextWhite,
                        fontSize = titleSize,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "Habit, Health & Discipline Studio",
                        color = NexoraPurpleHighlight,
                        fontSize = if (isCompactPhone) 13.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Rotating Spotlight Feature Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = highlights[tickerIndex].accent.copy(alpha = 0.45f)
            ) {
                AnimatedContent(
                    targetState = highlights[tickerIndex],
                    transitionSpec = {
                        (fadeIn(tween(350)) + slideInVertically { it / 3 }) togetherWith fadeOut(tween(250))
                    },
                    label = "live_feature_ticker"
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(item.accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.badge,
                                tint = item.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.badge,
                                color = item.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = item.headline,
                                color = NexoraTextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value props card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { 36 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        FeatureRow(text = "24/7 Background Step Counter & Live Home Widget")
                        FeatureRow(text = "Ultra-Fast RICKY AI (Discipline, Study, Health & Feelings)")
                        FeatureRow(text = "CameraX Food Nutrition Analyzer & Calorie Tracking")
                        FeatureRow(text = "Custom Music Alarms, Pomodoro & Cloud Sync")
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Animated Shimmer Sign-In with Google Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NexoraPurplePrimary,
                                NexoraNeonPurpleGlow,
                                NexoraPurplePrimary
                            ),
                            start = Offset(shimmerOffset * 600f - 200f, 0f),
                            end = Offset(shimmerOffset * 600f + 200f, 200f)
                        )
                    )
            ) {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = NexoraCardBackground
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NexoraTextWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = NexoraTextWhite
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                color = NexoraTextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: "",
                    color = NexoraErrorRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secured with Firebase Enterprise & Credential Manager",
                color = NexoraTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = NexoraPurpleHighlight,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = NexoraTextWhite,
            fontSize = 13.sp
        )
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun performSignOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
