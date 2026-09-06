package org.clockout

import org.clockout.domain.model.RitualStreakInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** The badge on the home screen reads the completion share off the active day count. */
class RitualStreakInfoPercentTest {

    private fun streakInfo(completed: Int, target: Int) = RitualStreakInfo(
        completedThisWeekCount = completed,
        targetWorkDays = target,
        isTodayCompleted = false,
        todayLog = null,
        weekDays = emptyList()
    )

    @Test
    fun `a full five day week is a hundred percent`() {
        assertEquals(100, streakInfo(completed = 5, target = 5).completionPercent)
    }

    @Test
    fun `five closed days of a seven day week is not yet a full week`() {
        assertEquals(71, streakInfo(completed = 5, target = 7).completionPercent)
    }

    @Test
    fun `an empty week is zero percent`() {
        assertEquals(0, streakInfo(completed = 0, target = 7).completionPercent)
    }
}
