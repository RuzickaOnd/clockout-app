package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.FakeSoundPlayer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * Rotating a tablet or unfolding a foldable rebuilds the composition, and the ritual screen then
 * hands its session number back in. The same number has to mean "carry on where we were"; only a
 * number the user produced by starting the ritual again may wipe what they had typed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualSessionSurvivalTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var container: AppContainer
    private val database get() = container.database

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = testAppContainer(preferencesFolder, context = context)
    }

    @After
    fun tearDown() {
        container.database.close()
    }

    private fun viewModel(): RitualViewModel {
        // A new graph over the same storage: the view model is rebuilt the way a new process would.
        val repository = AppContainer(
            context = context,
            preferences = container.preferences,
            database = container.database,
            todayFlow = flowOf(LocalDate.now())
        ).ritualRepository
        return RitualViewModel(
            audioPlayer = FakeSoundPlayer(),
            getTodayRitualUseCase = GetTodayRitualUseCase(repository),
            logMoodUseCase = LogMoodUseCase(repository),
            shouldRequestReviewUseCase = ShouldRequestReviewUseCase(repository),
            ritualRepository = repository
        )
    }

    @Test
    fun `handing back the same session number keeps the answer already given`() {
        val viewModel = viewModel()
        viewModel.startSession(1)
        viewModel.onIntent(RitualIntent.AnswerSelected("Finished a big task"))

        // What a rotation or a fold looks like from here: the same run, announced again.
        viewModel.startSession(1)

        assertEquals("Finished a big task", viewModel.state.value.selectedAnswer)
    }

    @Test
    fun `a new session number starts the ritual over`() {
        val viewModel = viewModel()
        viewModel.startSession(1)
        viewModel.onIntent(RitualIntent.AnswerSelected("Finished a big task"))

        viewModel.startSession(2)

        assertNull(viewModel.state.value.selectedAnswer)
        assertEquals("", viewModel.state.value.customAnswerText)
    }
}
