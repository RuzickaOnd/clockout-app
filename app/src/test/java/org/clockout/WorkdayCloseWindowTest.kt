package org.clockout

import org.clockout.domain.model.WorkdayCloseWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "16-21" names the hours the day can still be closed in, so the window opens as 16:00 strikes
 * and is over once the clock reads 21:00 - not an hour later.
 */
class WorkdayCloseWindowTest {

    @Test
    fun `the hour before the window is outside it`() {
        assertFalse(WorkdayCloseWindow.contains(15))
    }

    @Test
    fun `the window opens at sixteen`() {
        assertTrue(WorkdayCloseWindow.contains(16))
    }

    @Test
    fun `the last minutes before twenty one are still inside`() {
        assertTrue(WorkdayCloseWindow.contains(20))
    }

    @Test
    fun `twenty one is already outside`() {
        assertFalse(WorkdayCloseWindow.contains(21))
    }

    @Test
    fun `the small hours are outside`() {
        assertFalse(WorkdayCloseWindow.contains(0))
        assertFalse(WorkdayCloseWindow.contains(9))
    }
}
