package org.clockout.domain.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice

interface SoundRepository {
    fun getSoundEnabled(): Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
    fun getSoundChoice(): Flow<SoundChoice>
    suspend fun setSoundChoice(choice: SoundChoice)
    fun getCustomSounds(): Flow<List<CustomSound>>
    suspend fun addCustomSound(sound: CustomSound)
    suspend fun removeCustomSound(id: String)
}
