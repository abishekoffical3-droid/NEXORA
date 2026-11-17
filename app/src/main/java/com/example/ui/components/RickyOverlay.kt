package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ai.RickyAiService
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.RickyMessage
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.repository.NexoraRepository
import com.example.ui.theme.LocalNexoraMotion
import com.example.ui.theme.NexoraCardBackground
import com.example.ui.theme.NexoraChipBackground
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleDark
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraTextMuted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

enum class RickyCoachingSkill(
    val title: String,
    val emoji: String,
    val icon: ImageVector,
    val tagLine: String,
    val prompts: List<String>
) {
    DISCIPLINE(
        title = "Discipline",
        emoji = "🔥",
        icon = Icons.Default.LocalFireDepartment,
        tagLine = "Iron willpower, 5-second rule & anti-procrastination",
        prompts = listOf(
            "How do I stay disciplined even when I have zero motivation?",
            "Teach me the 5-Second Rule to defeat procrastination right now",
            "Build me a strict morning discipline checklist"
        )
    ),
    QUOTES(
        title = "Quotes",
        emoji = "💬",
        icon = Icons.Default.FormatQuote,
        tagLine = "Powerful motivational quotes & mindset fuel",
        prompts = listOf(
            "Give me 3 powerful quotes about discipline & hard work with action steps",
            "Motivate me right now to never give up on my goals",
            "Give me an intense daily stoic quote to conquer fear"
        )
    ),
    HEALTH(
        title = "Health",
        emoji = "🍏",
        icon = Icons.Default.Spa,
        tagLine = "Sleep, hydration, nutrition & breathing energy",
        prompts = listOf(
            "How do I optimize my sleep, water intake & daily energy?",
            "Give me a clean daily routine for nutrition and walking steps",
            "Guide me through 4-7-8 breathing for instant calm & focus"
        )
    ),
    STUDY(
        title = "Study",
        emoji = "📚",
        icon = Icons.Default.School,
        tagLine = "Feynman technique, active recall & Pomodoro",
        prompts = listOf(
            "Teach me how to remember what I study using Active Recall",
            "Create a high-focus 2-hour Pomodoro study sprint plan",
            "How can I understand hard subjects faster with the Feynman Technique?"
        )
    ),
    FEELINGS(
        title = "Feelings",
        emoji = "💜",
        icon = Icons.Default.Favorite,
        tagLine = "Empathy, stress relief, emotions & mental reset",
        prompts = listOf(
            "I feel overwhelmed and stressed today—help me reset my mind",
            "How do I handle feeling lonely, sad, or unmotivated?",
            "Help me turn self-doubt and overthinking into calm confidence"
        )
    ),
    HABITS(
        title = "Habits",
        emoji = "⚡",
        icon = Icons.Default.Repeat,
        tagLine = "Cue-routine-reward, 2-min rule & habit stacking",
        prompts = listOf(
            "Explain the Cue -> Routine -> Reward habit loop",
            "How do I use Habit Stacking and the 2-Minute Rule?",
            "How do I bounce back after breaking a habit streak?"
        )
    )
}

@Composable
fun RickyFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalNexoraMotion.current
    val scale = if (motion.shouldAnimateContinuousOrbs) {
        val infiniteTransition = rememberInfiniteTransition(label = "ricky_pulse")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        anim
    } else 1f

    val borderBrush = remember {
        Brush.radialGradient(
            listOf(NexoraPurpleHighlight, NexoraNeonPurpleGlow, NexoraPurpleDark)
        )
    }
    val bgBrush = remember {
        Brush.linearGradient(
            listOf(NexoraCardBackground, Color(0xFF2E1065))
        )
    }

    Box(
        modifier = modifier
            .testTag("ricky_floating_button")
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .border(
                BorderStroke(2.dp, borderBrush),
                CircleShape
            )
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ricky_avatar),
            contentDescription = "RICKY AI Assistant",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Small AI badge overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(NexoraNeonPurpleGlow)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RickyChatOverlay(
    onDismiss: () -> Unit,
    repository: NexoraRepository,
    rickyAiService: RickyAiService,
    messages: List<RickyMessage>,
    userProfile: UserProfile?,
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    goals: List<GoalItem>,
    allowPersonalization: Boolean
) {
    val context = LocalContext.current
    val motion = LocalNexoraMotion.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var currentStreamingText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastSentPrompt by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf<RickyCoachingSkill?>(null) }
    var messageToReport by remember { mutableStateOf<String?>(null) }

    // Optimistic local cache so user messages & replies appear instantaneously without waiting for Firestore round-trips
    val localMessages = remember { mutableStateListOf<RickyMessage>() }

    // Merge persisted messages with any optimistic messages not yet present in Firestore snapshot
    val displayedMessages = remember(messages, localMessages.toList()) {
        val persistedContents = messages.map { "${it.role}:${it.content}" }.toSet()
        val extraLocal = localMessages.filter { "${it.role}:${it.content}" !in persistedContents }
        messages + extraLocal
    }

    val listState = rememberLazyListState()

    LaunchedEffect(displayedMessages.size) {
        if (displayedMessages.isNotEmpty()) {
            if (motion.reduceAnimations || motion.isLowEndDevice) {
                listState.scrollToItem(displayedMessages.size - 1)
            } else {
                listState.animateScrollToItem(displayedMessages.size - 1)
            }
        }
    }

    LaunchedEffect(currentStreamingText.length / 40) {
        if (currentStreamingText.isNotEmpty() && displayedMessages.isNotEmpty()) {
            listState.scrollToItem(displayedMessages.size)
        }
    }

    fun sendMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || isGenerating) return

        lastSentPrompt = trimmed
        inputText = ""
        errorMessage = null
        isGenerating = true
        currentStreamingText = ""

        // Show user message instantly without blocking on Firestore
        localMessages.add(
            RickyMessage(
                id = "local_${UUID.randomUUID()}",
                role = "user",
                content = trimmed
            )
        )

        // Persist user message asynchronously in background
        scope.launch {
            try {
                repository.saveRickyMessage("user", trimmed)
            } catch (_: Exception) {}
        }

        // Immediately start streaming RICKY's response
        scope.launch {
            try {
                val collected = StringBuilder()
                rickyAiService.streamResponse(
                    userPrompt = trimmed,
                    profile = userProfile,
                    tasks = tasks,
                    habits = habits,
                    goals = goals,
                    allowPersonalization = allowPersonalization
                ).catch { error ->
                    isGenerating = false
                    errorMessage = "Ricky is temporarily unreachable: ${error.localizedMessage ?: "Check internet connection"}"
                }.collect { chunk ->
                    collected.append(chunk)
                    currentStreamingText = collected.toString()
                }

                val finalReply = collected.toString().trim()
                if (finalReply.isNotBlank()) {
                    localMessages.add(
                        RickyMessage(
                            id = "local_${UUID.randomUUID()}",
                            role = "model",
                            content = finalReply
                        )
                    )
                    currentStreamingText = ""
                    try {
                        repository.saveRickyMessage("model", finalReply)
                    } catch (_: Exception) {}
                } else if (errorMessage == null) {
                    errorMessage = "Ricky didn't receive a response. Tap Retry."
                }
            } catch (e: Exception) {
                errorMessage = "Ricky couldn't respond: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isGenerating = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .imePadding()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ricky_avatar),
                            contentDescription = "Ricky",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, NexoraPurplePrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RICKY",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NexoraChipBackground)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AI COACH",
                                        color = NexoraPurpleHighlight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = if (isGenerating) "Thinking ultra-fast..." else "Online • Ultra-Fast AI",
                                color = if (isGenerating) NexoraPurpleHighlight else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                localMessages.clear()
                                currentStreamingText = ""
                                errorMessage = null
                                scope.launch {
                                    repository.clearRickyHistory()
                                }
                            },
                            modifier = Modifier.testTag("clear_ricky_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_ricky_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ricky Coaching Skills Selector Tabs
                Text(
                    text = "RICKY'S COACHING SKILLS",
                    color = NexoraPurpleHighlight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RickyCoachingSkill.entries) { skill ->
                        val isSelected = selectedSkill == skill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) NexoraPurplePrimary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NexoraPurpleHighlight else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSkill = if (isSelected) null else skill
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = skill.emoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = skill.title,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // If a skill is selected, show its prompt lessons
                selectedSkill?.let { skill ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, NexoraPurpleHighlight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "${skill.emoji} ${skill.title} Masterclass: ${skill.tagLine}",
                                color = NexoraPurpleHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                skill.prompts.forEach { prompt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NexoraPurplePrimary.copy(alpha = 0.15f))
                                            .border(1.dp, NexoraPurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .clickable { sendMessage(prompt) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = prompt,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (displayedMessages.isEmpty() && currentStreamingText.isEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Hey there! I'm RICKY 👋",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "I'm your ultra-fast coach in NEXORA, trained deeply in Discipline, Motivational Quotes, Health, Study, Feelings, and Habits. Pick a pillar above or ask me anything!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Quick coaching starters:",
                                        color = NexoraPurpleHighlight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val suggestions = listOf(
                                        "Give me a powerful discipline quote",
                                        "Build an atomic study routine",
                                        "How to fix my sleep schedule?",
                                        "I feel stressed—help me reset",
                                        "Stop procrastinating right now"
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        suggestions.forEach { prompt ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                                    .clickable { sendMessage(prompt) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = prompt,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(displayedMessages) { msg ->
                        MessageBubble(
                            role = msg.role,
                            text = msg.content,
                            onReportAiResponse = if (msg.role != "user") {
                                { messageToReport = msg.content }
                            } else null
                        )
                    }

                    // Active streaming token response
                    if (currentStreamingText.isNotEmpty()) {
                        item {
                            MessageBubble(
                                role = "model",
                                text = currentStreamingText,
                                isStreaming = true
                            )
                        }
                    }

                    // Error banner with retry
                    if (errorMessage != null) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = NexoraErrorRed.copy(alpha = 0.4f),
                                backgroundColor = Color(0x33F87171)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = errorMessage ?: "Error generating response",
                                        color = NexoraErrorRed,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (lastSentPrompt.isNotBlank()) {
                                        IconButton(
                                            onClick = { sendMessage(lastSentPrompt) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ricky_input_field"),
                        placeholder = {
                            Text(
                                text = "Ask Ricky: Discipline, Quotes, Health, Study, Feelings...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraPurplePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = NexoraPurpleHighlight,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = !isGenerating && inputText.isNotBlank(),
                        modifier = Modifier
                            .testTag("ricky_send_button")
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGenerating || inputText.isBlank()) MaterialTheme.colorScheme.surfaceVariant
                                else NexoraPurplePrimary
                            )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NexoraPurpleHighlight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isBlank()) NexoraTextMuted else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI wellness coach • Tap flag on any reply to report inaccurate/unsafe content",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }

    if (messageToReport != null) {
        var selectedReason by remember { mutableStateOf("Inaccurate health or factual info") }
        val reasons = listOf(
            "Inaccurate health or factual info",
            "Harmful or unsafe suggestion",
            "Offensive or inappropriate content",
            "Spam or unhelpful response"
        )
        AlertDialog(
            onDismissRequest = { messageToReport = null },
            title = { Text("Report AI Response", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Help keep RICKY safe and accurate. Select a reason:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    reasons.forEach { reason ->
                        val active = selectedReason == reason
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (active) NexoraPurplePrimary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (active) NexoraPurpleHighlight else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedReason = reason }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = reason,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToReport = null
                        Toast.makeText(
                            context,
                            "Thank you. AI response flagged for safety review ($selectedReason).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Submit Report", color = NexoraErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToReport = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    role: String,
    text: String,
    isStreaming: Boolean = false,
    onReportAiResponse: (() -> Unit)? = null
) {
    val isUser = role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) Brush.linearGradient(listOf(NexoraPurplePrimary, NexoraNeonPurpleGlow))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))
                )
                .border(
                    1.dp,
                    if (isUser) NexoraPurpleHighlight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NexoraPurpleHighlight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RICKY",
                                color = NexoraPurpleHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isStreaming && onReportAiResponse != null) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report AI Response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onReportAiResponse)
                            )
                        }
                    }
                }

                Text(
                    text = text,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (isStreaming) {
                    Text(
                        text = "●",
                        color = NexoraPurpleHighlight,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
