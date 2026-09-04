package com.example.data.repository

import com.example.data.local.QuestionsData
import com.example.data.local.RitualPreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.domain.model.MeditationSoundStyle
import com.example.domain.model.DayStatus
import com.example.domain.model.MoodTag
import com.example.domain.model.RitualLogEntry
import com.example.domain.model.RitualQuestion
import com.example.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RitualRepositoryImpl(
    private val preferences: RitualPreferences
) : RitualRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getTodayQuestion(): RitualQuestion {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return QuestionsData.getTodayQuestion(dayOfYear)
    }

    override fun getLogs(): Flow<List<RitualLogEntry>> {
        return preferences.logsFlow
    }

    override fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    override fun getStreakInfo(): Flow<RitualStreakInfo> {
        return preferences.logsFlow.combine(preferences.testModeFlow) { logs, _ ->
            calculateStreakInfo(logs)
        }
    }

    private fun calculateStreakInfo(logs: List<RitualLogEntry>): RitualStreakInfo {
        val todayStr = getTodayDateString()
        val logsMap = logs.associateBy { it.date }
        val todayLog = logsMap[todayStr]

        // Build 7 days of current week (Monday to Sunday)
        val calendar = Calendar.getInstance()
        // Set first day of week to Monday
        calendar.firstDayOfWeek = Calendar.MONDAY
        // Find Monday of this week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val dayNamesCs = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
        val dayNamesEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weekDays = mutableListOf<DayStatus>()
        var completedCountThisWeek = 0

        for (i in 0..6) {
            val dateStr = dateFormat.format(calendar.time)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val isToday = dateStr == todayStr
            val logForDay = logsMap[dateStr]
            val isCompleted = logForDay != null
            if (isCompleted && i < 5) { // count workdays
                completedCountThisWeek++
            }

            weekDays.add(
                DayStatus(
                    dayNameCs = dayNamesCs[i],
                    dayNameEn = dayNamesEn[i],
                    dayOfMonth = dayOfMonth,
                    dateString = dateStr,
                    isToday = isToday,
                    isCompleted = isCompleted,
                    moodTag = logForDay?.moodTag
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return RitualStreakInfo(
            completedThisWeekCount = completedCountThisWeek,
            targetWorkDays = 5,
            isTodayCompleted = todayLog != null,
            todayLog = todayLog,
            weekDays = weekDays
        )
    }

    override suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String) {
        val entry = RitualLogEntry(
            date = getTodayDateString(),
            questionText = questionText,
            selectedAnswer = selectedAnswer,
            moodTag = mood,
            timestamp = System.currentTimeMillis()
        )
        preferences.saveLogEntry(entry)
    }

    override fun isWithinWorkdayCloseWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // Typically 16:00 to 21:59
        return hour in 16..21
    }

    override fun getTestMode(): Flow<Boolean> = preferences.testModeFlow

    override suspend fun setTestMode(enabled: Boolean) = preferences.setTestMode(enabled)

    override fun getNotificationTime(): Flow<Pair<Int, Int>> {
        return preferences.notificationHourFlow.combine(preferences.notificationMinuteFlow) { h, m ->
            Pair(h, m)
        }
    }

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        preferences.setNotificationTime(hour, minute)
    }

    override fun getNotificationEnabled(): Flow<Boolean> = preferences.notificationEnabledFlow

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        preferences.setNotificationEnabled(enabled)
    }

    override fun getSoundEnabled(): Flow<Boolean> = preferences.soundEnabledFlow

    override suspend fun setSoundEnabled(enabled: Boolean) = preferences.setSoundEnabled(enabled)

    override fun getSoundStyle(): Flow<MeditationSoundStyle> = preferences.soundStyleFlow

    override suspend fun setSoundStyle(style: MeditationSoundStyle) = preferences.setSoundStyle(style)

    override fun getLanguage(): Flow<AppLanguage> = preferences.languageFlow

    override fun getLanguageOption(): Flow<LanguageOption> = preferences.languageOptionFlow

    override suspend fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)

    override suspend fun setLanguageOption(option: LanguageOption) = preferences.setLanguageOption(option)

    override suspend fun clearLogs() = preferences.clearLogs()
}
