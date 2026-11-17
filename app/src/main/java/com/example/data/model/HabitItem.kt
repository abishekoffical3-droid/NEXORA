package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class HabitItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val frequency: String = "DAILY",
    val targetDaysPerWeek: Int = 7,
    val completedDates: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
