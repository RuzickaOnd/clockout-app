package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.SettingsRepository
import org.clockout.presentation.settings.SettingsState
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
 * A hand-picked workday end time is remembered apart from the active one, so the custom chip
 * still offers it after the user has switched to a preset.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomReminderTimeTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: AppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.settings
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `no custom time exists until the user picks one`() = runTest {
        assertNull(repository.getCustomNotificationTime().first())
    }

    @Test
    fun `a picked time becomes both the active and the custom time`() = runTest {
        repository.setCustomNotificationTime(20, 15)

        assertEquals(20 to 15, repository.getNotificationTime().first())
        assertEquals(20 to 15, repository.getCustomNotificationTime().first())
    }

    @Test
    fun `choosing a preset afterwards keeps the custom time`() = runTest {
        repository.setCustomNotificationTime(20, 15)

        repository.setNotificationTime(17, 0)

        assertEquals(17 to 0, repository.getNotificationTime().first())
        assertEquals(20 to 15, repository.getCustomNotificationTime().first())
    }

    @Test
    fun `picking a preset's time by hand leaves the earlier custom time in place`() = runTest {
        repository.setCustomNotificationTime(20, 15)

        repository.setCustomNotificationTime(18, 0)

        assertEquals(18 to 0, repository.getNotificationTime().first())
        assertEquals(20 to 15, repository.getCustomNotificationTime().first())
    }

    @Test
    fun `the custom chip is selected only for a time no preset covers`() {
        val custom = SettingsState(notificationHour = 20, notificationMinute = 15, customNotificationTime = 20 to 15)
        assertTrue(custom.isCustomTimeSelected)

        val preset = custom.copy(notificationHour = 16, notificationMinute = 30)
        assertFalse(preset.isCustomTimeSelected)
        assertEquals(20 to 15, preset.displayedCustomTime)
    }

    @Test
    fun `a non-preset active time is shown even if it was never stored as custom`() {
        val state = SettingsState(notificationHour = 9, notificationMinute = 45, customNotificationTime = null)

        assertEquals(9 to 45, state.displayedCustomTime)
    }
}
