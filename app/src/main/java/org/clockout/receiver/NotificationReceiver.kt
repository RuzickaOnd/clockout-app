package org.clockout.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.clockout.MainActivity
import org.clockout.R
import org.clockout.appContainer
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualDayPolicy
import org.clockout.notification.NotificationScheduler
import org.clockout.notification.ReminderOutcome
import org.clockout.notification.ReminderPolicy
import org.clockout.util.localizedFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WORKDAY_END = "org.clockout.ACTION_WORKDAY_END_NOTIFICATION"
        private const val NOTIFICATION_ID = 8842
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = context.appContainer.preferences
                val isEnabled = preferences.notificationEnabledFlow.first()
                val hour = preferences.notificationHourFlow.first()
                val minute = preferences.notificationMinuteFlow.first()
                val language = preferences.languageFlow.first()
                val includeWeekends = preferences.includeWeekendsFlow.first()

                val outcome = ReminderPolicy.outcomeFor(
                    isEnabled = isEnabled,
                    isRitualDay = RitualDayPolicy.isTodayRitualDay(includeWeekends)
                )

                if (outcome == ReminderOutcome.CANCEL) {
                    NotificationScheduler.cancelNotification(context)
                    return@launch
                }

                if (action == ACTION_WORKDAY_END) {
                    if (outcome == ReminderOutcome.NOTIFY) {
                        showNotification(context, language)
                    }
                    // Reschedule for next day even on a skipped day, so the alarm survives the weekend
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

        val resources = context.localizedFor(language).resources
        val title = resources.getString(R.string.notification_workday_end_title)
        val text = resources.getString(R.string.notification_workday_end_text)

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
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
