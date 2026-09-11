package org.clockout.domain.model

/**
 * What the user selected as the ambient sound: either one of the synthesized
 * built-in styles, or an audio file they uploaded themselves.
 */
sealed interface SoundChoice {

    val id: String

    data class BuiltIn(val style: MeditationSoundStyle) : SoundChoice {
        override val id: String get() = style.id
    }

    data class Custom(val sound: CustomSound) : SoundChoice {
        override val id: String get() = sound.id
    }

    companion object {
        val Default: SoundChoice = BuiltIn(MeditationSoundStyle.TIBETAN_BOWL)

        /**
         * Resolves a persisted id back into a choice. A custom id falls back to the default
         * built-in style when its metadata is gone (the sound was deleted) and equally when the
         * metadata is there but the audio file is not, which is the state a cloud backup
         * restores into.
         */
        fun fromId(id: String?, customSounds: List<CustomSound>): SoundChoice {
            if (CustomSound.isCustomId(id)) {
                val match = customSounds.firstOrNull { it.id == id && it.isAvailable } ?: return Default
                return Custom(match)
            }
            return BuiltIn(MeditationSoundStyle.fromId(id))
        }
    }
}
