package org.clockout.notification

import android.content.Context
import org.clockout.domain.platform.ReminderScheduler

class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext

    override fun schedule(hour: Int, minute: Int) =
        NotificationScheduler.scheduleDailyNotification(appContext, hour, minute)

    override fun cancel() = NotificationScheduler.cancelNotification(appContext)
}
