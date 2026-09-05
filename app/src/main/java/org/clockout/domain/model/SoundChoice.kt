package org.clockout.domain.model

/**
 * What the user selected as the ambient sound: either one of the synthesized
 * built-in styles, or an audio file they uploaded themselves.
 */
sealed interface SoundChoice {

    val id: String

    fun label(language: AppLanguage): String

    fun description(language: AppLanguage): String

    data class BuiltIn(val style: MeditationSoundStyle) : SoundChoice {
        override val id: String get() = style.id
        override fun label(language: AppLanguage): String = style.label(language)
        override fun description(language: AppLanguage): String = style.description(language)
    }

    data class Custom(val sound: CustomSound) : SoundChoice {
        override val id: String get() = sound.id
        override fun label(language: AppLanguage): String = sound.displayName
        override fun description(language: AppLanguage): String = when (language) {
            AppLanguage.CZECH ->
                "Vlastní nahrávka (${sound.formattedDuration}) přehrávaná ve smyčce s jemným náběhem hlasitosti"
            AppLanguage.ENGLISH ->
                "Your own recording (${sound.formattedDuration}) played in a gentle looping fade"
        }
    }

    companion object {
        val Default: SoundChoice = BuiltIn(MeditationSoundStyle.TIBETAN_BOWL)

        /**
         * Resolves a persisted id back into a choice. A custom id whose file/metadata
         * is gone (deleted, restored backup) falls back to the default built-in style.
         */
        fun fromId(id: String?, customSounds: List<CustomSound>): SoundChoice {
            if (CustomSound.isCustomId(id)) {
                val match = customSounds.firstOrNull { it.id == id } ?: return Default
                return Custom(match)
            }
            return BuiltIn(MeditationSoundStyle.fromId(id))
        }
    }
}
