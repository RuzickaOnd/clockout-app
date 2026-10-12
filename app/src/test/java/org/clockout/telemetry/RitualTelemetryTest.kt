package org.clockout.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.AppContainer
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.FakeSoundPlayer
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
import kotlinx.coroutines.test.runTest

/**
 * What the ritual reports, and - more importantly - what it does not. The question, the answer and
 * the note must never reach a parameter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualTelemetryTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: AppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualLogRepository
    private lateinit var tracker: RecordingAnalyticsTracker

    private fun viewModel(): RitualViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        return RitualViewModel(
            audioPlayer = FakeSoundPlayer(),
            logs = repository,
            settings = container.settings,
            sounds = container.sounds,
            completeRitual = container.completeRitual,
            analyticsTracker = tracker
        )
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.ritualLogs
        tracker = RecordingAnalyticsTracker()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `starting a run reports it once`() = runTest {
        val model = viewModel()

        model.startSession(1)
        model.startSession(1) // the same run handed back after a rotation

        assertEquals(listOf(AnalyticsEvent.RitualStarted), tracker.events)
    }

    @Test
    fun `each step reports its own completion`() = runTest {
        val model = viewModel()
        model.startSession(1)

        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)

        val steps = tracker.events
            .filterIsInstance<AnalyticsEvent.RitualStepCompleted>()
            .map { it.step }

        assertEquals(listOf("question", "breathing", "closing"), steps)
    }

    @Test
    fun `leaving early reports the step it was left from`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.ProceedToBreathing)

        model.stopSession()

        assertEquals(
            listOf(AnalyticsEvent.RitualAbandoned("breathing")),
            tracker.events.filterIsInstance<AnalyticsEvent.RitualAbandoned>()
        )
    }

    @Test
    fun `finishing does not count as abandoning`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)
        model.onIntent(RitualIntent.MoodSelected(MoodTag.CALM))

        model.stopSession()

        assertTrue(tracker.events.filterIsInstance<AnalyticsEvent.RitualAbandoned>().isEmpty())
    }

    @Test
    fun `no reported parameter carries what the user wrote`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.CustomAnswerChanged("finished the quarterly report"))
        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)
        model.onIntent(RitualIntent.MoodSelected(MoodTag.SATISFIED))

        val everyValue = tracker.events.flatMap { it.params.values }.map { it.toString() }
        assertTrue(everyValue.none { it.contains("quarterly") })
    }
}
