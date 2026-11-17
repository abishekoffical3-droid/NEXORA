package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "RickyAiService"

class RickyAiService {

    private val baseSystemPrompt = """
        You are RICKY, the ultra-fast personal AI coach, life strategist, and empathetic companion inside NEXORA.
        Respond directly and energetically in clear English (or match the user's language/Nepali-English style if they write in Nepali/Roman Nepali).

        SPEED & RESPONSE STYLE:
        - Start immediately with the core answer—zero fluff or long intros.
        - Keep standard answers crisp (90-170 words) using bold highlights, bullet points, and 1 immediate micro-action step the user can do in 2 minutes.

        YOU ARE DEEPLY TRAINED IN 5 CORE PILLARS:
        1. DISCIPLINE & HABITS:
           - Identity-based discipline over fleeting motivation, the 5-Second Rule (5-4-3-2-1 Go), activation energy reduction, Cue -> Craving -> Response -> Reward, 2-Minute Rule, Habit Stacking, and "Never Miss Twice".
        2. MOTIVATIONAL QUOTES & MINDSET:
           - Deliver unforgettable, authentic motivational quotes (from Marcus Aurelius, David Goggins, James Clear, Seneca, Steve Jobs, etc.) matched to the user's exact moment, plus a practical action trigger.
        3. HEALTH & BIO-OPTIMIZATION:
           - Sleep cycles & circadian morning sunlight, hydration pacing, balanced protein & micronutrients, post-meal walks, posture breaks, and box / 4-7-8 breathing.
        4. STUDY & ACCELERATED LEARNING:
           - Feynman Technique, Active Recall, Spaced Repetition (1-3-7-21 days), 25/5 or 50/10 Pomodoro deep work sprints, exam/coding roadmaps, and beating study distraction.
        5. FEELINGS & EMOTIONAL SUPPORT:
           - Warm, non-judgmental validation when the user feels stressed, anxious, heartbroken, lonely, overwhelmed, or burnt out. Offer emotional grounding, gentle perspective shifts, and self-compassion without preaching.

        Safety Rules:
        - Never diagnose medical/psychiatric illnesses or recommend unsafe extremes. Never shame missed goals.
    """.trimIndent()

    // Ultra-fast model for instant conversational coaching & TTFT
    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.65f
                maxOutputTokens = 480
            },
            systemInstruction = content { text(baseSystemPrompt) }
        )
    }

    // High-accuracy fallback & multimodal vision model
    private val smartModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 650
            },
            systemInstruction = content { text(baseSystemPrompt) }
        )
    }

    suspend fun generateResponse(
        userPrompt: String,
        profile: UserProfile? = null,
        tasks: List<TaskItem> = emptyList(),
        habits: List<HabitItem> = emptyList(),
        goals: List<GoalItem> = emptyList(),
        allowPersonalization: Boolean = true
    ): String {
        val contextPrompt = buildUserContextOnly(
            userPrompt, profile, tasks, habits, goals, allowPersonalization
        )
        return try {
            withTimeout(25_000L) {
                val response = fastModel.generateContent(contextPrompt)
                val text = response.text
                if (!text.isNullOrBlank()) return@withTimeout text
                throw IllegalStateException("Empty fastModel response")
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.w(TAG, "fastModel timed out, falling back to smartModel")
            } else {
                throw e
            }
            withTimeout(40_000L) {
                smartModel.generateContent(contextPrompt).text
                    ?: "I'm ready to help—could you rephrase that slightly?"
            }
        } catch (e: Exception) {
            Log.w(TAG, "fastModel failed, falling back to smartModel", e)
            withTimeout(40_000L) {
                smartModel.generateContent(contextPrompt).text
                    ?: "I reviewed your request, please try once more."
            }
        }
    }

    fun streamResponse(
        userPrompt: String,
        profile: UserProfile? = null,
        tasks: List<TaskItem> = emptyList(),
        habits: List<HabitItem> = emptyList(),
        goals: List<GoalItem> = emptyList(),
        allowPersonalization: Boolean = true
    ): Flow<String> = flow {
        val contextPrompt = buildUserContextOnly(
            userPrompt, profile, tasks, habits, goals, allowPersonalization
        )
        var emittedAny = false
        try {
            withTimeout(25_000L) {
                fastModel.generateContentStream(contextPrompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emittedAny = true
                        emit(text)
                    }
                }
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Log.w(TAG, "fastModel stream timed out, retrying with smartModel")
        } catch (e: Exception) {
            if (emittedAny) throw e
            Log.w(TAG, "fastModel stream error, falling back to smartModel", e)
        }

        if (!emittedAny) {
            withTimeout(45_000L) {
                smartModel.generateContentStream(contextPrompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
        }
    }

    /**
     * Rapid voice/query helper that answers concisely for Text-to-Speech playback.
     */
    suspend fun askRickyQuickAdvice(query: String): String {
        val prompt = """
            The user asked via voice: "$query"
            Reply in 2 short, motivating, practical sentences ideal for spoken audio.
        """.trimIndent()
        return try {
            val res = withTimeout(12_000L) {
                fastModel.generateContent(prompt)
            }
            res.text?.trim().takeIf { !it.isNullOrBlank() }
                ?: "Stay locked in! Take one small breath, start your top priority for just 2 minutes, and build unstoppable momentum."
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                "Stay focused! Tackle one concrete task right now for two minutes and keep your streak alive."
            } else {
                throw e
            }
        } catch (e: Exception) {
            try {
                smartModel.generateContent(prompt).text?.trim()
                    ?: "Stay focused! Start small right now and let discipline carry you forward."
            } catch (_: Exception) {
                "Stay focused! Start small right now and let discipline carry you forward."
            }
        }
    }

    private val foodVisionEngine by lazy { FoodVisionNutritionEngine() }

    /**
     * Multimodal Food & Nutrition Analysis using dedicated FoodVisionNutritionEngine.
     * Extracts per-item grams, cooked/raw states, confidence, fiber/sugar/sat fat, and Atwater-verified totals.
     */
    suspend fun analyzeFoodNutrition(bitmap: Bitmap): FoodNutritionAnalysis {
        return foodVisionEngine.analyzeFoodImage(bitmap)
    }

    private fun buildUserContextOnly(
        prompt: String,
        profile: UserProfile?,
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        goals: List<GoalItem>,
        allowPersonalization: Boolean
    ): String {
        val sb = StringBuilder()

        if (allowPersonalization && profile != null) {
            sb.append("User Context:\n")
            sb.append("- Name: ${profile.displayName}, Level ${profile.level}, ${profile.xp} XP, ${profile.streak}d streak\n")

            val pendingTasks = tasks.filter { !it.completed }
            if (pendingTasks.isNotEmpty()) {
                sb.append("- Pending Tasks: ")
                sb.append(pendingTasks.take(4).joinToString(", ") { "${it.title} [${it.priority}]" })
                sb.append("\n")
            }

            if (habits.isNotEmpty()) {
                sb.append("- Habits: ")
                sb.append(habits.take(4).joinToString(", ") { "${it.title} (${it.currentStreak}d)" })
                sb.append("\n")
            }

            val activeGoals = goals.filter { !it.completed }
            if (activeGoals.isNotEmpty()) {
                sb.append("- Goals: ")
                sb.append(activeGoals.take(2).joinToString(", ") { "${it.title} (${it.progress}%)" })
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append("User Message:\n").append(prompt)
        return sb.toString()
    }
}
