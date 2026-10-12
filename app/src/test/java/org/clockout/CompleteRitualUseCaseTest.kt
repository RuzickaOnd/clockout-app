package org.clockout

import kotlinx.coroutines.test.runTest
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathRhythm
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SkyPolicy
import org.clockout.domain.usecase.CompleteRitualUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.testing.FakeProgressRepository
import org.clockout.testing.FakeRitualLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CompleteRitualUseCaseTest {

    private val today = LocalDate.of(2026, 10, 12)

    private fun sky(days: Set<LocalDate>, on: LocalDate = today) =
        SkyData(days, emptyMap(), PathRhythm.evaluate(days, on).weeks, on, includeWeekends = false)

    /** [beforeOn] and [afterOn] are the days the two snapshots are read on. */
    private fun progress(
        before: Set<LocalDate>,
        after: Set<LocalDate>,
        failure: Exception? = null,
        beforeOn: LocalDate = today,
        afterOn: LocalDate = today
    ) = FakeProgressRepository(
        paths = ArrayDeque(listOf(PathPolicy.progress(before, beforeOn), PathPolicy.progress(after, afterOn))),
        skies = ArrayDeque(listOf(sky(before, beforeOn), sky(after, afterOn))),
        failure = failure
    )

    private fun useCase(logs: FakeRitualLogRepository, progress: FakeProgressRepository) =
        CompleteRitualUseCase(logs, progress, ShouldRequestReviewUseCase(logs))

    @Test
    fun `the first closed day reaches the first stage`() = runTest {
        val logs = FakeRitualLogRepository(loggedDay = today)
        val result = useCase(logs, progress(emptySet(), setOf(today)))("A", MoodTag.CALM, "Q?", moodWasChosen = true)

        assertEquals(PathStage.LAPTOP_CLOSED, result.reachedStage)
        assertEquals(1, logs.logged.size)
    }

    @Test
    fun `a broken sky still writes the entry and celebrates nothing`() = runTest {
        val logs = FakeRitualLogRepository(loggedDay = today)
        val result = useCase(logs, progress(emptySet(), setOf(today), IllegalStateException("sky")))(
            "A", MoodTag.CALM, "Q?", moodWasChosen = true
        )

        assertEquals(1, logs.logged.size)
        assertNull(result.reachedStage)
        assertNull(result.litConstellationNumber)
        assertNull(result.skyTeaser)
    }

    @Test
    fun `a skipped mood never asks for a review`() = runTest {
        val logs = FakeRitualLogRepository(loggedDay = today, completedCount = 50)
        val result = useCase(logs, progress(emptySet(), setOf(today)))(null, MoodTag.CALM, "Q?", moodWasChosen = false)

        assertFalse(result.shouldRequestReview)
        assertFalse(logs.reviewShown)
    }

    @Test
    fun `a ritual saved just before midnight announces the week it was written into`() = runTest {
        // Week N runs Monday 12 - Sunday 18 October; the snapshot after the write already reads
        // Monday 19, the first day of week N + 1.
        val sunday = LocalDate.of(2026, 10, 18)
        val nextMonday = sunday.plusDays(1)
        val earlierWeek = today.minusWeeks(1) // so week N is not the first and offers every day
        val weekdays = (0L..4L).map { today.plusDays(it) }

        // Three weekdays closed before keep the week short of its rhythm; the Sunday ritual stands in
        // for the missing one and completes it. Which weekdays share a line of the week's figure
        // depends on the figure, so look for three that do.
        val before = weekdays.flatMap { first -> weekdays.map { second -> setOf(first, second) } }
            .flatMap { pair -> weekdays.map { pair + it } }
            .filter { it.size == 3 }
            .map { it + earlierWeek }
            .first { before ->
                val after = before + sunday
                SkyPolicy.isLit(today, after, sky(after, nextMonday).rhythmWeeks) &&
                    !SkyPolicy.isLit(today, before, sky(before, sunday).rhythmWeeks)
            }
        val closing = sunday

        val logs = FakeRitualLogRepository(loggedDay = closing)
        val result = useCase(logs, progress(before, before + closing, beforeOn = sunday, afterOn = nextMonday))(
            "A", MoodTag.CALM, "Q?", moodWasChosen = true
        )

        assertNotNull(result.litConstellationNumber)
        assertEquals(SkyPolicy.constellationNumber(today), result.litConstellationNumber)
    }
}
