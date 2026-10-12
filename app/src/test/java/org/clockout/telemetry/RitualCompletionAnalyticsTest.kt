package org.clockout.telemetry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.usecase.CompleteRitualUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.FakeSoundPlayer
import org.clockout.testing.TestAppContainer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** The week's closed-day count exists only for the completion event, so a failure reading it costs that event alone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualCompletionAnalyticsTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var container: TestAppContainer
    private val tracker = RecordingAnalyticsTracker()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(preferencesFolder)
    }

    @After
    fun tearDown() {
        container.database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(completeRitual: CompleteRitualUseCase = container.completeRitual) = RitualViewModel(
        logs = container.ritualLogs,
        settings = container.settings,
        sounds = container.sounds,
        completeRitual = completeRitual,
        audioPlayer = FakeSoundPlayer(),
        analyticsTracker = tracker
    )

    @Test
    fun `the completion event counts today among the week's closed days`() = runTest(dispatcher) {
        val model = viewModel()
        model.onIntent(RitualIntent.MoodSelected(MoodTag.CALM))
        model.state.first { it.isCompletedAndSaved }

        val completed = tracker.events.filterIsInstance<AnalyticsEvent.RitualCompleted>().single()
        assertEquals(1, completed.daysClosedThisWeek)
    }

    @Test
    fun `a failing week read still completes the ritual and drops only the event`() = runTest(dispatcher) {
        val brokenWeek = object : RitualLogRepository by container.ritualLogs {
            override fun getStreakInfo(): Flow<RitualStreakInfo> = flow { throw IOException("disk") }
        }
        val completeRitual = CompleteRitualUseCase(
            brokenWeek, container.progress, ShouldRequestReviewUseCase(brokenWeek)
        )
        val model = viewModel(completeRitual = completeRitual)

        model.onIntent(RitualIntent.MoodSelected(MoodTag.CALM))
        val state = model.state.first { it.isCompletedAndSaved }

        assertTrue(state.isCompletedAndSaved)
        assertEquals(1, container.ritualLogs.getLogs().first().size)
        assertTrue(tracker.events.filterIsInstance<AnalyticsEvent.RitualCompleted>().isEmpty())
    }
}
