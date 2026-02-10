package com.example.fluidcheck.model

/**
 * Basic user information used for authentication and administration.
 */
data class UserCredentials(
    val name: String,
    val email: String,
    val role: String,
    val streak: String
)

/**
 * Detailed personal physical information used for calculating hydration needs.
 */
data class UserRecord(
    val weight: String = "",
    val height: String = "",
    val age: String = "",
    val sex: String = "",
    val activity: String = "",
    val environment: String = ""
) {
    fun isEmpty(): Boolean {
        return weight.isEmpty() && height.isEmpty() && age.isEmpty() && 
               sex.isEmpty() && activity.isEmpty() && environment.isEmpty()
    }
}
