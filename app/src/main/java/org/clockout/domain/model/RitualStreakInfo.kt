package org.clockout.domain.model

/**
 * [dayIndex] is the day's position in the week counted from Monday (0 = Monday ... 6 = Sunday).
 * It matches the item order of the `day_names_short` string array, from which the presentation
 * layer takes the localized day name (see `presentation/util/Localization.kt`). The domain model
 * therefore stays free of any `R` dependency, the same way `MoodTag` does.
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
