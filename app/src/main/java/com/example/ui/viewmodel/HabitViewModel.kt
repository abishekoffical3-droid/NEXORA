package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.HabitEntity
import com.example.data.model.HabitItem
import com.example.data.repository.LocalHabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * UI State for Habit Tracking and completion states.
 */
data class HabitUiState(
    val habits: List<HabitEntity> = emptyList(),
    val filteredHabits: List<HabitEntity> = emptyList(),
    val selectedDate: String = LocalDate.now().toString(),
    val selectedCategory: String? = null,
    val completedHabitIds: Set<String> = emptySet(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionRate: Float = 0f,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

/**
 * ViewModel managing habit tracking, Room persistence, and completion states across dates.
 */
class HabitViewModel(
    private val localHabitRepository: LocalHabitRepository,
    private val currentUserId: String = ""
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val habitsFlow = if (currentUserId.isNotBlank()) {
        localHabitRepository.getActiveHabitsForUser(currentUserId)
    } else {
        localHabitRepository.allActiveHabits
    }

    /**
     * Combined reactive UI state containing all habits, filtered subset, completion counts, and rate.
     */
    val uiState: StateFlow<HabitUiState> = combine(
        habitsFlow,
        _selectedDate,
        _selectedCategory,
        _userMessage,
        _isLoading
    ) { habits, date, category, message, loading ->
        val filtered = if (category.isNullOrBlank()) {
            habits
        } else {
            habits.filter { it.category.equals(category, ignoreCase = true) }
        }

        val completedIds = habits.filter { it.isCompletedOn(date) }.map { it.id }.toSet()
        val total = filtered.size
        val completed = filtered.count { it.isCompletedOn(date) }
        val rate = if (total > 0) completed.toFloat() / total else 0f

        HabitUiState(
            habits = habits,
            filteredHabits = filtered,
            selectedDate = date,
            selectedCategory = category,
            completedHabitIds = completedIds,
            completedCount = completed,
            totalCount = total,
            completionRate = rate,
            isLoading = loading,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState()
    )

    /**
     * Changes the active date for viewing and managing habit completion states.
     * @param dateStr ISO-8601 formatted date string (YYYY-MM-DD).
     */
    fun selectDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    /**
     * Toggles habit completion state for a given date (defaults to currently selected date).
     * Updates Room SQLite entity and recalculates consecutive streak.
     */
    fun toggleHabitCompletion(habitId: String, dateStr: String = _selectedDate.value) = viewModelScope.launch {
        try {
            val updated = localHabitRepository.toggleHabitCompletion(habitId, dateStr)
            if (updated != null) {
                val isDone = updated.isCompletedOn(dateStr)
                _userMessage.value = if (isDone) {
                    "Completed \"${updated.title}\"! Streak: ${updated.currentStreak}d 🔥"
                } else {
                    "Unmarked \"${updated.title}\""
                }
            }
        } catch (e: Exception) {
            _userMessage.value = "Failed to update habit: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Explicitly completes a habit for the given date, incrementing the current streak and updating persistence.
     */
    fun completeHabitAndIncrementStreak(habitId: String, dateStr: String = _selectedDate.value) = viewModelScope.launch {
        try {
            val habit = localHabitRepository.getHabitById(habitId) ?: return@launch
            if (!habit.isCompletedOn(dateStr)) {
                val updated = localHabitRepository.toggleHabitCompletion(habitId, dateStr)
                if (updated != null) {
                    _userMessage.value = "Habit \"${updated.title}\" completed! Streak incremented to ${updated.currentStreak} 🔥"
                }
            }
        } catch (e: Exception) {
            _userMessage.value = "Failed to complete habit: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Creates and persists a new habit in Room database.
     */
    fun createHabit(
        title: String,
        notes: String? = null,
        category: String = "Growth",
        frequency: String = "DAILY",
        targetDaysPerWeek: Int = 7
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val created = localHabitRepository.createHabit(
                    title = title,
                    notes = notes,
                    category = category,
                    frequency = frequency,
                    targetDaysPerWeek = targetDaysPerWeek,
                    userId = currentUserId
                )
                _userMessage.value = "Habit \"${created.title}\" created!"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create habit: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a habit by ID from Room database.
     */
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            try {
                localHabitRepository.deleteHabit(habitId)
                _userMessage.value = "Habit deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete habit: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Archives a habit instead of deleting it.
     */
    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            try {
                localHabitRepository.archiveHabit(habitId)
                _userMessage.value = "Habit archived"
            } catch (e: Exception) {
                _userMessage.value = "Failed to archive habit"
            }
        }
    }

    /**
     * Filter habits by category (e.g. Growth, Health, Mind, Fitness, etc.).
     */
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    /**
     * Clears user feedback toast or banner message.
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Syncs cloud habits from Firestore into local Room cache.
     */
    fun syncWithCloud(cloudHabits: List<HabitItem>) {
        viewModelScope.launch {
            try {
                localHabitRepository.syncFromCloud(cloudHabits)
            } catch (_: Exception) {}
        }
    }
}

/**
 * Factory class for instantiating [HabitViewModel] with dependencies.
 */
class HabitViewModelFactory(
    private val localHabitRepository: LocalHabitRepository,
    private val currentUserId: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(localHabitRepository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
