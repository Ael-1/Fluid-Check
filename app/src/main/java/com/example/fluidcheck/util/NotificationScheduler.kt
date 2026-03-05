package com.example.fluidcheck.util

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling and canceling periodic notifications via WorkManager.
 */
object NotificationScheduler {
    private const val REMINDER_WORK_NAME = "hydration_reminder_work"
    private const val SMART_REMINDER_WORK_NAME = "smart_reminder_work"

    fun scheduleReminders(context: Context, userId: String, frequencyMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val inputData = workDataOf("user_id" to userId)

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            frequencyMinutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(REMINDER_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    fun scheduleSmartReminders(context: Context, userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val inputData = workDataOf("user_id" to userId)

        // Smart reminders every 4 hours
        val smartReminderRequest = PeriodicWorkRequestBuilder<SmartReminderWorker>(
            4, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(SMART_REMINDER_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SMART_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            smartReminderRequest
        )
    }

    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(SMART_REMINDER_WORK_NAME)
    }
}
