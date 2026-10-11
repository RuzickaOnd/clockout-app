package org.clockout

import org.clockout.domain.model.PathRhythm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PathRhythmTest {

    // 2026-09-14 is a Monday; W4 is the week of TODAY.
    private val w1 = LocalDate.of(2026, 9, 14)
    private val w2 = w1.plusWeeks(1)
    private val w3 = w1.plusWeeks(2)
    private val w4 = w1.plusWeeks(3)
    private val today = w4.plusDays(2) // Wednesday

    private fun days(monday: LocalDate, vararg offsets: Int) = offsets.map { monday.plusDays(it.toLong()) }
    private fun workweek(monday: LocalDate) = days(monday, 0, 1, 2, 3, 4)

    private fun eval(
        days: Collection<LocalDate>,
        today: LocalDate = this.today
    ) = PathRhythm.evaluate(days.toSet(), today)

    @Test
    fun `no days means no rhythm`() {
        assertEquals(PathRhythm.Result(0, 0, false), eval(emptyList()))
    }

    @Test
    fun `three full weeks make a rhythm of three`() {
        val result = eval(workweek(w1) + workweek(w2) + workweek(w3))
        assertEquals(3, result.current)
        assertEquals(3, result.longest)
        assertFalse(result.isCurrentWeekInRhythm)
    }

    @Test
    fun `four of five days is enough for a week`() {
        assertEquals(3, eval(workweek(w1) + days(w2, 0, 1, 2, 3) + workweek(w3)).current)
    }

    @Test
    fun `one weak week is tolerated but not counted`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(2, result.current)
        assertEquals(2, result.longest)
    }

    @Test
    fun `two weak weeks in a row end the rhythm`() {
        val result = eval(workweek(w1))
        assertEquals(0, result.current)
        assertEquals(1, result.longest)
    }

    @Test
    fun `the week in progress neither breaks nor extends the rhythm until it is met`() {
        val inProgress = eval(workweek(w3) + days(w4, 0, 1))
        assertEquals(1, inProgress.current)
        assertFalse(inProgress.isCurrentWeekInRhythm)

        val met = eval(workweek(w3) + days(w4, 0, 1, 2, 3), today = w4.plusDays(3))
        assertEquals(2, met.current)
        assertTrue(met.isCurrentWeekInRhythm)
    }

    @Test
    fun `the first week only offers the days from the first ritual on`() {
        assertEquals(1, eval(days(w3, 2, 3, 4)).current)
        assertEquals(1, eval(days(w3, 4)).current)
    }

    @Test
    fun `a closed weekend day stands in for a missed weekday`() {
        assertEquals(1, eval(days(w3, 0, 1, 2, 5)).current)
    }

    @Test
    fun `a closed weekend day counts whatever the setting is now`() {
        // Closed while weekends were on; switching them off must not un-light the week.
        val result = eval(workweek(w2) + days(w3, 0, 1, 2, 5, 6))
        assertEquals(2, result.current)
        assertEquals(true, result.weeks[w3])
    }

    @Test
    fun `a first ritual on a weekend starts a week of its own`() {
        assertEquals(1, eval(days(w3, 5)).current)
    }

    @Test
    fun `the longest rhythm survives a break`() {
        val e1 = w1.minusWeeks(4)
        val history = workweek(e1) + workweek(e1.plusWeeks(1)) + workweek(e1.plusWeeks(2)) + workweek(w2)
        val result = eval(history)
        assertEquals(1, result.current)
        assertEquals(3, result.longest)
    }

    @Test
    fun `weeks lists every week from the first one with its result`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(mapOf(w1 to true, w2 to false, w3 to true, w4 to false), result.weeks)
    }

    @Test
    fun `the week in progress appears in weeks once it is met`() {
        val result = eval(workweek(w3) + days(w4, 0, 1, 2, 3), today = w4.plusDays(3))
        assertEquals(true, result.weeks[w4])
    }

    @Test
    fun `a week before the first ritual is not in weeks`() {
        assertEquals(setOf(w3, w4), eval(days(w3, 4)).weeks.keys)
    }

    @Test
    fun `current and longest agree with weeks`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(2, result.weeks.values.count { it })
        assertEquals(2, result.current)
    }
}
