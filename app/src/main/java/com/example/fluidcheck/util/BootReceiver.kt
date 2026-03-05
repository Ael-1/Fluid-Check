package com.example.fluidcheck.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that re-schedules notifications after the device reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(Dispatchers.IO)
            val prefsRepository = UserPreferencesRepository(context)
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: "GUEST"

            scope.launch {
                val notificationsEnabled = prefsRepository.getNotificationsEnabled(userId).first()
                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true

                if (notificationsEnabled == true || (notificationsEnabled == null && hasPermission)) {
                    val frequency = prefsRepository.getReminderFrequency(userId).first().toIntOrNull() ?: 60
                    NotificationScheduler.scheduleReminders(context, userId, frequency)
                    NotificationScheduler.scheduleSmartReminders(context, userId)
                }
            }
        }
    }
}
