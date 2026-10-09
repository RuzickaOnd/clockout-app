package org.clockout.presentation.ritual

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
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
    val breathingStyle: BreathingStyle = BreathingStyle.CLASSIC_CIRCLE,
    val isClosureTriggered: Boolean = false,
    val isCompletedAndSaved: Boolean = false,
    /** Set for one frame once a finished ritual earns a Play Store review request. */
    val shouldRequestReview: Boolean = false,
    /** The stage this ritual has just reached, or null when it reached none. */
    val reachedStage: PathStage? = null,
    /** Whether this ritual has just brought the current week up to the rhythm threshold. */
    val rhythmWeekReached: Boolean = false
)
