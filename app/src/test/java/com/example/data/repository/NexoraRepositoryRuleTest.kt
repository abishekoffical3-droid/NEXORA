package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NexoraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NexoraRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Test Morning Focus",
                category = "Productivity",
                priority = "HIGH"
            )
        }
        assertTrue("Task creation should succeed", createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue("Task ID must not be empty", taskId.isNotEmpty())
    }

    @Test
    fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NexoraRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask("Observe Stream Task").getOrThrow()
        }

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emittedTasks.any { it.id == taskId })
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NexoraRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected root FirebaseFirestoreException", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun createHabit_validPayload_createsHabit() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NexoraRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createHabit(
                title = "Daily Meditation",
                frequency = "DAILY"
            )
        }
        assertTrue("Habit creation should succeed", result.isSuccess)
        assertTrue(result.getOrThrow().isNotEmpty())
    }

    private companion object {
        const val ALICE_EMAIL = "alice_nexora@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
