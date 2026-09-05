package com.example.presentation.settings

import com.example.data.local.SoundImportError
import com.example.domain.model.AppLanguage
import com.example.domain.model.CustomSound
import com.example.domain.model.LanguageOption
import com.example.domain.model.SoundChoice

data class SettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedLanguageOption: LanguageOption = LanguageOption.SYSTEM,
    val notificationHour: Int = 17,
    val notificationMinute: Int = 30,
    val isNotificationEnabled: Boolean = false,
    val isTestModeEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val soundChoice: SoundChoice = SoundChoice.Default,
    val customSounds: List<CustomSound> = emptyList(),
    val isImportingSound: Boolean = false,
    val soundImportError: SoundImportError? = null,
    val isPreviewPlaying: Boolean = false
) {
    val formattedTime: String
        get() = String.format("%02d:%02d", notificationHour, notificationMinute)
}
