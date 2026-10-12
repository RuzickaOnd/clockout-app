package org.clockout

import org.clockout.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.domain.model.SoundImportError
import org.clockout.domain.model.SoundImportResult
import org.clockout.presentation.settings.SettingsViewModel
import org.clockout.testing.ViewModelScopes
import org.clockout.testing.FakeReminderScheduler
import org.clockout.testing.FakeSoundFileStore
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsReminderSchedulingTest {

    @get:Rule val folder = TemporaryFolder()
    private val viewModels = ViewModelScopes()
    private val dispatcher = UnconfinedTestDispatcher()
    private val scheduler = FakeReminderScheduler()
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

    private fun viewModel(settings: SettingsRepository = container.settings) = viewModels.track(SettingsViewModel(
        settings = settings,
        sounds = container.sounds,
        logs = container.ritualLogs,
        audioPlayer = FakeSoundPlayer(),
        reminderScheduler = scheduler,
        soundFiles = FakeSoundFileStore { SoundImportResult.Failure(SoundImportError.READ_FAILED) }
    ))

    /** The setters write to DataStore on its own dispatcher first, so the scheduler call lands later. */
    private suspend fun awaitScheduler(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    @Test
    fun `switching reminders on schedules them at the stored time`() = runTest(dispatcher) {
        container.settings.setNotificationTime(18, 5)
        val vm = viewModel()
        vm.state.first { it.notificationHour == 18 && it.notificationMinute == 5 }

        vm.setNotificationEnabled(true)

        awaitScheduler { scheduler.scheduled.isNotEmpty() }
        assertEquals(listOf(18 to 5), scheduler.scheduled)
    }

    @Test
    fun `switching reminders on before the settings have loaded still uses the stored time`() = runTest(dispatcher) {
        container.settings.setNotificationTime(19, 45)
        // One of the values the screen combines never arrives, so its state keeps the 17:30
        // default for as long as the test runs - the window a quick tap can fall into.
        val stillLoading = object : SettingsRepository by container.settings {
            override fun getCustomNotificationTime(): Flow<Pair<Int, Int>?> = flow { awaitCancellation() }
        }
        val vm = viewModel(settings = stillLoading)
        assertEquals(17 to 30, vm.state.value.notificationHour to vm.state.value.notificationMinute)

        vm.setNotificationEnabled(true)

        awaitScheduler { scheduler.scheduled.isNotEmpty() }
        assertEquals(listOf(19 to 45), scheduler.scheduled)
    }

    @Test
    fun `switching reminders off cancels the alarm`() = runTest(dispatcher) {
        viewModel().setNotificationEnabled(false)

        awaitScheduler { scheduler.cancelCount > 0 }
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `a new time while reminders are off schedules nothing`() = runTest(dispatcher) {
        container.settings.setNotificationEnabled(false)
        val vm = viewModel()
        vm.state.first { !it.isNotificationEnabled }

        vm.setNotificationTime(19, 0)

        // Both launches queue behind the same DataStore write, so once the cancel has landed the
        // time setter has already passed its isNotificationEnabled check.
        vm.setNotificationEnabled(false)
        awaitScheduler { scheduler.cancelCount > 0 }
        assertTrue(scheduler.scheduled.isEmpty())
    }
}
