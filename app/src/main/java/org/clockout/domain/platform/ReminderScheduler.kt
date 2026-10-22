package org.clockout.domain.platform

/** Plans the daily workday-end reminder; the alarm machinery behind it is the platform's business. */
interface ReminderScheduler {
    fun schedule(hour: Int, minute: Int)
    fun cancel()
}
