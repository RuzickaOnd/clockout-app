package org.clockout

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
import org.clockout.testing.FakeReminderScheduler
import org.clockout.testing.FakeSoundFileStore
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsReminderSchedulingTest {

    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = UnconfinedTestDispatcher()
    private val scheduler = FakeReminderScheduler()
    private lateinit var container: AppContainer

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(folder)
    }

    @After fun tearDown() {
        container.database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        repository = container.ritualRepository,
        audioPlayer = FakeSoundPlayer(),
        reminderScheduler = scheduler,
        soundFiles = FakeSoundFileStore { SoundImportResult.Failure(SoundImportError.READ_FAILED) }
    )

    /** The setters write to DataStore on its own dispatcher first, so the scheduler call lands later. */
    private suspend fun awaitScheduler(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    @Test
    fun `switching reminders on schedules them at the stored time`() = runTest(dispatcher) {
        container.ritualRepository.setNotificationTime(18, 5)
        val vm = viewModel()
        vm.state.first { it.notificationHour == 18 && it.notificationMinute == 5 }

        vm.setNotificationEnabled(true)

        awaitScheduler { scheduler.scheduled.isNotEmpty() }
        assertEquals(listOf(18 to 5), scheduler.scheduled)
    }

    @Test
    fun `switching reminders off cancels the alarm`() = runTest(dispatcher) {
        viewModel().setNotificationEnabled(false)

        awaitScheduler { scheduler.cancelCount > 0 }
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `a new time while reminders are off schedules nothing`() = runTest(dispatcher) {
        container.ritualRepository.setNotificationEnabled(false)
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
