package org.clockout.data.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.data.local.RitualPreferences
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.repository.SettingsRepository

class SettingsRepositoryImpl(private val preferences: RitualPreferences) : SettingsRepository {

    override fun getIncludeWeekends(): Flow<Boolean> = preferences.includeWeekendsFlow

    override suspend fun setIncludeWeekends(enabled: Boolean) = preferences.setIncludeWeekends(enabled)

    override fun getTestMode(): Flow<Boolean> = preferences.testModeFlow

    override suspend fun setTestMode(enabled: Boolean) = preferences.setTestMode(enabled)

    override fun getNotificationTime(): Flow<Pair<Int, Int>> = preferences.notificationTimeFlow

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        preferences.setNotificationTime(hour, minute)
    }

    override fun getCustomNotificationTime(): Flow<Pair<Int, Int>?> = preferences.customNotificationTimeFlow

    override suspend fun setCustomNotificationTime(hour: Int, minute: Int) =
        preferences.setCustomNotificationTime(hour, minute)

    override fun getNotificationEnabled(): Flow<Boolean> = preferences.notificationEnabledFlow

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        preferences.setNotificationEnabled(enabled)
    }

    override fun getLanguage(): Flow<AppLanguage> = preferences.languageFlow

    override fun getLanguageOption(): Flow<LanguageOption> = preferences.languageOptionFlow

    override suspend fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)

    override suspend fun setLanguageOption(option: LanguageOption) = preferences.setLanguageOption(option)

    override fun getBreathingStyle(): Flow<BreathingStyle> = preferences.breathingStyleFlow

    override suspend fun setBreathingStyle(style: BreathingStyle) = preferences.setBreathingStyle(style)

    override fun getTelemetryConsent(): Flow<TelemetryConsent> = preferences.telemetryConsentFlow

    override suspend fun setTelemetryConsent(consent: TelemetryConsent) {
        preferences.setTelemetryConsent(consent)
    }
}
