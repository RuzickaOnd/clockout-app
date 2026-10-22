package org.clockout.domain.repository

import kotlinx.coroutines.flow.Flow
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.TelemetryConsent

interface SettingsRepository {
    fun getIncludeWeekends(): Flow<Boolean>
    suspend fun setIncludeWeekends(enabled: Boolean)
    fun getTestMode(): Flow<Boolean>
    suspend fun setTestMode(enabled: Boolean)
    fun getNotificationTime(): Flow<Pair<Int, Int>>
    suspend fun setNotificationTime(hour: Int, minute: Int)
    fun getCustomNotificationTime(): Flow<Pair<Int, Int>?>
    /** Sets the active time from the picker and remembers it as the custom one, in one write. */
    suspend fun setCustomNotificationTime(hour: Int, minute: Int)
    fun getNotificationEnabled(): Flow<Boolean>
    suspend fun setNotificationEnabled(enabled: Boolean)
    fun getLanguage(): Flow<AppLanguage>
    fun getLanguageOption(): Flow<LanguageOption>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setLanguageOption(option: LanguageOption)
    fun getBreathingStyle(): Flow<BreathingStyle>
    suspend fun setBreathingStyle(style: BreathingStyle)
    fun getTelemetryConsent(): Flow<TelemetryConsent>
    suspend fun setTelemetryConsent(consent: TelemetryConsent)
}
