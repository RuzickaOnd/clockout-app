package org.clockout.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.clockout.data.local.RitualPreferences
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.repository.SoundRepository

class SoundRepositoryImpl(
    private val preferences: RitualPreferences,
    /**
     * Whether the audio file behind a custom sound is still on disk. Injected because the check
     * needs a Context the repository has no other use for, and because it lets a test reproduce
     * a restored cloud backup without touching the file system. Defaults to "everything is
     * there", which is what a test that does not care about sounds wants.
     */
    private val soundFileExists: (fileName: String) -> Boolean = { true }
) : SoundRepository {

    override fun getSoundEnabled(): Flow<Boolean> = preferences.soundEnabledFlow

    override suspend fun setSoundEnabled(enabled: Boolean) = preferences.setSoundEnabled(enabled)

    override fun getSoundChoice(): Flow<SoundChoice> = combine(
        preferences.selectedSoundIdFlow,
        customSoundsFlow
    ) { selectedId, customSounds ->
        SoundChoice.fromId(selectedId, customSounds)
    }

    /** Metadata from DataStore, stamped with whether the audio file it names is still there. */
    private val customSoundsFlow: Flow<List<CustomSound>> = preferences.customSoundsFlow.map { sounds ->
        sounds.map { it.copy(isAvailable = soundFileExists(it.fileName)) }
    }

    override fun getCustomSounds(): Flow<List<CustomSound>> = customSoundsFlow

    override suspend fun addCustomSound(sound: CustomSound) = preferences.addCustomSound(sound)

    override suspend fun removeCustomSound(id: String) = preferences.removeCustomSound(id)

    override suspend fun setSoundChoice(choice: SoundChoice) = preferences.setSelectedSoundId(choice.id)
}
