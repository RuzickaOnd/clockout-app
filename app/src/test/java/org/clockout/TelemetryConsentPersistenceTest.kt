package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.TelemetryConsent
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Consent is the one preference that must never default to "yes", and the one that must survive a
 * wipe: clearing the history is not a withdrawal of consent, and re-asking after every wipe would
 * be noise. Each test gets its own preferences file so nothing leaks between them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelemetryConsentPersistenceTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: AppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var logs: RitualLogRepository

    @Before
    fun setUp() {
        container = testAppContainer(preferencesFolder)
        database = container.database
        settings = container.settings
        logs = container.ritualLogs
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a fresh install has not been asked yet`() = runTest {
        assertEquals(TelemetryConsent.UNKNOWN, settings.getTelemetryConsent().first())
    }

    @Test
    fun `granting and withdrawing both round-trip`() = runTest {
        settings.setTelemetryConsent(TelemetryConsent.GRANTED)
        assertEquals(TelemetryConsent.GRANTED, settings.getTelemetryConsent().first())

        settings.setTelemetryConsent(TelemetryConsent.DENIED)
        assertEquals(TelemetryConsent.DENIED, settings.getTelemetryConsent().first())
    }

    @Test
    fun `an unrecognised stored value reads as not asked`() {
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromId("yes_please"))
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromId(null))
    }

    @Test
    fun `clearing data and history leaves consent alone`() = runTest {
        settings.setTelemetryConsent(TelemetryConsent.GRANTED)
        logs.logRitual(selectedAnswer = "done", mood = MoodTag.CALM, questionText = "q")

        logs.clearLogs()

        assertEquals(TelemetryConsent.GRANTED, settings.getTelemetryConsent().first())
    }
}
