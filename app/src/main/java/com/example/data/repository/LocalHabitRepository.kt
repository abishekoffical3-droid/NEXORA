package com.example.data.repository

import com.example.data.local.dao.HabitDao
import com.example.data.local.entity.HabitEntity
import com.example.data.model.HabitItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

/**
 * Repository pattern implementation managing Habit data access between Room DAO and ViewModels.
 */
class LocalHabitRepository(
    private val habitDao: HabitDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allActiveHabits: Flow<List<HabitEntity>> = habitDao.getAllActiveHabits()

    fun getActiveHabitsForUser(userId: String): Flow<List<HabitEntity>> {
        return habitDao.getActiveHabitsForUser(userId)
    }

    suspend fun getHabitById(id: String): HabitEntity? = withContext(ioDispatcher) {
        habitDao.getHabitById(id)
    }

    fun getHabitByIdFlow(id: String): Flow<HabitEntity?> {
        return habitDao.getHabitByIdFlow(id)
    }

    suspend fun saveHabit(habit: HabitEntity) = withContext(ioDispatcher) {
        habitDao.insertHabit(habit)
    }

    suspend fun createHabit(
        title: String,
        notes: String? = null,
        category: String = "Growth",
        frequency: String = "DAILY",
        targetDaysPerWeek: Int = 7,
        userId: String = ""
    ): HabitEntity = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            notes = notes?.trim()?.ifBlank { null },
            category = category,
            frequency = frequency,
            targetDaysPerWeek = targetDaysPerWeek,
            completedDates = emptyList(),
            currentStreak = 0,
            bestStreak = 0,
            isArchived = false,
            createdAtTimestamp = now,
            updatedAtTimestamp = now
        )
        habitDao.insertHabit(habit)
        habit
    }

    /**
     * Toggles the completion state of a habit for a specific date (YYYY-MM-DD).
     * Automatically recalculates currentStreak and bestStreak.
     */
    suspend fun toggleHabitCompletion(habitId: String, dateStr: String): HabitEntity? = withContext(ioDispatcher) {
        val existing = habitDao.getHabitById(habitId) ?: return@withContext null
        val isAlreadyCompleted = existing.completedDates.contains(dateStr)

        val updatedDates = if (isAlreadyCompleted) {
            existing.completedDates - dateStr
        } else {
            (existing.completedDates + dateStr).distinct().sorted()
        }

        // Calculate consecutive streak up to latest completed date
        val newCurrentStreak = calculateConsecutiveStreak(updatedDates)
        val newBestStreak = maxOf(newCurrentStreak, existing.bestStreak, updatedDates.size.coerceAtMost(updatedDates.size))

        val updated = existing.copy(
            completedDates = updatedDates,
            currentStreak = newCurrentStreak,
            bestStreak = maxOf(newBestStreak, newCurrentStreak),
            updatedAtTimestamp = System.currentTimeMillis()
        )
        habitDao.updateHabit(updated)
        updated
    }

    suspend fun deleteHabit(habitId: String) = withContext(ioDispatcher) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun archiveHabit(habitId: String) = withContext(ioDispatcher) {
        habitDao.setHabitArchived(habitId, true, System.currentTimeMillis())
    }

    /**
     * Synchronizes a list of [HabitItem] records (e.g. from Firestore) into local Room storage.
     */
    suspend fun syncFromCloud(cloudHabits: List<HabitItem>) = withContext(ioDispatcher) {
        if (cloudHabits.isEmpty()) return@withContext
        val entities = cloudHabits.map { HabitEntity.fromHabitItem(it) }
        habitDao.insertHabits(entities)
    }

    private fun calculateConsecutiveStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.mapNotNull {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }.distinct().sortedDescending()

        if (sortedDates.isEmpty()) return 0

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val first = sortedDates.first()
        // If the latest check-in is neither today nor yesterday, streak is currently 0
        if (first != today && first != yesterday) {
            return 0
        }

        var streak = 1
        var previousDate = first
        for (i in 1 until sortedDates.size) {
            val expected = previousDate.minusDays(1)
            if (sortedDates[i] == expected) {
                streak++
                previousDate = expected
            } else {
                break
            }
        }
        return streak
    }
}
