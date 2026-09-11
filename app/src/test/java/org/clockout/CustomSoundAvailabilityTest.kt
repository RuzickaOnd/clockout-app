package org.clockout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A cloud backup restores the custom sound metadata out of DataStore but not the audio files,
 * which only ride along on a device-to-device transfer. These tests pin what the app does in
 * that state: the entry is kept and flagged, never silently selected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSoundAvailabilityTest {

    private lateinit var context: Context
    private lateinit var database: ClockoutDatabase
    private lateinit var preferences: RitualPreferences

    private val restored = CustomSound(
        id = CustomSound.ID_PREFIX + "restored",
        displayName = "Ocean night",
        fileName = "restored.mp3",
        durationMs = 154_000,
        addedAt = 0L
    )

    private val present = CustomSound(
        id = CustomSound.ID_PREFIX + "present",
        displayName = "Forest",
        fileName = "present.mp3",
        durationMs = 90_000,
        addedAt = 0L
    )

    private fun repository(soundFileExists: (String) -> Boolean) = RitualRepositoryImpl(
        preferences = preferences,
        ritualLogDao = database.ritualLogDao(),
        soundFileExists = soundFileExists
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        preferences = RitualPreferences(context)
    }

    @After
    fun tearDown() = runTest {
        preferences.removeCustomSound(restored.id)
        preferences.removeCustomSound(present.id)
        preferences.setSelectedSoundId(SoundChoice.Default.id)
        database.close()
    }

    @Test
    fun `a restored sound whose file is missing is reported unavailable`() = runTest {
        preferences.addCustomSound(restored)

        val sounds = repository(soundFileExists = { false }).getCustomSounds().first()

        assertEquals(1, sounds.size)
        assertFalse(sounds.first().isAvailable)
    }

    @Test
    fun `a sound whose file is still on disk stays available`() = runTest {
        preferences.addCustomSound(present)

        val sounds = repository(soundFileExists = { true }).getCustomSounds().first()

        assertTrue(sounds.first().isAvailable)
    }

    @Test
    fun `availability is decided per file name`() = runTest {
        preferences.addCustomSound(restored)
        preferences.addCustomSound(present)

        val sounds = repository(soundFileExists = { it == present.fileName }).getCustomSounds().first()

        assertTrue(sounds.first { it.id == present.id }.isAvailable)
        assertFalse(sounds.first { it.id == restored.id }.isAvailable)
    }

    @Test
    fun `a selected sound whose file is missing falls back to the default style`() = runTest {
        preferences.addCustomSound(restored)
        preferences.setSelectedSoundId(restored.id)

        val choice = repository(soundFileExists = { false }).getSoundChoice().first()

        assertEquals(SoundChoice.Default, choice)
    }
}
