package org.clockout

import org.clockout.domain.model.WorkdayCloseWindow
import org.junit.Assert.assertEquals
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

    @Test
    fun `a morning time is moved up to the opening hour, keeping its minute`() {
        assertEquals(16 to 15, WorkdayCloseWindow.coerce(10, 15))
    }

    @Test
    fun `a late time is moved back to the last open hour, keeping its minute`() {
        assertEquals(20 to 30, WorkdayCloseWindow.coerce(22, 30))
    }

    @Test
    fun `a time inside the window is left alone`() {
        assertEquals(18 to 45, WorkdayCloseWindow.coerce(18, 45))
    }
}
