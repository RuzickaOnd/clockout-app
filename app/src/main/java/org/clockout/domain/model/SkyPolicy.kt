package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

data class SkyStar(val date: LocalDate, val pointIndex: Int, val mood: MoodTag?)

data class SkyWeek(
    val monday: LocalDate,
    val constellationNumber: Int,
    val isInRhythm: Boolean,
    /** In rhythm and with at least one line to draw - a lone star names nothing yet. */
    val isLit: Boolean,
    val stars: List<SkyStar>
)

data class SkyMonth(val yearMonth: YearMonth, val weeks: List<SkyWeek>) {
    val starCount: Int get() = weeks.sumOf { it.stars.size }
}

data class SkySummary(
    val stars: Int,
    val constellationsLit: Int,
    val litThisYear: Int,
    val weeksThisYear: Int,
    /** Distinct constellations lit in the previous ISO year, so January does not start from nothing. */
    val litLastYear: Int,
    val timesLitByNumber: Map<Int, Int>,
    /** The ISO years each constellation was lit in, oldest first. */
    val litYearsByNumber: Map<Int, List<Int>>
)

/**
 * Everything the night sky is drawn from. Months are built on demand, page by page, rather than
 * all at once - a long history would otherwise rebuild years of sky on every new day.
 */
data class SkyData(
    val days: Set<LocalDate>,
    val logs: Map<LocalDate, RitualLogEntry>,
    val rhythmWeeks: Map<LocalDate, Boolean>,
    val today: LocalDate,
    /** Only for the countdown: which future days the ritual will be offered on. */
    val includeWeekends: Boolean = false
) {
    // Computed once per emission rather than on every read: the screen reads both on each recomposition.
    val months: List<YearMonth> by lazy { SkyPolicy.months(days, today) }
    val summary: SkySummary by lazy { SkyPolicy.summary(days, rhythmWeeks, today) }
    val daysToFirstConstellation: Int? by lazy { SkyPolicy.daysToFirstConstellation(days, today, includeWeekends) }
    fun month(yearMonth: YearMonth): SkyMonth = SkyPolicy.month(yearMonth, days, logs, rhythmWeeks)

    companion object {
        val EMPTY = SkyData(emptySet(), emptyMap(), emptyMap(), LocalDate.MIN)
    }
}

/**
 * A week belongs to the month its Thursday falls in - the rule ISO uses for years - so a week is
 * never split across two skies and every month holds four or five of them.
 */
object SkyPolicy {

    fun constellationNumber(monday: LocalDate): Int = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    fun monthOf(monday: LocalDate): YearMonth = YearMonth.from(monday.plusDays(3))

    fun month(
        yearMonth: YearMonth,
        days: Set<LocalDate>,
        logs: Map<LocalDate, RitualLogEntry>,
        rhythmWeeks: Map<LocalDate, Boolean>
    ): SkyMonth {
        val firstThursday = yearMonth.atDay(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY))
        val weeks = generateSequence(firstThursday.minusDays(3)) { it.plusWeeks(1) }
            .takeWhile { monthOf(it) == yearMonth }
            .map { monday ->
                SkyWeek(
                    monday = monday,
                    constellationNumber = constellationNumber(monday),
                    isInRhythm = rhythmWeeks[monday] == true,
                    isLit = isLit(monday, days, rhythmWeeks),
                    stars = (0L until 7L).map { monday.plusDays(it) }
                        .filter { it in days }
                        .map { SkyStar(it, it.dayOfWeek.value - 1, logs[it]?.moodTag) }
                )
            }
            .toList()
        return SkyMonth(yearMonth, weeks)
    }

    /**
     * A constellation lights up once its week is in rhythm and at least one of its lines has both
     * stars - a week that is in rhythm on a single day (the first, partial week) shows that star,
     * but has no figure to name yet.
     */
    fun isLit(monday: LocalDate, days: Set<LocalDate>, rhythmWeeks: Map<LocalDate, Boolean>): Boolean {
        if (rhythmWeeks[monday] != true) return false
        val closed = (0 until 7).filter { monday.plusDays(it.toLong()) in days }.toSet()
        return ConstellationCatalog.forWeek(constellationNumber(monday)).edges.any { (a, b) -> a in closed && b in closed }
    }

    private const val COUNTDOWN_LOOKAHEAD_DAYS = 21

    /**
     * How many more closed days light the first constellation, assuming the user closes every day
     * the ritual is offered on from today - the honest count, rather than the smallest subset of
     * days, which the first-week rule would make favour starting late in the week. Null once any
     * constellation has been lit: from then on the sky is discovered and needs no teaser.
     */
    fun daysToFirstConstellation(days: Set<LocalDate>, today: LocalDate, includeWeekends: Boolean): Int? {
        val weeks = PathRhythm.evaluate(days, today).weeks
        if (weeks.keys.any { isLit(it, days, weeks) }) return null
        val closed = days.toMutableSet()
        var added = 0
        var day = today
        repeat(COUNTDOWN_LOOKAHEAD_DAYS) {
            if ((includeWeekends || day.dayOfWeek < DayOfWeek.SATURDAY) && closed.add(day)) {
                added++
                val simulated = PathRhythm.evaluate(closed, day).weeks
                if (isLit(day.mondayOfWeek(), closed, simulated)) return added
            }
            day = day.plusDays(1)
        }
        return null
    }

    fun months(days: Set<LocalDate>, today: LocalDate): List<YearMonth> {
        if (days.isEmpty()) return emptyList()
        val first = monthOf(days.min().mondayOfWeek())
        val last = maxOf(monthOf(today.mondayOfWeek()), first)
        return generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(last) }.toList()
    }

    fun summary(days: Set<LocalDate>, rhythmWeeks: Map<LocalDate, Boolean>, today: LocalDate): SkySummary {
        val lit = rhythmWeeks.keys.filter { isLit(it, days, rhythmWeeks) }
        val year = today.get(IsoFields.WEEK_BASED_YEAR)
        fun distinctIn(isoYear: Int) =
            lit.filter { it.get(IsoFields.WEEK_BASED_YEAR) == isoYear }.map(::constellationNumber).toSet().size
        return SkySummary(
            stars = days.size,
            constellationsLit = lit.size,
            litThisYear = distinctIn(year),
            weeksThisYear = today.range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).maximum.toInt(),
            litLastYear = distinctIn(year - 1),
            timesLitByNumber = lit.groupingBy(::constellationNumber).eachCount(),
            litYearsByNumber = lit.groupBy(::constellationNumber)
                .mapValues { (_, mondays) -> mondays.map { it.get(IsoFields.WEEK_BASED_YEAR) }.sorted() }
        )
    }

    private fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
