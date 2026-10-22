package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.CustomSoundStorage
import org.clockout.domain.model.SoundImportError
import org.clockout.domain.model.SoundImportResult
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.presentation.settings.SettingsViewModel
import org.clockout.testing.ViewModelScopes
import org.clockout.testing.FakeReminderScheduler
import org.clockout.testing.FakeSoundFileStore
import org.clockout.testing.FakeSoundPlayer
import org.clockout.testing.TestAppContainer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

/**
 * Uploading a file over a sound whose audio is gone has to land on the existing entry: the id is
 * what preferences point at, so a new id would leave the user's selection dangling a second time,
 * and a full library would refuse the upload outright.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomSoundReplacementTest {

    @get:Rule val preferencesFolder = TemporaryFolder()
    private val viewModels = ViewModelScopes()

    private lateinit var context: Context
    private lateinit var container: TestAppContainer
    private val preferences get() = container.preferences

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

    private fun repository() = container.sounds

    /** Stands in for CustomSoundStorage, keeping its documented refusal once the library is full. */
    private fun viewModel() = viewModels.track(SettingsViewModel(
        settings = container.settings,
        sounds = container.sounds,
        logs = container.ritualLogs,
        audioPlayer = FakeSoundPlayer(),
        reminderScheduler = FakeReminderScheduler(),
        soundFiles = FakeSoundFileStore { existingCount ->
            if (existingCount >= CustomSoundStorage.MAX_CUSTOM_SOUNDS) {
                SoundImportResult.Failure(SoundImportError.LIMIT_REACHED)
            } else {
                SoundImportResult.Success(uploaded)
            }
        }
    ))

    @Before
    fun setUp() {
        // replaceCustomSound is fire-and-forget on viewModelScope; this makes it start eagerly.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        // Only the uploaded file is on disk; everything restored from a cloud backup is not.
        container = testAppContainer(
            preferencesFolder,
            soundFileExists = { it == uploaded.fileName },
            context = context
        )
    }

    @After
    fun tearDown() {
        viewModels.cancelAll()
        container.database.close()
        Dispatchers.resetMain()
    }

    /**
     * Waits for the view model to finish the replacement, then reads the sounds once.
     *
     * The view model flags the import before its first suspension and clears the flag only after
     * DataStore has acknowledged the write, so once the flag is down the new list is on disk and in
     * DataStore's cache. Collecting DataStore while the write is still in flight is not safe: in
     * DataStore 1.2.1 a collector that starts between the write bumping the version and updating
     * the cache can read the old data stamped with the new version, and then drops the real update
     * as stale - the test waited forever for an emission that never came.
     */
    private suspend fun awaitReplacement(viewModel: SettingsViewModel): List<CustomSound> {
        viewModel.state.first { !it.isImportingSound }
        return repository().getCustomSounds().first()
    }

    @Test
    fun `the uploaded file takes over the id of the sound it replaces`() = runTest {
        preferences.addCustomSound(restored)

        val viewModel = viewModel()
        viewModel.replaceCustomSound("content://test/forest.mp3", restored)

        val sounds = awaitReplacement(viewModel)
        assertEquals("Forest", sounds.single().displayName)
        assertEquals(restored.id, sounds.single().id)
        assertTrue(sounds.single().isAvailable)
    }

    @Test
    fun `a selection pointing at the replaced sound plays again`() = runTest {
        preferences.addCustomSound(restored)
        preferences.setSelectedSoundId(restored.id)

        val viewModel = viewModel()
        viewModel.replaceCustomSound("content://test/forest.mp3", restored)

        awaitReplacement(viewModel)

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
        viewModel.replaceCustomSound("content://test/forest.mp3", restored)

        val sounds = awaitReplacement(viewModel)
        assertNull(viewModel.state.value.soundImportError)
        assertTrue(sounds.any { it.id == restored.id && it.displayName == "Forest" })
        assertEquals(CustomSoundStorage.MAX_CUSTOM_SOUNDS, sounds.size)
    }
}
