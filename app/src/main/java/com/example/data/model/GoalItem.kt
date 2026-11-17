package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GoalItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val targetDate: String? = null,
    val category: String = "Growth",
    val progress: Int = 0,
    val completed: Boolean = false,
    val milestones: List<String> = emptyList(),
    val completedMilestones: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
