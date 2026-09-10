package org.clockout.domain.model

/**
 * [dayIndex] je pořadí dne v týdnu od pondělí (0 = pondělí ... 6 = neděle) - odpovídá pořadí
 * položek v string-array `day_names_short`, ze kterého si prezentační vrstva vezme lokalizovaný
 * název dne (viz `presentation/util/Localization.kt`). Doménový model tím zůstává bez závislosti
 * na `R`, stejně jako u `MoodTag`.
 */
data class DayStatus(
    val dayIndex: Int,
    val dayOfMonth: Int,
    val dateString: String,
    val isToday: Boolean,
    val isCompleted: Boolean,
    val moodTag: MoodTag? = null
)

data class RitualStreakInfo(
    val completedThisWeekCount: Int,
    val targetWorkDays: Int = 5,
    val isTodayCompleted: Boolean,
    val todayLog: RitualLogEntry?,
    val weekDays: List<DayStatus>
) {
    /** Share of the week's offered days that are already closed. */
    val completionPercent: Int
        get() = if (targetWorkDays == 0) 0 else completedThisWeekCount * 100 / targetWorkDays
}
