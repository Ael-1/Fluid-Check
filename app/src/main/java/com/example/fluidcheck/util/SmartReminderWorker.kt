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

        // logic for smart notifications
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
