package com.example.fluidcheck.util

object ValidationUtils {
    /**
     * Validates a username based on Task 1.10:
     * - 4–20 characters
     * - Alphanumeric, underscores (_), or periods (.)
     * - No spaces/emojis
     */
    fun validateUsername(username: String): String? {
        if (username.isBlank()) return "Username cannot be empty."
        if (username.length < 4) return "Username must be at least 4 characters."
        if (username.length > 20) return "Username must be no more than 20 characters."
        
        val regex = "^[a-zA-Z0-9._]+$".toRegex()
        if (!regex.matches(username)) {
            return "Only alphanumeric, underscores, and periods allowed. No spaces or symbols."
        }
        return null
    }

    /**
     * Validates numeric ranges based on Task 12.2
     */
    fun validateWeight(weight: Float?): String? {
        if (weight == null) return "Invalid weight."
        if (weight < 1 || weight > 500) return "Weight must be between 1 and 500 kg."
        return null
    }

    fun validateHeight(height: Float?): String? {
        if (height == null) return "Invalid height."
        if (height < 30 || height > 300) return "Height must be between 30 and 300 cm."
        return null
    }

    fun validateAge(age: Int?): String? {
        if (age == null) return "Invalid age."
        if (age < 1 || age > 150) return "Age must be between 1 and 150."
        return null
    }

    fun validateDailyGoal(goal: Int?): String? {
        if (goal == null) return "Invalid goal."
        if (goal < 100 || goal > 20000) return "Daily goal must be between 100 and 20000 ml."
        return null
    }

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email cannot be empty."
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        if (!email.matches(emailPattern.toRegex())) {
            return "Please enter a valid email address."
        }
        return null
    }
}
