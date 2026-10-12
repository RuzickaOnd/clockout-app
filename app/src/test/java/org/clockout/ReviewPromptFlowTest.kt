package org.clockout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.testing.TestAppContainer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

/**
 * The review request end to end, over a real DataStore counter and a real Room log: the counter
 * has to survive Room collapsing several rituals logged on the same day into a single row.
 *
 * Each test gets its own preferences file, so neither the ritual count nor the "already asked"
 * flag leaks in from a test that ran earlier in the same JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewPromptFlowTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: TestAppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualLogRepository
    private lateinit var shouldRequestReview: ShouldRequestReviewUseCase

    @Before
    fun setUp() {
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.ritualLogs
        shouldRequestReview = ShouldRequestReviewUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** One finished ritual: logged first, exactly as the view model does it. */
    private suspend fun completeRitual(mood: MoodTag): Boolean {
        repository.logRitual(selectedAnswer = null, mood = mood, questionText = "Q")
        return shouldRequestReview(mood)
    }

    @Test
    fun `the first nine rituals never ask, however good the mood`() = runTest {
        repeat(9) { index ->
            assertFalse("ritual ${index + 1} asked too early", completeRitual(MoodTag.SATISFIED))
        }
    }

    @Test
    fun `the tenth ritual in a good mood asks`() = runTest {
        repeat(9) { completeRitual(MoodTag.CALM) }

        assertTrue(completeRitual(MoodTag.SATISFIED))
    }

    @Test
    fun `a bad tenth ritual postpones the question to the next good one`() = runTest {
        repeat(9) { completeRitual(MoodTag.CALM) }

        assertFalse(completeRitual(MoodTag.TENSE))
        assertFalse(completeRitual(MoodTag.TIRED))
        assertTrue(completeRitual(MoodTag.CALM))
    }

    @Test
    fun `the question is never asked twice`() = runTest {
        repeat(9) { completeRitual(MoodTag.CALM) }
        assertTrue(completeRitual(MoodTag.SATISFIED))

        assertFalse(completeRitual(MoodTag.SATISFIED))
        assertFalse(completeRitual(MoodTag.CALM))
    }

    /** Clearing the history is about the logs; it must not hand out a second review request. */
    @Test
    fun `clearing the history does not reopen the question`() = runTest {
        repeat(9) { completeRitual(MoodTag.CALM) }
        assertTrue(completeRitual(MoodTag.SATISFIED))

        repository.clearLogs()

        assertFalse(completeRitual(MoodTag.SATISFIED))
    }
}
