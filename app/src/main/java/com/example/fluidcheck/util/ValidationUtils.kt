package com.example.fluidcheck.util

import android.content.Context

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
     * Values are validated in the user's display unit.
     */
    fun validateWeight(context: Context, weight: Float?, unit: WeightUnit = WeightUnit.METRIC): String? {
        if (weight == null) return "Invalid weight."
        val range = MeasurementUtils.weightRange(unit)
        val unitLabel = MeasurementUtils.weightUnit(context, unit)
        if (weight < range.first || weight > range.second) {
            return "Weight must be between ${range.first.toInt()} and ${range.second.toInt()} $unitLabel."
        }
        return null
    }

    fun validateHeight(context: Context, height: Float?, unit: HeightUnit = HeightUnit.METRIC): String? {
        if (height == null) return "Invalid height."
        val range = MeasurementUtils.heightRange(unit)
        val unitLabel = MeasurementUtils.heightUnit(context, unit)
        if (height < range.first || height > range.second) {
            return if (unit == HeightUnit.IMPERIAL) {
                "Height must be between ${range.first.toInt()} and ${range.second.toInt()} inches."
            } else {
                "Height must be between ${range.first.toInt()} and ${range.second.toInt()} $unitLabel."
            }
        }
        return null
    }

    fun validateAge(age: Int?): String? {
        if (age == null) return "Invalid age."
        if (age < 1 || age > 150) return "Age must be between 1 and 150."
        return null
    }

    fun validateDailyGoal(context: Context, goal: Int?, unit: VolumeUnit = VolumeUnit.METRIC): String? {
        if (goal == null) return "Invalid goal."
        val range = MeasurementUtils.goalRange(unit)
        val unitLabel = MeasurementUtils.volumeUnit(context, unit)
        if (goal < range.first || goal > range.second) {
            return "Daily goal must be between ${range.first} and ${range.second} $unitLabel."
        }
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

