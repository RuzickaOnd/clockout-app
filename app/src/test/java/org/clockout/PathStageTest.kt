package org.clockout

import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PathStageTest {

    @Test
    fun `stages switch exactly at their thresholds`() {
        assertEquals(PathStage.AT_DESK, PathStage.forDays(0))
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.forDays(1))
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.forDays(4))
        assertEquals(PathStage.COAT_ON, PathStage.forDays(5))
        assertEquals(PathStage.THRESHOLD, PathStage.forDays(249))
        assertEquals(PathStage.HOME, PathStage.forDays(250))
        assertEquals(PathStage.HOME, PathStage.forDays(1000))
    }

    @Test
    fun `thresholds match the spec and rise strictly`() {
        assertEquals(
            listOf(0, 1, 5, 10, 20, 35, 50, 75, 100, 150, 250),
            PathStage.entries.map { it.minDays }
        )
    }

    @Test
    fun `the last stage has no next one`() {
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.AT_DESK.next)
        assertNull(PathStage.HOME.next)
    }

    @Test
    fun `progress counts the days left to the next stage`() {
        val monday = LocalDate.of(2026, 10, 5)
        val days = (0L..2L).map { monday.plusDays(it) }.toSet()

        val progress = PathPolicy.progress(days, today = monday.plusDays(2))

        assertEquals(3, progress.closedDays)
        assertEquals(PathStage.LAPTOP_CLOSED, progress.stage)
        assertEquals(PathStage.COAT_ON, progress.nextStage)
        assertEquals(2, progress.daysToNextStage)
    }

    @Test
    fun `progress on the last stage has nothing left to reach`() {
        val start = LocalDate.of(2025, 1, 1)
        val days = (0L until 250L).map { start.plusDays(it) }.toSet()

        val progress = PathPolicy.progress(days, today = start.plusDays(249))

        assertEquals(PathStage.HOME, progress.stage)
        assertNull(progress.nextStage)
        assertNull(progress.daysToNextStage)
    }
}
