package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TaskItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val category: String = "Personal",
    val priority: String = "MEDIUM",
    val dueDate: String? = null,
    val dueTime: String? = null,
    val completed: Boolean = false,
    val completedAt: String? = null,
    val xpAwarded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
