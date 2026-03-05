package com.example.fluidcheck.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fluidcheck.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that fires when an AlarmManager alarm triggers.
 * Shows a hydration reminder notification and reschedules the next alarm.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.fluidcheck.SHOW_REMINDER"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_FREQUENCY_MINUTES = "frequency_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_REMINDER) return

        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: "GUEST"
        val frequencyMinutes = intent.getIntExtra(EXTRA_FREQUENCY_MINUTES, 60)

        // goAsync() gives us up to ~30 seconds to do async work in a BroadcastReceiver
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsRepository = UserPreferencesRepository(context)
                val notificationsEnabled = prefsRepository.getNotificationsEnabled(userId).first()

                // Only show if not explicitly disabled
                if (notificationsEnabled != false) {
                    NotificationHelper.showHydrationReminder(context)
                }

                // Chain the next alarm so reminders keep firing even when the app is closed
                NotificationScheduler.scheduleReminders(context, userId, frequencyMinutes)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
