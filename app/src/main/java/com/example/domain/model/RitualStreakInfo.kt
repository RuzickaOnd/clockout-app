package com.example.domain.model

data class DayStatus(
    val dayNameCs: String,
    val dayNameEn: String,
    val dayOfMonth: Int,
    val dateString: String,
    val isToday: Boolean,
    val isCompleted: Boolean,
    val moodTag: MoodTag? = null
) {
    val dayName: String get() = dayNameEn
    fun dayName(lang: AppLanguage): String = if (lang == AppLanguage.CZECH) dayNameCs else dayNameEn
}

data class RitualStreakInfo(
    val completedThisWeekCount: Int,
    val targetWorkDays: Int = 5,
    val isTodayCompleted: Boolean,
    val todayLog: RitualLogEntry?,
    val weekDays: List<DayStatus>
)
