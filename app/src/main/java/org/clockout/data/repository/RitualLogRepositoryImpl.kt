package org.clockout.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.local.db.RitualLogDao
import org.clockout.data.local.db.toDomain
import org.clockout.data.local.db.toEntity
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.repository.RitualLogRepository
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RitualLogRepositoryImpl(
    private val preferences: RitualPreferences,
    private val ritualLogDao: RitualLogDao,
    private val logMigrator: RitualLogMigrator
) : RitualLogRepository {

    /**
     * A new formatter on every use: this repository lives as long as the process, SimpleDateFormat
     * is not thread-safe, and the stored dates follow the system locale's digits, which can change
     * while the process runs.
     */
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        return dateFormat().format(Date())
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

        val dateFormat = dateFormat()
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
            date = dateFormat().format(now),
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

    override suspend fun clearLogs() {
        // Migrate first so the legacy blob is dropped too, otherwise it would reappear later.
        logMigrator.ensureMigrated()
        ritualLogDao.deleteAll()
    }
}
