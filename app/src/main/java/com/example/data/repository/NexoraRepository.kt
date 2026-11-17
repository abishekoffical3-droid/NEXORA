package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FocusSession
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.RickyMessage
import com.example.data.model.RoutineItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID

class NexoraRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // -------------------------------------------------------------
    // USER PROFILE
    // -------------------------------------------------------------

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getOrCreateUserProfile(displayName: String, email: String): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java)
            if (existing != null) return existing
        }

        val initialProfile = UserProfile(
            userId = uid,
            displayName = displayName.ifBlank { "Achiever" },
            email = email,
            level = "Beginner",
            xp = 0,
            streak = 1,
            bestStreak = 1,
            lastActiveDate = LocalDate.now().toString(),
            focusMinutesTotal = 0
        )

        val payload = mapOf(
            "userId" to uid,
            "displayName" to initialProfile.displayName,
            "email" to initialProfile.email,
            "level" to initialProfile.level,
            "xp" to initialProfile.xp,
            "streak" to initialProfile.streak,
            "bestStreak" to initialProfile.bestStreak,
            "lastActiveDate" to initialProfile.lastActiveDate,
            "focusMinutesTotal" to initialProfile.focusMinutesTotal,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        return initialProfile
    }

    suspend fun addXpAndCheckStreak(addedXp: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val todayStr = LocalDate.now().toString()

        try {
            val snapshot = docRef.get().await()
            val current = snapshot.toObject(UserProfile::class.java)
            val currentXp = (current?.xp ?: 0) + addedXp
            val newLevel = calculateLevel(currentXp)

            val updates = mutableMapOf<String, Any>(
                "xp" to currentXp,
                "level" to newLevel,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (current?.lastActiveDate != todayStr) {
                val newStreak = (current?.streak ?: 0) + 1
                val newBest = maxOf(newStreak, current?.bestStreak ?: 1)
                updates["streak"] = newStreak
                updates["bestStreak"] = newBest
                updates["lastActiveDate"] = todayStr
            }

            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun addFocusMinutes(minutes: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "focusMinutesTotal" to FieldValue.increment(minutes.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    private fun calculateLevel(xp: Int): String {
        return when {
            xp >= 2000 -> "Legend"
            xp >= 1000 -> "Elite"
            xp >= 600 -> "Disciplined"
            xp >= 300 -> "Consistent"
            xp >= 100 -> "Explorer"
            else -> "Beginner"
        }
    }

    // -------------------------------------------------------------
    // TASKS
    // -------------------------------------------------------------

    fun observeTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TaskItem::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createTask(
        title: String,
        notes: String? = null,
        category: String = "Personal",
        priority: String = "MEDIUM",
        dueDate: String? = null,
        dueTime: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val taskId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)

        val payload = mutableMapOf<String, Any>(
            "id" to taskId,
            "userId" to uid,
            "title" to title.trim(),
            "category" to category,
            "priority" to priority,
            "completed" to false,
            "xpAwarded" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!notes.isNullOrBlank()) payload["notes"] = notes.trim()
        if (!dueDate.isNullOrBlank()) payload["dueDate"] = dueDate.trim()
        if (!dueTime.isNullOrBlank()) payload["dueTime"] = dueTime.trim()

        return try {
            docRef.set(payload).await()
            Result.success(taskId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        taskId: String,
        title: String,
        notes: String? = null,
        category: String = "Personal",
        priority: String = "MEDIUM",
        dueDate: String? = null,
        dueTime: String? = null
    ): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        val updates = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "category" to category,
            "priority" to priority,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (notes != null) updates["notes"] = notes.trim()
        if (dueDate != null) updates["dueDate"] = dueDate.trim()
        if (dueTime != null) updates["dueTime"] = dueTime.trim()

        return try {
            docRef.update(updates).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            false
        }
    }

    suspend fun toggleTaskCompletion(task: TaskItem): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        val willBeCompleted = !task.completed
        val awardXp = willBeCompleted && !task.xpAwarded

        val updates = mutableMapOf<String, Any>(
            "completed" to willBeCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (willBeCompleted) {
            updates["completedAt"] = LocalDate.now().toString()
            if (awardXp) {
                updates["xpAwarded"] = true
            }
        }

        try {
            docRef.update(updates).await()
            if (awardXp) {
                addXpAndCheckStreak(15)
            }
            return true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            return false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            false
        }
    }

    // -------------------------------------------------------------
    // HABITS
    // -------------------------------------------------------------

    fun observeHabits(): Flow<List<HabitItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/habits"
        emitAll(
            db.collection("users").document(uid).collection("habits")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(HabitItem::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createHabit(
        title: String,
        notes: String? = null,
        frequency: String = "DAILY",
        targetDaysPerWeek: Int = 7
    ): Result<String> {
        val uid = requireUserId()
        val habitId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)

        val payload = mutableMapOf<String, Any>(
            "id" to habitId,
            "userId" to uid,
            "title" to title.trim(),
            "frequency" to frequency,
            "targetDaysPerWeek" to targetDaysPerWeek,
            "completedDates" to emptyList<String>(),
            "currentStreak" to 0,
            "bestStreak" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!notes.isNullOrBlank()) payload["notes"] = notes.trim()

        return try {
            docRef.set(payload).await()
            Result.success(habitId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitDate(habit: HabitItem, dateStr: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habit.id)
        val isCompleted = habit.completedDates.contains(dateStr)
        val updatedDates = if (isCompleted) {
            habit.completedDates - dateStr
        } else {
            habit.completedDates + dateStr
        }

        val newStreak = updatedDates.size
        val newBest = maxOf(newStreak, habit.bestStreak)

        return try {
            docRef.update(
                mapOf(
                    "completedDates" to updatedDates,
                    "currentStreak" to newStreak,
                    "bestStreak" to newBest,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!isCompleted) {
                addXpAndCheckStreak(20)
            }
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            false
        }
    }

    suspend fun deleteHabit(habitId: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
        return try {
            docRef.delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            false
        }
    }

    // -------------------------------------------------------------
    // GOALS
    // -------------------------------------------------------------

    fun observeGoals(): Flow<List<GoalItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/goals"
        emitAll(
            db.collection("users").document(uid).collection("goals")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(GoalItem::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createGoal(
        title: String,
        description: String? = null,
        targetDate: String? = null,
        category: String = "Growth",
        milestones: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val goalId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)

        val payload = mutableMapOf<String, Any>(
            "id" to goalId,
            "userId" to uid,
            "title" to title.trim(),
            "category" to category,
            "progress" to 0,
            "completed" to false,
            "milestones" to milestones,
            "completedMilestones" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!description.isNullOrBlank()) payload["description"] = description.trim()
        if (!targetDate.isNullOrBlank()) payload["targetDate"] = targetDate.trim()

        return try {
            docRef.set(payload).await()
            Result.success(goalId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGoalProgress(goalId: String, newProgress: Int, completed: Boolean): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        return try {
            docRef.update(
                mapOf(
                    "progress" to newProgress.coerceIn(0, 100),
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (completed) {
                addXpAndCheckStreak(50)
            }
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            false
        }
    }

    suspend fun toggleGoalMilestone(goalId: String, milestone: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        return try {
            val snapshot = docRef.get().await()
            val goal = snapshot.toObject(GoalItem::class.java) ?: return false
            val currentCompleted = goal.completedMilestones.toMutableList()
            if (currentCompleted.contains(milestone)) {
                currentCompleted.remove(milestone)
            } else {
                currentCompleted.add(milestone)
            }
            val totalMilestones = if (goal.milestones.isNotEmpty()) goal.milestones.size else 1
            val newProgress = ((currentCompleted.size.toFloat() / totalMilestones) * 100).toInt().coerceIn(0, 100)
            val isComplete = newProgress == 100 || (goal.milestones.isNotEmpty() && currentCompleted.size == goal.milestones.size)

            val updates = mutableMapOf<String, Any>(
                "completedMilestones" to currentCompleted,
                "progress" to newProgress,
                "completed" to isComplete,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            if (isComplete && !goal.completed) {
                addXpAndCheckStreak(50)
            }
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            false
        }
    }

    suspend fun deleteGoal(goalId: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        return try {
            docRef.delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            false
        }
    }

    // -------------------------------------------------------------
    // ROUTINES
    // -------------------------------------------------------------

    fun observeRoutines(): Flow<List<RoutineItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/routines"
        emitAll(
            db.collection("users").document(uid).collection("routines")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RoutineItem::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createRoutine(
        title: String,
        category: String,
        timeOfDay: String,
        items: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val routineId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)

        val payload = mapOf(
            "id" to routineId,
            "userId" to uid,
            "title" to title.trim(),
            "category" to category,
            "timeOfDay" to timeOfDay,
            "items" to items,
            "completedItems" to emptyList<String>(),
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(routineId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleRoutineStep(routineId: String, step: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
        return try {
            val snapshot = docRef.get().await()
            val routine = snapshot.toObject(RoutineItem::class.java) ?: return false
            val currentCompleted = routine.completedItems.toMutableList()
            val isNowCompleted = !currentCompleted.contains(step)
            if (isNowCompleted) {
                currentCompleted.add(step)
            } else {
                currentCompleted.remove(step)
            }
            docRef.update(
                mapOf(
                    "completedItems" to currentCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (isNowCompleted) {
                addXpAndCheckStreak(10)
            }
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            false
        }
    }

    suspend fun deleteRoutine(routineId: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
        return try {
            docRef.delete().await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            false
        }
    }

    // -------------------------------------------------------------
    // FOCUS SESSIONS
    // -------------------------------------------------------------

    fun observeFocusSessions(): Flow<List<FocusSession>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/focusSessions"
        emitAll(
            db.collection("users").document(uid).collection("focusSessions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FocusSession::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun logFocusSession(
        durationMinutes: Int,
        sessionType: String,
        taskTitle: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val sessionId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("focusSessions").document(sessionId)

        val payload = mutableMapOf<String, Any>(
            "id" to sessionId,
            "userId" to uid,
            "durationMinutes" to durationMinutes,
            "sessionType" to sessionType,
            "completed" to true,
            "xpAwarded" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!taskTitle.isNullOrBlank()) payload["taskTitle"] = taskTitle.trim()

        return try {
            docRef.set(payload).await()
            addFocusMinutes(durationMinutes)
            addXpAndCheckStreak(25)
            Result.success(sessionId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // RICKY MESSAGES
    // -------------------------------------------------------------

    fun observeRickyMessages(): Flow<List<RickyMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/rickyMessages"
        emitAll(
            db.collection("users").document(uid).collection("rickyMessages")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RickyMessage::class.java)?.let {
                            if (it.id.isBlank()) it.copy(id = doc.id) else it
                        }
                    }.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveRickyMessage(role: String, content: String): Result<String> {
        val uid = requireUserId()
        val messageId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("rickyMessages").document(messageId)

        val payload = mapOf(
            "id" to messageId,
            "userId" to uid,
            "role" to role,
            "content" to content.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearRickyHistory(): Boolean {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("rickyMessages")
        return try {
            val snapshots = colRef.get().await()
            for (doc in snapshots.documents) {
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            false
        }
    }

    /**
     * Permanently deletes all cloud documents belonging to the currently signed-in user
     * across all subcollections and the root user profile document (Google Play Data Deletion compliance).
     */
    suspend fun deleteAllUserCloudData(): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No signed-in user"))

        return try {
            val userDoc = db.collection("users").document(uid)
            val subcollections = listOf(
                "tasks",
                "habits",
                "goals",
                "routines",
                "focusSessions",
                "rickyMessages"
            )
            for (sub in subcollections) {
                val snap = userDoc.collection(sub).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
