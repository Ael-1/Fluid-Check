package com.example.fluidcheck.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling and canceling notifications.
 * Uses AlarmManager for reliable periodic hydration reminders (fires even in Doze mode).
 * Uses WorkManager only for smart reminders that need Firestore access.
 */
object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    private const val SMART_REMINDER_WORK_NAME = "smart_reminder_work"
    private const val ALARM_REQUEST_CODE = 2001

    /**
     * Schedules the next hydration reminder using AlarmManager.
     * Uses setExactAndAllowWhileIdle() for precise delivery even in Doze mode.
     * Falls back to setAndAllowWhileIdle() if exact alarm permission isn't granted.
     */
    fun scheduleReminders(context: Context, userId: String, frequencyMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_USER_ID, userId)
            putExtra(ReminderAlarmReceiver.EXTRA_FREQUENCY_MINUTES, frequencyMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + (frequencyMinutes * 60 * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm in $frequencyMinutes minutes")
            } else {
                // Exact alarm permission not granted on Android 12+, use inexact (still fires in Doze)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled inexact alarm in $frequencyMinutes minutes (no exact alarm permission)")
            }
        } else {
            // Pre-Android 12: no permission needed for exact alarms
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled exact alarm in $frequencyMinutes minutes (pre-S)")
        }
    }

    /**
     * Schedules smart reminders via WorkManager (needs Firestore access for context-aware messages).
     * Uses KEEP policy so reopening the app doesn't restart the 4-hour timer.
     */
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
        Log.d(TAG, "Smart reminders scheduled (KEEP policy)")
    }

    fun cancelAllReminders(context: Context) {
        // Cancel AlarmManager hydration reminder
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Cancel WorkManager smart reminder
        WorkManager.getInstance(context).cancelUniqueWork(SMART_REMINDER_WORK_NAME)
        Log.d(TAG, "All reminders cancelled")
    }
}
