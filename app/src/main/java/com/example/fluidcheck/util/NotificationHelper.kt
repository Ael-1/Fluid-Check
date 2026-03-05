package com.example.fluidcheck.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fluidcheck.MainActivity
import com.example.fluidcheck.R
import java.util.Random

/**
 * Utility for creating notification channels and posting notifications.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "hydration_reminders"
    private const val CHANNEL_NAME = "Hydration Reminders"
    private const val CHANNEL_DESCRIPTION = "Periodic reminders to drink water and stay hydrated."
    
    private const val REMINDER_NOTIFICATION_ID = 1001
    private const val SMART_NOTIFICATION_ID = 1002

    private val motivationalMessages = listOf(
        "Time to hydrate! A glass of water is a gift to your body. 💧",
        "Keep the flow going! Drink some water now. 🌊",
        "Don't forget to sip! Staying hydrated keeps your brain sharp. 🧠",
        "Water is fuel for your cells. Take a quick hydration break! 🔋",
        "Feeling tired? A glass of water might be just what you need. ⚡",
        "Stay fresh! Your future self will thank you for this glass of water. ✨",
        "Hydration check! How much have you drank lately? 🥤",
        "Pure H2O: the original energy drink. Drink up! 💎"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showHydrationReminder(context: Context) {
        val message = motivationalMessages[Random().nextInt(motivationalMessages.size)]
        showNotification(context, "Hydration Reminder", message, REMINDER_NOTIFICATION_ID)
    }

    fun showSmartReminder(context: Context, title: String, message: String) {
        showNotification(context, title, message, SMART_NOTIFICATION_ID)
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.fluid_check_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted on Android 13+
            }
        }
    }
}
