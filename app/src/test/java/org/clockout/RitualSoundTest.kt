package org.clockout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.ViewModelScopes
import org.clockout.testing.FakeSoundPlayer
import org.clockout.testing.TestAppContainer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualSoundTest {

    @get:Rule val folder = TemporaryFolder()
    private val viewModels = ViewModelScopes()
    private val dispatcher = UnconfinedTestDispatcher()
    private val player = FakeSoundPlayer()
    private lateinit var container: TestAppContainer

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(folder)
    }

    @After fun tearDown() {
        viewModels.cancelAll()
        container.database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(): RitualViewModel {
        return viewModels.track(RitualViewModel(
            settings = container.settings,
            sounds = container.sounds,
            completeRitual = container.completeRitual,
            audioPlayer = player
        )).also { it.startSession(1) }
    }

    @Test
    fun `breathing starts the ambient sound unless muted`() = runTest(dispatcher) {
        container.sounds.setSoundEnabled(true)
        val vm = viewModel()
        vm.onIntent(RitualIntent.ProceedToBreathing)

        assertNotNull(player.ambient)
        vm.stopSession()
    }

    @Test
    fun `leaving the ritual stops the sound`() = runTest(dispatcher) {
        container.sounds.setSoundEnabled(true)
        val vm = viewModel()
        vm.onIntent(RitualIntent.ProceedToBreathing)

        vm.stopSession()

        assertNull(player.ambient)
    }

    @Test
    fun `a muted ritual stays silent`() = runTest(dispatcher) {
        container.sounds.setSoundEnabled(false)
        val vm = viewModel()
        vm.state.first { it.isSoundMuted }
        vm.onIntent(RitualIntent.ProceedToBreathing)

        assertNull(player.ambient)
        vm.stopSession()
    }
}
