package org.clockout.notification

/**
 * What the daily reminder alarm should do when it fires.
 *
 * A day the ritual is not offered on is skipped, not cancelled: the alarm still has to be
 * rearmed, otherwise a single weekend would silence the reminder for good.
 */
enum class ReminderOutcome(val reschedules: Boolean) {
    CANCEL(reschedules = false),
    SKIP_TODAY(reschedules = true),
    NOTIFY(reschedules = true)
}

object ReminderPolicy {

    fun outcomeFor(isEnabled: Boolean, isRitualDay: Boolean): ReminderOutcome = when {
        !isEnabled -> ReminderOutcome.CANCEL
        !isRitualDay -> ReminderOutcome.SKIP_TODAY
        else -> ReminderOutcome.NOTIFY
    }
}
