package org.clockout

import org.clockout.notification.ReminderOutcome
import org.clockout.notification.ReminderPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the daily alarm does when it fires. A skipped day must still rearm the alarm,
 * otherwise one weekend would silence the reminder for good.
 */
class ReminderPolicyTest {

    @Test
    fun `a disabled reminder cancels the alarm`() {
        assertEquals(
            ReminderOutcome.CANCEL,
            ReminderPolicy.outcomeFor(isEnabled = false, isRitualDay = true)
        )
    }

    @Test
    fun `a day without the ritual is skipped rather than cancelled`() {
        assertEquals(
            ReminderOutcome.SKIP_TODAY,
            ReminderPolicy.outcomeFor(isEnabled = true, isRitualDay = false)
        )
    }

    @Test
    fun `an enabled reminder on a ritual day notifies`() {
        assertEquals(
            ReminderOutcome.NOTIFY,
            ReminderPolicy.outcomeFor(isEnabled = true, isRitualDay = true)
        )
    }

    @Test
    fun `a skipped day still reschedules the alarm`() {
        assertEquals(true, ReminderOutcome.SKIP_TODAY.reschedules)
        assertEquals(true, ReminderOutcome.NOTIFY.reschedules)
        assertEquals(false, ReminderOutcome.CANCEL.reschedules)
    }
}
