package org.clockout.presentation.settings

import org.clockout.data.local.SoundImportError
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.ReminderTimePresets
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.model.WorkdayCloseWindow

data class SettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedLanguageOption: LanguageOption = LanguageOption.SYSTEM,
    val notificationHour: Int = ReminderTimePresets.DEFAULT_HOUR,
    val notificationMinute: Int = ReminderTimePresets.DEFAULT_MINUTE,
    val isNotificationEnabled: Boolean = false,
    /** The last time picked by hand, as hour and minute; null until the user has picked one. */
    val customNotificationTime: Pair<Int, Int>? = null,
    val includeWeekends: Boolean = false,
    val isTestModeEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val soundChoice: SoundChoice = SoundChoice.Default,
    val breathingStyle: BreathingStyle = BreathingStyle.CLASSIC_CIRCLE,
    val customSounds: List<CustomSound> = emptyList(),
    val isImportingSound: Boolean = false,
    val soundImportError: SoundImportError? = null,
    val isPreviewPlaying: Boolean = false,
    val telemetryConsent: TelemetryConsent = TelemetryConsent.UNKNOWN
) {
    val formattedTime: String
        get() = formatTime(notificationHour, notificationMinute)

    /** True when turning "Ritual available anytime" off would leave the reminder outside the window. */
    val isTimeOutsideRitualWindow: Boolean
        get() = !WorkdayCloseWindow.contains(notificationHour)

    /** The custom chip owns every time that no preset chip does. */
    val isCustomTimeSelected: Boolean
        get() = !ReminderTimePresets.contains(notificationHour, notificationMinute)

    /**
     * What the custom chip shows. Falls back to the active time when that is not a preset but was
     * never stored as custom, so no selected time is ever left without a chip to show it.
     */
    val displayedCustomTime: Pair<Int, Int>?
        get() = if (isCustomTimeSelected) notificationHour to notificationMinute else customNotificationTime

    companion object {
        fun formatTime(hour: Int, minute: Int): String = String.format("%02d:%02d", hour, minute)
    }
}
