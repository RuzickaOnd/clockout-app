package org.clockout.presentation.settings

import org.clockout.data.local.SoundImportError
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.SoundChoice

data class SettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedLanguageOption: LanguageOption = LanguageOption.SYSTEM,
    val notificationHour: Int = 17,
    val notificationMinute: Int = 30,
    val isNotificationEnabled: Boolean = false,
    val includeWeekends: Boolean = false,
    val isTestModeEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val soundChoice: SoundChoice = SoundChoice.Default,
    val breathingStyle: BreathingStyle = BreathingStyle.CLASSIC_CIRCLE,
    val customSounds: List<CustomSound> = emptyList(),
    val isImportingSound: Boolean = false,
    val soundImportError: SoundImportError? = null,
    val isPreviewPlaying: Boolean = false
) {
    val formattedTime: String
        get() = String.format("%02d:%02d", notificationHour, notificationMinute)
}
