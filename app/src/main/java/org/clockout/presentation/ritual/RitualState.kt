package org.clockout.presentation.ritual

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStep

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
    val soundChoice: SoundChoice = SoundChoice.Default,
    val isClosureTriggered: Boolean = false,
    val isCompletedAndSaved: Boolean = false
)
