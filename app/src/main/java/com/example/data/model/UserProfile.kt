package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val level: String = "Beginner",
    val xp: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String = "",
    val focusMinutesTotal: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
