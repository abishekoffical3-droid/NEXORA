package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.HabitItem
import java.util.UUID

/**
 * Room database entity representing a trackable habit.
 * Stores local habit configuration, completion history across dates, and calculated streaks.
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isArchived"])
    ]
)
data class HabitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val notes: String? = null,
    val category: String = "Growth",
    val frequency: String = "DAILY", // "DAILY", "WEEKLY"
    val targetDaysPerWeek: Int = 7,
    val completedDates: List<String> = emptyList(), // e.g. ["2026-11-04", "2026-11-05"]
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isArchived: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this habit is marked completed for a given ISO-8601 date (YYYY-MM-DD).
     */
    fun isCompletedOn(dateStr: String): Boolean {
        return completedDates.contains(dateStr)
    }

    /**
     * Converts this local Room entity into the shared [HabitItem] model used in Cloud Firestore.
     */
    fun toHabitItem(): HabitItem {
        return HabitItem(
            id = id,
            userId = userId,
            title = title,
            notes = notes,
            frequency = frequency,
            targetDaysPerWeek = targetDaysPerWeek,
            completedDates = completedDates,
            currentStreak = currentStreak,
            bestStreak = bestStreak
        )
    }

    companion object {
        /**
         * Converts a Cloud [HabitItem] into a local [HabitEntity].
         */
        fun fromHabitItem(item: HabitItem, category: String = "Growth"): HabitEntity {
            return HabitEntity(
                id = item.id.ifBlank { UUID.randomUUID().toString() },
                userId = item.userId,
                title = item.title,
                notes = item.notes,
                category = category,
                frequency = item.frequency,
                targetDaysPerWeek = item.targetDaysPerWeek,
                completedDates = item.completedDates,
                currentStreak = item.currentStreak,
                bestStreak = item.bestStreak,
                isArchived = false,
                createdAtTimestamp = item.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAtTimestamp = item.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
    }
}
