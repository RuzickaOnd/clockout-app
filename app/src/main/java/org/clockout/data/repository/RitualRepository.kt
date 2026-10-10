package org.clockout.data.repository

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow

interface RitualRepository {
    fun getTodayQuestion(): RitualQuestion
    fun getLogs(): Flow<List<RitualLogEntry>>
    fun getStreakInfo(): Flow<RitualStreakInfo>
    /** The path's stage and weekly rhythm; survives [clearLogs], which only empties the history. */
    fun getPathProgress(): Flow<PathProgress>
    /** The night sky: practice days, the entries still in the history, and the rhythm per week. */
    fun getSky(): Flow<SkyData>
    /** Records a finished ritual: the log entry itself, and the lifetime completion count. */
    suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String)
    suspend fun getCompletedRitualCount(): Int
    suspend fun isReviewPromptShown(): Boolean
    suspend fun markReviewPromptShown()
    fun isWithinWorkdayCloseWindow(): Boolean
    fun getIncludeWeekends(): Flow<Boolean>
    suspend fun setIncludeWeekends(enabled: Boolean)
    fun getTestMode(): Flow<Boolean>
    suspend fun setTestMode(enabled: Boolean)
    fun getNotificationTime(): Flow<Pair<Int, Int>>
    suspend fun setNotificationTime(hour: Int, minute: Int)
    fun getCustomNotificationTime(): Flow<Pair<Int, Int>?>
    /** Sets the active time from the picker; see [org.clockout.data.local.RitualPreferences.setCustomNotificationTime]. */
    suspend fun setCustomNotificationTime(hour: Int, minute: Int)
    fun getNotificationEnabled(): Flow<Boolean>
    suspend fun setNotificationEnabled(enabled: Boolean)
    fun getSoundEnabled(): Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
    fun getSoundChoice(): Flow<SoundChoice>
    suspend fun setSoundChoice(choice: SoundChoice)
    fun getCustomSounds(): Flow<List<CustomSound>>
    suspend fun addCustomSound(sound: CustomSound)
    suspend fun removeCustomSound(id: String)
    fun getLanguage(): Flow<AppLanguage>
    fun getLanguageOption(): Flow<LanguageOption>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setLanguageOption(option: LanguageOption)
    fun getBreathingStyle(): Flow<BreathingStyle>
    suspend fun setBreathingStyle(style: BreathingStyle)
    fun getTelemetryConsent(): Flow<TelemetryConsent>
    suspend fun setTelemetryConsent(consent: TelemetryConsent)
    suspend fun clearLogs()
    fun getTodayDateString(): String
}
