package org.clockout

import kotlinx.coroutines.test.runTest
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathRhythm
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.SkyData
import org.clockout.domain.usecase.CompleteRitualUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.testing.FakeProgressRepository
import org.clockout.testing.FakeRitualLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CompleteRitualUseCaseTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val question = RitualQuestion(
        id = "q", textEn = "Q?", textCs = "O?", suggestedAnswersEn = listOf("A"), suggestedAnswersCs = listOf("A")
    )

    private fun sky(days: Set<LocalDate>) =
        SkyData(days, emptyMap(), PathRhythm.evaluate(days, today).weeks, today, includeWeekends = false)

    private fun progress(before: Set<LocalDate>, after: Set<LocalDate>, failure: Exception? = null) =
        FakeProgressRepository(
            paths = ArrayDeque(listOf(PathPolicy.progress(before, today), PathPolicy.progress(after, today))),
            skies = ArrayDeque(listOf(sky(before), sky(after))),
            failure = failure
        )

    private fun useCase(logs: FakeRitualLogRepository, progress: FakeProgressRepository) =
        CompleteRitualUseCase(logs, progress, ShouldRequestReviewUseCase(logs), today = { today })

    @Test
    fun `the first closed day reaches the first stage`() = runTest {
        val logs = FakeRitualLogRepository(question)
        val result = useCase(logs, progress(emptySet(), setOf(today)))("A", MoodTag.CALM, "Q?", moodWasChosen = true)

        assertEquals(PathStage.LAPTOP_CLOSED, result.reachedStage)
        assertEquals(1, logs.logged.size)
    }

    @Test
    fun `a broken sky still writes the entry and celebrates nothing`() = runTest {
        val logs = FakeRitualLogRepository(question)
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
        val logs = FakeRitualLogRepository(question, completedCount = 50)
        val result = useCase(logs, progress(emptySet(), setOf(today)))(null, MoodTag.CALM, "Q?", moodWasChosen = false)

        assertFalse(result.shouldRequestReview)
        assertFalse(logs.reviewShown)
    }
}
