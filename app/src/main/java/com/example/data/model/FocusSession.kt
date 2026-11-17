package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FocusSession(
    val id: String = "",
    val userId: String = "",
    val durationMinutes: Int = 25,
    val sessionType: String = "POMODORO",
    val completed: Boolean = true,
    val taskTitle: String? = null,
    val xpAwarded: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
