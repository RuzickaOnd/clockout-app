package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Counts rhythm in weeks rather than days, so a single missed day never breaks anything.
 *
 * A week is in rhythm when all but one of its weekdays were closed. A closed Saturday or Sunday
 * counts as well and can stand in for a missed weekday, but never raises the threshold - and it
 * counts whatever the weekend setting says now, because a weekend day can only have been closed
 * while weekends were on. Switching weekends either way can therefore never re-score a past week
 * below its threshold. Only two weak weeks in a row end a rhythm - one week of illness or holiday
 * is tolerated, it just does not count. The week in progress is counted once it reaches the
 * threshold and otherwise left out, so the rhythm cannot drop on a Monday morning. The week of the
 * very first closed day offers only the days from that day on: whoever starts on a Wednesday was
 * not absent on Monday, the app was.
 */
object PathRhythm {

    data class Result(
        val current: Int,
        val longest: Int,
        val isCurrentWeekInRhythm: Boolean,
        /**
         * Monday of every week from the first closed day's week to today's, mapped to whether it
         * was in rhythm. The night sky draws its constellations from this map, so the
         * path and the sky can never disagree about a week.
         */
        val weeks: Map<LocalDate, Boolean> = emptyMap()
    )

    private const val WEAK_WEEKS_THAT_END_A_RHYTHM = 2

    fun evaluate(days: Set<LocalDate>, today: LocalDate): Result {
        if (days.isEmpty()) return Result(0, 0, false)
        val firstDay = days.min()
        val currentWeek = today.mondayOfWeek()

        val weeks = generateSequence(firstDay.mondayOfWeek()) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(currentWeek) }
            .associateWith { monday -> isWeekMet(monday, days, firstDay) }
        val isCurrentWeekInRhythm = weeks[currentWeek] == true

        var run = 0
        var longest = 0
        var weakInARow = 0
        // The week in progress only ever adds: until it is met it is left out rather than weak.
        for ((monday, met) in weeks) {
            if (monday == currentWeek && !met) continue
            if (met) {
                run++
                weakInARow = 0
                longest = maxOf(longest, run)
            } else {
                weakInARow++
                if (weakInARow >= WEAK_WEEKS_THAT_END_A_RHYTHM) run = 0
            }
        }
        return Result(run, longest, isCurrentWeekInRhythm, weeks)
    }

    /**
     * Every week from the first closed day's Monday on offers at least one day - the first day
     * itself - so there is no "empty week" case to skip.
     */
    private fun isWeekMet(monday: LocalDate, days: Set<LocalDate>, firstDay: LocalDate): Boolean {
        val week = (0L until 7L).map { monday.plusDays(it) }.filterNot { it.isBefore(firstDay) }
        val weekdays = week.filter { it.dayOfWeek < DayOfWeek.SATURDAY }
        val threshold = maxOf(1, weekdays.size - 1)
        return week.count { it in days } >= threshold
    }
}
