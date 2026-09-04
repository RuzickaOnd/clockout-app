package com.example.data.repository

import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.domain.model.MeditationSoundStyle
import com.example.domain.model.MoodTag
import com.example.domain.model.RitualLogEntry
import com.example.domain.model.RitualQuestion
import com.example.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow

interface RitualRepository {
    fun getTodayQuestion(): RitualQuestion
    fun getLogs(): Flow<List<RitualLogEntry>>
    fun getStreakInfo(): Flow<RitualStreakInfo>
    suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String)
    fun isWithinWorkdayCloseWindow(): Boolean
    fun getTestMode(): Flow<Boolean>
    suspend fun setTestMode(enabled: Boolean)
    fun getNotificationTime(): Flow<Pair<Int, Int>>
    suspend fun setNotificationTime(hour: Int, minute: Int)
    fun getNotificationEnabled(): Flow<Boolean>
    suspend fun setNotificationEnabled(enabled: Boolean)
    fun getSoundEnabled(): Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
    fun getSoundStyle(): Flow<MeditationSoundStyle>
    suspend fun setSoundStyle(style: MeditationSoundStyle)
    fun getLanguage(): Flow<AppLanguage>
    fun getLanguageOption(): Flow<LanguageOption>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setLanguageOption(option: LanguageOption)
    suspend fun clearLogs()
    fun getTodayDateString(): String
}
