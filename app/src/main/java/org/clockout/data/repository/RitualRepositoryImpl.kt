package org.clockout.data.repository

import org.clockout.data.local.QuestionsData
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.RitualLogDao
import org.clockout.data.local.db.toDomain
import org.clockout.data.local.db.toEntity
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RitualRepositoryImpl(
    private val preferences: RitualPreferences,
    private val ritualLogDao: RitualLogDao,
    private val logMigrator: RitualLogMigrator = RitualLogMigrator(preferences, ritualLogDao)
) : RitualRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Ritual logs from the Room table. The legacy DataStore import is awaited before the table is
     * observed, so a collector never sees an empty list that is about to be filled by the import.
     */
    private val logsFlow: Flow<List<RitualLogEntry>> = flow {
        logMigrator.ensureMigrated()
        emitAll(ritualLogDao.observeAll().map { entities -> entities.map { it.toDomain() } })
    }

    override fun getTodayQuestion(): RitualQuestion {
        val calendar = Calendar.getInstance()
        return QuestionsData.getTodayQuestion(
            dayOfYear = calendar.get(Calendar.DAY_OF_YEAR),
            year = calendar.get(Calendar.YEAR)
        )
    }

    override fun getLogs(): Flow<List<RitualLogEntry>> = logsFlow

    override fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    override fun getStreakInfo(): Flow<RitualStreakInfo> {
        return logsFlow.combine(preferences.includeWeekendsFlow) { entries, includeWeekends ->
            calculateStreakInfo(entries, includeWeekends)
        }
    }

    private fun calculateStreakInfo(
        logs: List<RitualLogEntry>,
        includeWeekends: Boolean
    ): RitualStreakInfo {
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

        // Only the days the ritual is offered on make it into the strip, so the summary
        // underneath counts against the same set of days.
        val offeredDayCount = if (includeWeekends) 7 else 5
        val weekDays = mutableListOf<DayStatus>()
        var completedCountThisWeek = 0

        for (i in 0 until offeredDayCount) {
            val dateStr = dateFormat.format(calendar.time)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val isToday = dateStr == todayStr
            val logForDay = logsMap[dateStr]
            val isCompleted = logForDay != null
            if (isCompleted) {
                completedCountThisWeek++
            }

            weekDays.add(
                DayStatus(
                    dayIndex = i,
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
            targetWorkDays = offeredDayCount,
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
        logMigrator.ensureMigrated()
        ritualLogDao.upsert(entry.toEntity())
    }

    override fun isWithinWorkdayCloseWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // Typically 16:00 to 21:59
        return hour in 16..21
    }

    override fun getIncludeWeekends(): Flow<Boolean> = preferences.includeWeekendsFlow

    override suspend fun setIncludeWeekends(enabled: Boolean) = preferences.setIncludeWeekends(enabled)

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

    override fun getSoundChoice(): Flow<SoundChoice> = combine(
        preferences.selectedSoundIdFlow,
        preferences.customSoundsFlow
    ) { selectedId, customSounds ->
        SoundChoice.fromId(selectedId, customSounds)
    }

    override fun getCustomSounds(): Flow<List<CustomSound>> = preferences.customSoundsFlow

    override suspend fun addCustomSound(sound: CustomSound) = preferences.addCustomSound(sound)

    override suspend fun removeCustomSound(id: String) = preferences.removeCustomSound(id)

    override suspend fun setSoundChoice(choice: SoundChoice) = preferences.setSelectedSoundId(choice.id)

    override fun getLanguage(): Flow<AppLanguage> = preferences.languageFlow

    override fun getLanguageOption(): Flow<LanguageOption> = preferences.languageOptionFlow

    override suspend fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)

    override suspend fun setLanguageOption(option: LanguageOption) = preferences.setLanguageOption(option)

    override suspend fun clearLogs() {
        // Migrate first so the legacy blob is dropped too, otherwise it would reappear later.
        logMigrator.ensureMigrated()
        ritualLogDao.deleteAll()
    }
}
