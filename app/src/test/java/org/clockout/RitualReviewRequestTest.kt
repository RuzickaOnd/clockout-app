package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.ViewModelScopes
import org.clockout.testing.FakeSoundPlayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** The view model side of the review request: which ways of finishing a ritual earn one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualReviewRequestTest {

    @get:Rule val preferencesFolder = TemporaryFolder()
    private val viewModels = ViewModelScopes()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var container: TestAppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.ritualLogs
    }

    @After
    fun tearDown() {
        viewModels.cancelAll()
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = viewModels.track(RitualViewModel(
        audioPlayer = FakeSoundPlayer(),
        settings = container.settings,
        sounds = container.sounds,
        completeRitual = container.completeRitual
    ))

    /** Nine rituals already behind the user, so the next one is the tenth. */
    private suspend fun nineRitualsAlreadyDone() {
        repeat(9) { repository.logRitual(null, MoodTag.CALM, "Q") }
    }

    @Test
    fun `choosing a good mood on the tenth ritual raises the request`() = runTest(dispatcher) {
        nineRitualsAlreadyDone()
        val viewModel = viewModel()

        viewModel.onIntent(RitualIntent.MoodSelected(MoodTag.SATISFIED))
        viewModel.state.first { it.isCompletedAndSaved }

        assertTrue(viewModel.state.value.shouldRequestReview)
    }

    @Test
    fun `skipping the mood step never raises the request`() = runTest(dispatcher) {
        nineRitualsAlreadyDone()
        val viewModel = viewModel()

        viewModel.onIntent(RitualIntent.SkipMood)
        viewModel.state.first { it.isCompletedAndSaved }

        assertFalse(viewModel.state.value.shouldRequestReview)
    }

    @Test
    fun `handling the request clears the one-shot flag`() = runTest(dispatcher) {
        nineRitualsAlreadyDone()
        val viewModel = viewModel()
        viewModel.onIntent(RitualIntent.MoodSelected(MoodTag.CALM))
        viewModel.state.first { it.shouldRequestReview }

        viewModel.onReviewRequestHandled()

        assertFalse(viewModel.state.value.shouldRequestReview)
    }
}
