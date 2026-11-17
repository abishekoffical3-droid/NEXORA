package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NexoraDatabase
import com.example.data.local.entity.HabitEntity
import com.example.data.repository.LocalHabitRepository
import com.example.ui.viewmodel.HabitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitRoomAndViewModelTest {

    private lateinit var db: NexoraDatabase
    private lateinit var repository: LocalHabitRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NexoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalHabitRepository(db.habitDao(), testDispatcher)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testInsertAndRetrieveHabitEntity() = runTest(testDispatcher) {
        val habit = HabitEntity(
            id = "test_habit_1",
            userId = "user_123",
            title = "Morning Meditation",
            category = "Mind",
            frequency = "DAILY",
            completedDates = listOf("2026-11-05")
        )

        db.habitDao().insertHabit(habit)
        val retrieved = db.habitDao().getHabitById("test_habit_1")

        assertNotNull(retrieved)
        assertEquals("Morning Meditation", retrieved?.title)
        assertEquals("Mind", retrieved?.category)
        assertTrue(retrieved!!.isCompletedOn("2026-11-05"))
        assertFalse(retrieved.isCompletedOn("2026-11-06"))
    }

    @Test
    fun testRepositoryToggleHabitCompletion() = runTest(testDispatcher) {
        val habit = repository.createHabit(
            title = "Read 20 Pages",
            category = "Growth",
            userId = "user_123"
        )

        val today = LocalDate.now().toString()

        // Toggle ON
        val toggledOn = repository.toggleHabitCompletion(habit.id, today)
        assertNotNull(toggledOn)
        assertTrue(toggledOn!!.isCompletedOn(today))
        assertEquals(1, toggledOn.currentStreak)

        // Toggle OFF
        val toggledOff = repository.toggleHabitCompletion(habit.id, today)
        assertNotNull(toggledOff)
        assertFalse(toggledOff!!.isCompletedOn(today))
    }

    @Test
    fun testViewModelManagesHabitCompletionStates() = runTest(testDispatcher) {
        val viewModel = HabitViewModel(repository, currentUserId = "user_123")
        val today = LocalDate.now().toString()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Create 2 habits
        val h1 = repository.createHabit(title = "Hydrate 2L", category = "Health", userId = "user_123")
        val h2 = repository.createHabit(title = "Code 1 Hour", category = "Career", userId = "user_123")
        advanceUntilIdle()

        val initialHabits = repository.allActiveHabits.first()
        assertEquals(2, initialHabits.size)

        // Toggle completion on the first habit via ViewModel
        viewModel.toggleHabitCompletion(h1.id, today).join()

        val updatedHabit = repository.getHabitById(h1.id)
        assertNotNull(updatedHabit)
        assertTrue(updatedHabit!!.isCompletedOn(today))

        // Check ViewModel's computed state via flow emission
        val state = viewModel.uiState.first { it.completedHabitIds.contains(h1.id) }
        assertEquals(today, state.selectedDate)
        assertTrue(state.completedHabitIds.contains(h1.id))
        assertEquals(1, state.completedCount)
        assertEquals(2, state.totalCount)
        assertEquals(0.5f, state.completionRate)
    }
}
