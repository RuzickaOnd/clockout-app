package org.clockout

import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SkyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SkyPolicyTest {

    private fun log(date: LocalDate, mood: MoodTag) = RitualLogEntry(date.toString(), "Q", null, mood, 0L)

    private fun data(
        days: List<LocalDate>,
        rhythm: Map<LocalDate, Boolean> = emptyMap(),
        logs: List<RitualLogEntry> = emptyList(),
        today: LocalDate = LocalDate.of(2026, 10, 9)
    ) = SkyData(days.toSet(), logs.associateBy { LocalDate.parse(it.date) }, rhythm, today)

    @Test
    fun `a week belongs to the month of its thursday`() {
        // Mon 28 Sep 2026 - Sun 4 Oct 2026; Thursday is 1 October.
        assertEquals(YearMonth.of(2026, 10), SkyPolicy.monthOf(LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun `a week across new year belongs to January as constellation 1`() {
        val monday = LocalDate.of(2025, 12, 29)
        assertEquals(YearMonth.of(2026, 1), SkyPolicy.monthOf(monday))
        assertEquals(1, SkyPolicy.constellationNumber(monday))
    }

    @Test
    fun `2026 has 53 weeks and the blue moon`() {
        val monday = LocalDate.of(2026, 12, 28)
        assertEquals(53, SkyPolicy.constellationNumber(monday))
        val summary = data(listOf(monday), today = monday.plusDays(1)).summary
        assertEquals(53, summary.weeksThisYear)
    }

    @Test
    fun `a month lists its four or five weeks in order`() {
        val october = data(listOf(LocalDate.of(2026, 10, 5))).month(YearMonth.of(2026, 10))
        assertEquals(
            listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)),
            october.weeks.map { it.monday }
        )
        assertEquals(listOf(40, 41, 42, 43, 44), october.weeks.map { it.constellationNumber })
    }

    @Test
    fun `a star sits on the point of its weekday and carries the mood of its entry`() {
        val friday = LocalDate.of(2026, 10, 9)
        val saturday = LocalDate.of(2026, 10, 10)
        val week = data(listOf(friday, saturday), logs = listOf(log(friday, MoodTag.TENSE)))
            .month(YearMonth.of(2026, 10)).weeks.first { it.monday == LocalDate.of(2026, 10, 5) }
        assertEquals(listOf(4, 5), week.stars.map { it.pointIndex })
        assertEquals(MoodTag.TENSE, week.stars[0].mood)
        assertNull(week.stars[1].mood)
    }

    @Test
    fun `only a week in rhythm is a lit constellation`() {
        // Week 41 is Knitting, whose Monday and Friday stars share a line.
        val monday = LocalDate.of(2026, 10, 5)
        val nextMonday = LocalDate.of(2026, 10, 12)
        val month = data(
            listOf(monday, monday.plusDays(4), nextMonday, nextMonday.plusDays(4)),
            rhythm = mapOf(monday to true, nextMonday to false)
        ).month(YearMonth.of(2026, 10))
        assertTrue(month.weeks.first { it.monday == monday }.isLit)
        assertFalse(month.weeks.first { it.monday == nextMonday }.isLit)
    }

    @Test
    fun `a lone star in a week in rhythm is not a lit constellation yet`() {
        val friday = LocalDate.of(2026, 10, 9)
        val sky = data(listOf(friday), rhythm = mapOf(LocalDate.of(2026, 10, 5) to true))
        val week = sky.month(YearMonth.of(2026, 10)).weeks.first { it.monday == LocalDate.of(2026, 10, 5) }
        assertTrue(week.isInRhythm)
        assertFalse(week.isLit)
        assertEquals(0, sky.summary.constellationsLit)
    }

    @Test
    fun `two stars without a shared line do not light a constellation either`() {
        // In Knitting, Monday and Tuesday are both tied to Friday but not to each other.
        val monday = LocalDate.of(2026, 10, 5)
        assertFalse(SkyPolicy.isLit(monday, setOf(monday, monday.plusDays(1)), mapOf(monday to true)))
        assertTrue(SkyPolicy.isLit(monday, setOf(monday, monday.plusDays(4)), mapOf(monday to true)))
    }

    @Test
    fun `months run from the first star to the current week`() {
        val sky = data(listOf(LocalDate.of(2026, 8, 3)), today = LocalDate.of(2026, 10, 9))
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)), sky.months)
    }

    @Test
    fun `a sky without days has no months`() {
        assertEquals(emptyList<YearMonth>(), SkyData.EMPTY.months)
    }

    @Test
    fun `the summary counts stars, lit weeks and distinct constellations this year`() {
        val lit2025 = LocalDate.of(2025, 10, 6)   // week 41 of 2025
        val lit2026a = LocalDate.of(2026, 10, 5)  // week 41 of 2026
        val lit2026b = LocalDate.of(2026, 9, 28)  // week 40 of 2026
        val summary = data(
            // Each lit week closes the two days its figure joins with a line.
            days = listOf(lit2025, lit2025.plusDays(4), lit2026a, lit2026a.plusDays(4), lit2026b, lit2026b.plusDays(1)),
            rhythm = mapOf(lit2025 to true, lit2026a to true, lit2026b to true, LocalDate.of(2026, 9, 21) to false)
        ).summary
        assertEquals(6, summary.stars)
        assertEquals(3, summary.constellationsLit)
        assertEquals(2, summary.litThisYear)
        assertEquals(53, summary.weeksThisYear)
        assertEquals(mapOf(41 to 2, 40 to 1), summary.timesLitByNumber)
    }

    @Test
    fun `a month without stars has its weeks but no stars`() {
        val sky = data(listOf(LocalDate.of(2026, 8, 3)))
        assertEquals(0, sky.month(YearMonth.of(2026, 9)).starCount)
    }

    @Test
    fun `the summary of an empty sky is empty`() {
        assertEquals(0, SkyData.EMPTY.summary.stars)
        assertEquals(0, SkyData.EMPTY.summary.constellationsLit)
    }
}
