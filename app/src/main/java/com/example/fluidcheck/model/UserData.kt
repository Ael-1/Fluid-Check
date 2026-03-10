package com.example.fluidcheck.model

import com.google.firebase.Timestamp

/**
 * Basic user information used for authentication and administration.
 */
data class UserCredentials(
    val name: String,
    val email: String,
    val role: String,
    val streak: Int = 0
)

/**
 * Quick Add Configuration for the home screen.
 */
data class QuickAddConfig(
    val amount: Int = 0,
    val type: String = "Water",
    val id: String = java.util.UUID.randomUUID().toString()
)

val DEFAULT_QUICK_ADD_CONFIGS = listOf(
    QuickAddConfig(250, "Water"),
    QuickAddConfig(500, "Water"),
    QuickAddConfig(750, "Water")
)

/**
 * Detailed personal physical information used for calculating hydration needs and tracking progress.
 */
data class UserRecord(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val weight: String = "",
    val height: String = "",
    val age: String = "",
    val sex: String = "",
    val activity: String = "",
    val environment: String = "",
    val setupCompleted: Boolean = false,
    val role: String = "USER",
    val dailyGoal: Int? = 3000,
    
    // New fields from section 7 of TODO.md
    val deleted: Boolean = false,
    val fcmToken: String = "",
    val quickAddConfig: List<QuickAddConfig>? = null, // Changed to nullable to distinguish between missing and empty
    val notificationsEnabled: Boolean? = null,
    val reminderFrequency: String = "60", // minutes
    val lastRingClosedDate: String = "",
    val streak: Int = 0,
    val highestStreak: Int = 0,
    val totalFluidDrankAllTime: Int = 0,
    val totalRingsClosed: Int = 0,
    val createdAt: Timestamp? = null,
    val emailVerified: Boolean = false
) {
    fun isEmpty(): Boolean {
        return weight.isEmpty() && height.isEmpty() && age.isEmpty() && 
               sex.isEmpty() && activity.isEmpty() && environment.isEmpty()
    }
}
