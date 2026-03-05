package com.example.fluidcheck.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.FirestoreRepository
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that fires periodic hydration reminders.
 */
class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepository = UserPreferencesRepository(applicationContext)
        
        // Get userId from input data, or fallback to Firebase, or "GUEST"
        val userId = inputData.getString("user_id") 
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: "GUEST"
        
        val notificationsEnabled = prefsRepository.getNotificationsEnabled(userId).first()
        
        // If null (not set), we'll assume it's enabled if scheduled (scheduler has guards).
        // If explicitly false, we skip.
        if (notificationsEnabled != false) {
            NotificationHelper.showHydrationReminder(applicationContext)
        }
        
        return Result.success()
    }
}
