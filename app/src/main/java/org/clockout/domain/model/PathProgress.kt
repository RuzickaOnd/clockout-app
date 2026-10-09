package org.clockout.domain.model

import java.time.LocalDate

data class PathProgress(
    val closedDays: Int,
    val stage: PathStage,
    val nextStage: PathStage?,
    /** Null on the last stage, where there is nothing left to reach. */
    val daysToNextStage: Int?,
    val rhythmWeeks: Int,
    val longestRhythmWeeks: Int,
    val isCurrentWeekInRhythm: Boolean
) {
    companion object {
        val EMPTY = PathPolicy.progress(emptySet(), includeWeekends = false, today = LocalDate.MIN)
    }
}

object PathPolicy {

    fun progress(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): PathProgress {
        val stage = PathStage.forDays(days.size)
        val next = stage.next
        val rhythm = PathRhythm.evaluate(days, includeWeekends, today)
        return PathProgress(
            closedDays = days.size,
            stage = stage,
            nextStage = next,
            daysToNextStage = next?.let { it.minDays - days.size },
            rhythmWeeks = rhythm.current,
            longestRhythmWeeks = rhythm.longest,
            isCurrentWeekInRhythm = rhythm.isCurrentWeekInRhythm
        )
    }
}
