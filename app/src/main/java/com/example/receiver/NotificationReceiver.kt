package com.example.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.RitualPreferences
import com.example.domain.model.AppLanguage
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WORKDAY_END = "com.example.ACTION_WORKDAY_END_NOTIFICATION"
        private const val NOTIFICATION_ID = 8842
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = RitualPreferences(context)
                val isEnabled = preferences.notificationEnabledFlow.first()
                val hour = preferences.notificationHourFlow.first()
                val minute = preferences.notificationMinuteFlow.first()
                val language = preferences.languageFlow.first()

                if (!isEnabled) {
                    NotificationScheduler.cancelNotification(context)
                    return@launch
                }

                if (action == ACTION_WORKDAY_END) {
                    showNotification(context, language)
                    // Reschedule for next day
                    NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                } else if (action == Intent.ACTION_BOOT_COMPLETED) {
                    NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                }
            } catch (_: Exception) {
                // Ignore background notification failure
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, language: AppLanguage) {
        if (!NotificationScheduler.hasNotificationPermission(context)) {
            return
        }

        NotificationScheduler.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (language) {
            AppLanguage.CZECH -> "Padla – čas ukončit práci"
            AppLanguage.ENGLISH -> "ClockOut – time to wrap up work"
        }

        val text = when (language) {
            AppLanguage.CZECH -> "Zastav se na 2 minuty a vědomě uzavři dnešní pracovní den."
            AppLanguage.ENGLISH -> "Take 2 minutes to pause and close your workday."
        }

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }
}
