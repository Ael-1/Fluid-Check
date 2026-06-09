package com.example.fluidcheck.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * WorkManager worker that fires contextual smart notifications based on user progress.
 */
class SmartReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val firestoreRepository = FirestoreRepository(applicationContext)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = inputData.getString("user_id") ?: auth.currentUser?.uid ?: "GUEST"
        
        val notificationsEnabled = prefsRepository.getNotificationsEnabled(userId).first()
        if (notificationsEnabled == false) return Result.success()

        val userRecord = firestoreRepository.getUserRecord(userId) ?: return Result.success()
        val dailyGoal = userRecord.dailyGoal ?: 3000
        
        val todayLogs = firestoreRepository.getTodayFluidLogs(userId)
        val totalIntake = todayLogs.sumOf { it.amount }
        
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. Check AI-generated predictive reminders
        val predictiveRemindersJson = prefsRepository.getPredictiveReminders(userId).first()
        if (predictiveRemindersJson.isNotEmpty() && predictiveRemindersJson != "[]") {
            try {
                val jsonArray = org.json.JSONArray(predictiveRemindersJson)
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2, ..., Saturday = 7
                
                val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                val isWeekday = !isWeekend
                
                val currentDayName = when (dayOfWeek) {
                    Calendar.MONDAY -> "monday"
                    Calendar.TUESDAY -> "tuesday"
                    Calendar.WEDNESDAY -> "wednesday"
                    Calendar.THURSDAY -> "thursday"
                    Calendar.FRIDAY -> "friday"
                    Calendar.SATURDAY -> "saturday"
                    Calendar.SUNDAY -> "sunday"
                    else -> ""
                }
                
                // Rule verification
                for (i in 0 until jsonArray.length()) {
                    val ruleObj = jsonArray.getJSONObject(i)
                    val message = ruleObj.optString("message")
                    val triggerCondition = ruleObj.optString("triggerCondition").lowercase(Locale.US)
                    val timeToTrigger = ruleObj.optString("timeToTrigger").lowercase(Locale.US)
                    
                    // Verify day-of-week conditions if mentioned in triggerCondition or message
                    var dayMatch = true
                    if (triggerCondition.contains("weekend") || message.lowercase(Locale.US).contains("weekend")) {
                        dayMatch = isWeekend
                    } else if (triggerCondition.contains("weekday") || message.lowercase(Locale.US).contains("weekday")) {
                        dayMatch = isWeekday
                    } else {
                        // Check for specific days of week
                        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
                        for (d in days) {
                            if (triggerCondition.contains(d) || message.lowercase(Locale.US).contains(d)) {
                                dayMatch = (currentDayName == d)
                                break
                            }
                        }
                    }
                    if (!dayMatch) continue
                    
                    // Verify time conditions
                    var timeMatch = false
                    if (timeToTrigger.contains(":")) {
                        val parts = timeToTrigger.split(":")
                        if (parts.size >= 2) {
                            val ruleHour = parts[0].trim().toIntOrNull()
                            if (ruleHour != null) {
                                // Match if the current hour is equal to the rule trigger hour
                                timeMatch = (currentHour == ruleHour)
                            }
                        }
                    } else {
                        // Word match (e.g. "morning", "afternoon", "evening", "night")
                        if (timeToTrigger.contains("morning") && currentHour in 6..11) {
                            timeMatch = true
                        } else if (timeToTrigger.contains("afternoon") && currentHour in 12..17) {
                            timeMatch = true
                        } else if (timeToTrigger.contains("evening") && currentHour in 18..21) {
                            timeMatch = true
                        } else if (timeToTrigger.contains("night") && (currentHour >= 22 || currentHour < 6)) {
                            timeMatch = true
                        }
                    }
                    if (!timeMatch) continue
                    
                    // Only trigger if today's intake is less than 50% of the daily goal
                    if (totalIntake < 0.5 * dailyGoal) {
                        NotificationHelper.showSmartReminder(
                            applicationContext,
                            "AI Predictive Reminder 💧",
                            message
                        )
                        return Result.success()
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors and fall back to default logic
            }
        }

        // 2. Default fallback smart notifications
        when {
            // Evening check: ring not closed
            hour >= 18 && totalIntake < dailyGoal -> {
                val remaining = dailyGoal - totalIntake
                NotificationHelper.showSmartReminder(
                    applicationContext,
                    "Almost there! 🔥",
                    "You're only ${remaining}ml away from closing your ring today! You can do it."
                )
            }
            
            // Morning check: no logs yet
            hour in 9..11 && totalIntake == 0 -> {
                NotificationHelper.showSmartReminder(
                    applicationContext,
                    "Start your day right 💧",
                    "You haven't logged any water today. A fresh glass is waiting!"
                )
            }
            
            // Streak alert: last day was today, but maybe check if streak is high
            userRecord.streak >= 7 && userRecord.lastRingClosedDate != todayDate && hour >= 20 -> {
                NotificationHelper.showSmartReminder(
                    applicationContext,
                    "Protect your streak! 🔥",
                    "You've been consistent for ${userRecord.streak} days! Don't let it break tonight."
                )
            }
        }
        
        return Result.success()
    }
}
