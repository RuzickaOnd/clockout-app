package org.clockout.data.repository

import org.clockout.domain.model.QuestionCatalog
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.PracticeDayDao
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.local.db.RitualLogDao
import org.clockout.data.local.db.toDomain
import org.clockout.data.local.db.toEntity
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathRhythm
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RitualRepositoryImpl(
    private val preferences: RitualPreferences,
    private val ritualLogDao: RitualLogDao,
    private val practiceDayDao: PracticeDayDao,
    private val logMigrator: RitualLogMigrator = RitualLogMigrator(preferences, ritualLogDao, practiceDayDao),
    /**
     * Whether the audio file behind a custom sound is still on disk. Injected because the check
     * needs a Context the repository has no other use for, and because it lets a test reproduce
     * a restored cloud backup without touching the file system. Defaults to "everything is
     * there", which is what a test that does not care about sounds wants.
     */
    private val soundFileExists: (fileName: String) -> Boolean = { true },
    /**
     * Today's date, emitted again whenever it changes. The path's rhythm and the sky's current
     * month depend on the date, and nothing else would emit when it changes under an open screen.
     * The app passes `Context.todayFlow()`, which follows the system's date broadcasts; without a
     * context (tests) it is just today, read once.
     */
    private val todayFlow: Flow<LocalDate> = flow { emit(LocalDate.now()) }
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
        return QuestionCatalog.getTodayQuestion(
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

    private val practiceDaysFlow: Flow<Set<LocalDate>> = flow {
        logMigrator.ensureMigrated()
        emitAll(practiceDayDao.observeAll().map { dates -> dates.mapNotNull(::parsePracticeDay).toSet() })
    }

    override fun getPathProgress(): Flow<PathProgress> =
        practiceDaysFlow.combine(todayFlow) { days, today -> PathPolicy.progress(days = days, today = today) }
            // The rhythm walks the whole history; keep that off the main thread.
            .flowOn(Dispatchers.Default)

    override fun getSky(): Flow<SkyData> =
        combine(practiceDaysFlow, logsFlow, todayFlow, preferences.includeWeekendsFlow) { days, logs, today, includeWeekends ->
            SkyData(
                days = days,
                logs = logs.mapNotNull { entry -> parsePracticeDay(entry.date)?.let { it to entry } }.toMap(),
                rhythmWeeks = PathRhythm.evaluate(days, today).weeks,
                today = today,
                includeWeekends = includeWeekends
            )
        }
            .flowOn(Dispatchers.Default)

    /**
     * Rows seeded from ritual_logs carry whatever digits the system locale wrote them in - Persian
     * or Arabic-Indic ones included - which ISO parsing rejects, so they are read back as ASCII.
     * A row that still does not parse is skipped rather than taking the home screen down with it.
     */
    private fun parsePracticeDay(date: String): LocalDate? {
        val ascii = date.map { char -> Character.digit(char, 10).takeIf { it >= 0 }?.digitToChar() ?: char }
            .joinToString("")
        return runCatching { LocalDate.parse(ascii) }.getOrNull()
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
        // One reading of the clock for both rows: read twice, a ritual saved at midnight could
        // land in the history on one day and on the path on the next.
        val now = Date()
        val entry = RitualLogEntry(
            date = dateFormat.format(now),
            questionText = questionText,
            selectedAnswer = selectedAnswer,
            moodTag = mood,
            timestamp = now.time
        )
        // LocalDate always prints ASCII ISO digits; entry.date follows the system locale's digits.
        val practiceDay = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        logMigrator.ensureMigrated()
        ritualLogDao.logDay(entry.toEntity(), PracticeDayEntity(practiceDay.toString()))
        preferences.incrementCompletedRitualCount()
    }

    override suspend fun getCompletedRitualCount(): Int = preferences.getCompletedRitualCount()

    override suspend fun isReviewPromptShown(): Boolean = preferences.isReviewPromptShown()

    override suspend fun markReviewPromptShown() = preferences.markReviewPromptShown()


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

    override fun getSoundEnabled(): Flow<Boolean> = preferences.soundEnabledFlow

    override suspend fun setSoundEnabled(enabled: Boolean) = preferences.setSoundEnabled(enabled)

    override fun getSoundChoice(): Flow<SoundChoice> = combine(
        preferences.selectedSoundIdFlow,
        customSoundsFlow
    ) { selectedId, customSounds ->
        SoundChoice.fromId(selectedId, customSounds)
    }

    /** Metadata from DataStore, stamped with whether the audio file it names is still there. */
    private val customSoundsFlow: Flow<List<CustomSound>> = preferences.customSoundsFlow.map { sounds ->
        sounds.map { it.copy(isAvailable = soundFileExists(it.fileName)) }
    }

    override fun getCustomSounds(): Flow<List<CustomSound>> = customSoundsFlow

    override suspend fun addCustomSound(sound: CustomSound) = preferences.addCustomSound(sound)

    override suspend fun removeCustomSound(id: String) = preferences.removeCustomSound(id)

    override suspend fun setSoundChoice(choice: SoundChoice) = preferences.setSelectedSoundId(choice.id)

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

    override suspend fun clearLogs() {
        // Migrate first so the legacy blob is dropped too, otherwise it would reappear later.
        logMigrator.ensureMigrated()
        ritualLogDao.deleteAll()
    }
}
