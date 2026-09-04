package com.example.presentation.ritual

import com.example.domain.model.AppLanguage
import com.example.domain.model.MeditationSoundStyle
import com.example.domain.model.MoodTag
import com.example.domain.model.RitualQuestion
import com.example.domain.model.RitualStep

data class RitualState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val currentStep: RitualStep = RitualStep.Question,
    val question: RitualQuestion,
    val selectedAnswer: String? = null,
    val customAnswerText: String = "",
    val selectedMood: MoodTag? = null,
    val breathingSecondsRemaining: Int = 60,
    val isBreathingRunning: Boolean = true,
    val isSoundMuted: Boolean = false,
    val soundStyle: MeditationSoundStyle = MeditationSoundStyle.TIBETAN_BOWL,
    val isClosureTriggered: Boolean = false,
    val isCompletedAndSaved: Boolean = false
)
