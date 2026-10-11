package org.clockout

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.SoundImportError
import org.clockout.data.local.SoundImportResult
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.presentation.settings.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Uploading a file over a sound whose audio is gone has to land on the existing entry: the id is
 * what preferences point at, so a new id would leave the user's selection dangling a second time,
 * and a full library would refuse the upload outright.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSoundReplacementTest {

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

    private val uploaded = CustomSound(
        id = CustomSound.ID_PREFIX + "freshly-imported",
        displayName = "Forest",
        fileName = "forest.mp3",
        durationMs = 90_000,
        addedAt = 1L
    )

    private fun repository() = RitualRepositoryImpl(
        preferences = preferences,
        ritualLogDao = database.ritualLogDao(),
        practiceDayDao = database.practiceDayDao(),
        // Only the uploaded file is on disk; everything restored from a cloud backup is not.
        soundFileExists = { it == uploaded.fileName }
    )

    /** Stands in for CustomSoundStorage, keeping its documented refusal once the library is full. */
    private fun viewModel() = SettingsViewModel(
        repository = repository(),
        importSound = { _, _, existingCount ->
            if (existingCount >= CustomSoundStorage.MAX_CUSTOM_SOUNDS) {
                SoundImportResult.Failure(SoundImportError.LIMIT_REACHED)
            } else {
                SoundImportResult.Success(uploaded)
            }
        }
    )

    @Before
    fun setUp() {
        // replaceCustomSound is fire-and-forget on viewModelScope; this makes it start eagerly.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        preferences = RitualPreferences(context)
    }

    @After
    fun tearDown() = runTest {
        preferences.customSoundsFlow.first().forEach { preferences.removeCustomSound(it.id) }
        preferences.setSelectedSoundId(SoundChoice.Default.id)
        database.close()
        Dispatchers.resetMain()
    }

    /**
     * The DataStore backed flow re-emits on every write, so collecting until the replacement shows
     * up is what waits for the import coroutine - no arbitrary delay needed.
     */
    private suspend fun awaitSounds(predicate: (List<CustomSound>) -> Boolean): List<CustomSound> =
        repository().getCustomSounds().first(predicate)

    @Test
    fun `the uploaded file takes over the id of the sound it replaces`() = runTest {
        preferences.addCustomSound(restored)

        viewModel().replaceCustomSound(context, Uri.EMPTY, restored)

        val sounds = awaitSounds { it.singleOrNull()?.displayName == "Forest" }
        assertEquals(restored.id, sounds.single().id)
        assertTrue(sounds.single().isAvailable)
    }

    @Test
    fun `a selection pointing at the replaced sound plays again`() = runTest {
        preferences.addCustomSound(restored)
        preferences.setSelectedSoundId(restored.id)

        viewModel().replaceCustomSound(context, Uri.EMPTY, restored)

        awaitSounds { it.singleOrNull()?.displayName == "Forest" }

        val choice = repository().getSoundChoice().first()
        assertEquals(restored.id, choice.id)
        assertEquals("Forest", (choice as SoundChoice.Custom).sound.displayName)
    }

    @Test
    fun `replacing works even when the library is already full`() = runTest {
        repeat(CustomSoundStorage.MAX_CUSTOM_SOUNDS - 1) { index ->
            preferences.addCustomSound(restored.copy(id = CustomSound.ID_PREFIX + index, fileName = "$index.mp3"))
        }
        preferences.addCustomSound(restored)

        val viewModel = viewModel()
        viewModel.replaceCustomSound(context, Uri.EMPTY, restored)

        val sounds = awaitSounds { list -> list.any { it.id == restored.id && it.displayName == "Forest" } }
        assertNull(viewModel.state.value.soundImportError)
        assertEquals(CustomSoundStorage.MAX_CUSTOM_SOUNDS, sounds.size)
    }
}
