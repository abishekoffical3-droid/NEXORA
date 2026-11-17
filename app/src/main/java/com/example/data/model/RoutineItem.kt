package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RoutineItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Daily",
    val timeOfDay: String = "Morning",
    val items: List<String> = emptyList(),
    val completedItems: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
