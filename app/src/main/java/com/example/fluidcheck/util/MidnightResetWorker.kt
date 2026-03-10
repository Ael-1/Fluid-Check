package com.example.fluidcheck.util

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.repository.GuestRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit

class MidnightResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("MidnightResetWorker", "Starting midnight streak evaluation...")
        
        val firestoreRepository = FirestoreRepository(applicationContext)
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: "GUEST"

        return try {
            val result = if (userId == "GUEST") {
                val guestRepo = GuestRepository(applicationContext)
                guestRepo.evaluateStreak()
                Result.success()
            } else {
                val res = firestoreRepository.evaluateStreak(userId)
                if (res.isSuccess) Result.success() else Result.retry()
            }
            
            // Schedule next run
            schedule(applicationContext)
            
            result
        } catch (e: Exception) {
            Log.e("MidnightResetWorker", "Error evaluating streak", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "MidnightResetWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val currentTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
            val dueDate = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5) // Run slightly after midnight
                set(Calendar.SECOND, 0)
            }

            if (dueDate.before(currentTime)) {
                dueDate.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = dueDate.timeInMillis - currentTime.timeInMillis

            val workRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.d("MidnightResetWorker", "Scheduled next run in ${initialDelay / 1000 / 60} minutes")
        }
    }
}
