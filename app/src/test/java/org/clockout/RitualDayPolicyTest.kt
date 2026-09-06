package org.clockout

import org.clockout.domain.model.RitualDayPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * The single rule deciding which weekdays offer the ritual. Both the home screen and the
 * reminder notification consult it, so a gap here would let the two disagree.
 */
class RitualDayPolicyTest {

    private val weekdays = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )

    private val weekend = listOf(Calendar.SATURDAY, Calendar.SUNDAY)

    @Test
    fun `weekdays are ritual days when weekends are excluded`() {
        for (day in weekdays) {
            assertTrue("day $day", RitualDayPolicy.isRitualDay(day, includeWeekends = false))
        }
    }

    @Test
    fun `saturday and sunday are not ritual days when weekends are excluded`() {
        for (day in weekend) {
            assertFalse("day $day", RitualDayPolicy.isRitualDay(day, includeWeekends = false))
        }
    }

    @Test
    fun `every day of the week is a ritual day when weekends are included`() {
        for (day in weekdays + weekend) {
            assertTrue("day $day", RitualDayPolicy.isRitualDay(day, includeWeekends = true))
        }
    }
}
