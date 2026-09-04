package com.example.presentation.settings

import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.domain.model.MeditationSoundStyle

data class SettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedLanguageOption: LanguageOption = LanguageOption.SYSTEM,
    val notificationHour: Int = 17,
    val notificationMinute: Int = 30,
    val isNotificationEnabled: Boolean = false,
    val isTestModeEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val soundStyle: MeditationSoundStyle = MeditationSoundStyle.TIBETAN_BOWL,
    val isPreviewPlaying: Boolean = false
) {
    val formattedTime: String
        get() = String.format("%02d:%02d", notificationHour, notificationMinute)
}
