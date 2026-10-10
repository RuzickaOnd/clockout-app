package org.clockout.screenshot

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.DayStatus
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.model.SkyData
import org.clockout.presentation.history.HistoryState
import org.clockout.presentation.home.HomeState
import org.clockout.presentation.settings.SettingsState
import java.time.LocalDate

/**
 * One fixed set of states for every form-factor screenshot, so a diff between two devices only
 * ever shows a layout difference and never a difference in the data being rendered.
 */
object Fixtures {

    private val skyDays = (0L..11L).map { LocalDate.of(2026, 9, 28).plusDays(it) }
        .filter { it.dayOfWeek.value <= 5 && it != LocalDate.of(2026, 10, 7) }

    /** Two weeks in rhythm in October 2026, some days still carrying their mood. */
    val sky = SkyData(
        days = skyDays.toSet(),
        logs = listOf(
            LocalDate.of(2026, 9, 28) to MoodTag.CALM,
            LocalDate.of(2026, 9, 30) to MoodTag.SATISFIED,
            LocalDate.of(2026, 10, 2) to MoodTag.TIRED,
            LocalDate.of(2026, 10, 6) to MoodTag.TENSE,
            LocalDate.of(2026, 10, 9) to MoodTag.SATISFIED
        ).associate { (date, mood) -> date to RitualLogEntry(date.toString(), "What went well today?", null, mood, 0L) },
        rhythmWeeks = mapOf(LocalDate.of(2026, 9, 28) to true, LocalDate.of(2026, 10, 5) to true),
        today = LocalDate.of(2026, 10, 9)
    )

    val pathProgress = PathProgress(
        closedDays = 22, stage = PathStage.DOWN_THE_STAIRS, nextStage = PathStage.STREET_AT_DUSK,
        daysToNextStage = 13, rhythmWeeks = 3, longestRhythmWeeks = 4, isCurrentWeekInRhythm = true
    )

    val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf(
            "Finished a big task",
            "Helped a colleague",
            "Learned something new"
        ),
        suggestedAnswersCs = listOf(
            "Dokončil jsem velký úkol",
            "Pomohl jsem kolegovi",
            "Naučil jsem se něco nového"
        )
    )

    private val weekDays = (0..6).map { index ->
        DayStatus(
            dayIndex = index,
            dayOfMonth = index + 1,
            dateString = "2026-09-0${index + 1}",
            isToday = index == 2,
            isCompleted = index < 2,
            moodTag = if (index < 2) MoodTag.CALM else null
        )
    }

    private fun streak(todayCompleted: Boolean, todayLog: RitualLogEntry?) = RitualStreakInfo(
        completedThisWeekCount = if (todayCompleted) 3 else 2,
        isTodayCompleted = todayCompleted,
        todayLog = todayLog,
        weekDays = weekDays
    )

    val homeOpen = HomeState(
        language = AppLanguage.ENGLISH,
        todayQuestion = question,
        isTestModeEnabled = true,
        pathProgress = pathProgress,
        streakInfo = streak(todayCompleted = false, todayLog = null)
    )

    val homeClosed = HomeState(
        language = AppLanguage.ENGLISH,
        todayQuestion = question,
        isTestModeEnabled = true,
        pathProgress = pathProgress,
        streakInfo = streak(
            todayCompleted = true,
            todayLog = RitualLogEntry(
                date = "2026-09-03",
                questionText = question.textEn,
                selectedAnswer = "Finished a big task",
                moodTag = MoodTag.CALM,
                timestamp = 3L
            )
        )
    )

    private val logs = (1..6).map { day ->
        RitualLogEntry(
            date = "2026-09-0$day",
            questionText = question.textEn,
            selectedAnswer = "Finished a big task",
            moodTag = MoodTag.entries[day % MoodTag.entries.size],
            timestamp = day.toLong()
        )
    }

    val history = HistoryState(
        language = AppLanguage.ENGLISH,
        sky = sky,
        logs = logs,
        totalCount = logs.size,
        moodCounts = MoodTag.entries.associateWith { mood -> logs.count { it.moodTag == mood } }
    )

    val settings = SettingsState(
        language = AppLanguage.ENGLISH,
        isNotificationEnabled = true,
        isTestModeEnabled = true
    )
}
