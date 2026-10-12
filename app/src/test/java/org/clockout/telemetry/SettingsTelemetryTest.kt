package org.clockout.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.model.TelemetryConsent
import org.clockout.presentation.settings.SettingsViewModel
import org.clockout.testing.TestAppContainer
import org.clockout.testing.testAppContainer
import org.clockout.domain.model.SoundImportError
import org.clockout.domain.model.SoundImportResult
import org.clockout.testing.FakeReminderScheduler
import org.clockout.testing.FakeSoundFileStore
import org.clockout.testing.FakeSoundPlayer
import org.junit.After
import org.junit.Assert.assertEquals
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

/** The Settings switch is the only way back from a "no", so it has to write both directions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsTelemetryTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: TestAppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        // viewModelScope posts to Dispatchers.Main; without this the write has not happened by the
        // time the assertion runs. Same arrangement CustomSoundReplacementTest uses.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.settings
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the switch grants and withdraws`() = runTest {
        val model = SettingsViewModel(
            settings = repository,
            sounds = container.sounds,
            logs = container.ritualLogs,
            audioPlayer = FakeSoundPlayer(),
            reminderScheduler = FakeReminderScheduler(),
            soundFiles = FakeSoundFileStore { SoundImportResult.Failure(SoundImportError.READ_FAILED) }
        )

        // Awaited rather than read straight back: the setter writes from viewModelScope and
        // DataStore completes the edit on its own dispatcher, so an immediate read races it. A
        // value that never arrives fails the test by timing out.
        model.setTelemetryConsent(true)
        assertEquals(
            TelemetryConsent.GRANTED,
            model.state.first { it.telemetryConsent == TelemetryConsent.GRANTED }.telemetryConsent
        )
        assertEquals(TelemetryConsent.GRANTED, repository.getTelemetryConsent().first())

        model.setTelemetryConsent(false)
        assertEquals(
            TelemetryConsent.DENIED,
            model.state.first { it.telemetryConsent == TelemetryConsent.DENIED }.telemetryConsent
        )
        assertEquals(TelemetryConsent.DENIED, repository.getTelemetryConsent().first())
    }
}
