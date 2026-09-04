package com.example.presentation.ritual

import com.example.domain.model.MoodTag

sealed interface RitualIntent {
    data class AnswerSelected(val answer: String) : RitualIntent
    data class CustomAnswerChanged(val text: String) : RitualIntent
    data object ProceedToBreathing : RitualIntent
    data object BreathingFinished : RitualIntent
    data object SkipBreathing : RitualIntent
    data object ToggleSound : RitualIntent
    data object TriggerClosure : RitualIntent
    data object ProceedToMoodLog : RitualIntent
    data class MoodSelected(val mood: MoodTag) : RitualIntent
    data object SkipMood : RitualIntent
}
