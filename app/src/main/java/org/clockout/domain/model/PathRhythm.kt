package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Counts rhythm in weeks rather than days, so a single missed day never breaks anything.
 *
 * A week is in rhythm when all but one of its weekdays were closed. With weekends on, a closed
 * Saturday or Sunday counts too and can stand in for a missed weekday, but the threshold stays the
 * weekdays' - so switching weekends on can only add to a rhythm, never re-score past weeks below it. Only two weak weeks in a
 * row end a rhythm - one week of illness or holiday is tolerated, it just does not count. The week
 * in progress is counted once it reaches the threshold and otherwise left out, so the rhythm
 * cannot drop on a Monday morning. The week of the very first closed day offers only the days from
 * that day on: whoever starts on a Wednesday was not absent on Monday, the app was.
 */
object PathRhythm {

    data class Result(val current: Int, val longest: Int, val isCurrentWeekInRhythm: Boolean)

    private const val WEAK_WEEKS_THAT_END_A_RHYTHM = 2

    fun evaluate(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): Result {
        if (days.isEmpty()) return Result(0, 0, false)
        val firstDay = days.min()
        val currentWeek = today.mondayOfWeek()

        val pastWeeks = generateSequence(firstDay.mondayOfWeek()) { it.plusWeeks(1) }
            .takeWhile { it.isBefore(currentWeek) }
            .mapNotNull { isWeekMet(it, days, includeWeekends, firstDay) }
            .toList()
        val isCurrentWeekInRhythm = isWeekMet(currentWeek, days, includeWeekends, firstDay) == true

        var run = 0
        var longest = 0
        var weakInARow = 0
        for (met in pastWeeks + listOfNotNull(true.takeIf { isCurrentWeekInRhythm })) {
            if (met) {
                run++
                weakInARow = 0
                longest = maxOf(longest, run)
            } else {
                weakInARow++
                if (weakInARow >= WEAK_WEEKS_THAT_END_A_RHYTHM) run = 0
            }
        }
        return Result(current = run, longest = longest, isCurrentWeekInRhythm = isCurrentWeekInRhythm)
    }

    /** Null for a week that offered no day at all, which neither counts nor breaks anything. */
    private fun isWeekMet(
        monday: LocalDate,
        days: Set<LocalDate>,
        includeWeekends: Boolean,
        firstDay: LocalDate
    ): Boolean? {
        val week = (0L until 7L).map { monday.plusDays(it) }.filterNot { it.isBefore(firstDay) }
        val weekdays = week.filter { it.dayOfWeek < DayOfWeek.SATURDAY }
        val counted = if (includeWeekends) week else weekdays
        if (counted.isEmpty()) return null
        val threshold = maxOf(1, weekdays.size - 1)
        return counted.count { it in days } >= threshold
    }

    private fun LocalDate.mondayOfWeek(): LocalDate =
        with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
