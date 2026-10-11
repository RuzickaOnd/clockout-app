package org.clockout.presentation.ritual

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.RitualStep
import org.clockout.domain.model.SkyPolicy
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.NoOpAnalyticsTracker
import org.clockout.domain.telemetry.analyticsId
import org.clockout.sound.AmbientAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class RitualViewModel(
    private val appContext: Context,
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val logMoodUseCase: LogMoodUseCase,
    private val shouldRequestReviewUseCase: ShouldRequestReviewUseCase,
    private val ritualRepository: RitualRepository,
    private val audioPlayer: AmbientAudioPlayer = AmbientAudioPlayer(),
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(
        RitualState(question = getTodayRitualUseCase())
    )
    val state: StateFlow<RitualState> = _state.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Which run of the ritual the current state belongs to. The screen hands its session number in
     * on every composition, so a rotation or a fold - which rebuilds the composition but keeps this
     * view model - is recognised as the same run and leaves the ritual exactly where it was. Only a
     * genuinely new number, handed in when the user starts the ritual again, wipes the slate.
     */
    private var sessionId: Int? = null

    init {
        observePreferences()
    }

    /** Starts a new run of the ritual, or does nothing if [id] is the run already in progress. */
    fun startSession(id: Int) {
        if (sessionId == id) return
        sessionId = id
        timerJob?.cancel()
        timerJob = null
        audioPlayer.stopAmbient()
        _state.update { current ->
            RitualState(
                question = getTodayRitualUseCase(),
                // Everything a preference feeds has to be carried over by hand: the flows only
                // re-emit when the stored value changes, so a fresh RitualState would otherwise
                // start the ritual on the defaults until the user next edits Settings.
                language = current.language,
                isSoundMuted = current.isSoundMuted,
                soundChoice = current.soundChoice,
                breathingStyle = current.breathingStyle
            )
        }
        analyticsTracker.track(AnalyticsEvent.RitualStarted)
    }

    /** Leaves the ritual: the countdown stops and the ambient sound does not follow the user out. */
    fun stopSession() {
        // A finished ritual reaches this too, on the way home from the completion card. Only a
        // ritual left before the mood was chosen is an abandonment - gated on selectedMood rather
        // than on isCompletedAndSaved, because the latter is set from a coroutine and leaving in
        // the moment between the tap and the write would otherwise report both endings.
        if (_state.value.selectedMood == null) {
            analyticsTracker.track(
                AnalyticsEvent.RitualAbandoned(_state.value.currentStep.analyticsId())
            )
        }
        timerJob?.cancel()
        timerJob = null
        audioPlayer.stopAmbient()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                ritualRepository.getLanguage(),
                ritualRepository.getSoundEnabled(),
                ritualRepository.getSoundChoice(),
                ritualRepository.getBreathingStyle()
            ) { language, soundEnabled, soundChoice, breathingStyle ->
                RitualPreferencesSnapshot(language, soundEnabled, soundChoice, breathingStyle)
            }.collect { snapshot ->
                _state.update {
                    it.copy(
                        language = snapshot.language,
                        isSoundMuted = !snapshot.soundEnabled,
                        soundChoice = snapshot.soundChoice,
                        breathingStyle = snapshot.breathingStyle
                    )
                }
            }
        }
    }

    fun onIntent(intent: RitualIntent) {
        when (intent) {
            is RitualIntent.AnswerSelected -> {
                _state.update {
                    it.copy(
                        selectedAnswer = intent.answer,
                        customAnswerText = ""
                    )
                }
            }
            is RitualIntent.CustomAnswerChanged -> {
                _state.update {
                    it.copy(
                        customAnswerText = intent.text,
                        selectedAnswer = if (intent.text.isNotBlank()) intent.text.trim() else null
                    )
                }
            }
            is RitualIntent.ProceedToBreathing -> {
                _state.update {
                    it.copy(
                        currentStep = RitualStep.Breathing,
                        breathingSecondsRemaining = 60,
                        isBreathingRunning = true
                    )
                }
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Question.analyticsId())
                )
                startBreathingTimer()
                if (!_state.value.isSoundMuted) {
                    audioPlayer.startAmbient(appContext, _state.value.soundChoice, true)
                }
            }
            is RitualIntent.ToggleSound -> {
                val newMuted = !_state.value.isSoundMuted
                _state.update { it.copy(isSoundMuted = newMuted) }
                if (newMuted) {
                    audioPlayer.stopAmbient()
                } else {
                    if (_state.value.currentStep is RitualStep.Breathing) {
                        audioPlayer.startAmbient(appContext, _state.value.soundChoice, true)
                    }
                }
            }
            is RitualIntent.BreathingFinished, is RitualIntent.SkipBreathing -> {
                timerJob?.cancel()
                timerJob = null
                audioPlayer.stopAmbient()
                _state.update {
                    it.copy(
                        currentStep = RitualStep.Closing,
                        isClosureTriggered = false
                    )
                }
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Breathing.analyticsId())
                )
            }
            is RitualIntent.TriggerClosure -> {
                _state.update { it.copy(isClosureTriggered = true) }
            }
            is RitualIntent.ProceedToMoodLog -> {
                _state.update {
                    it.copy(currentStep = RitualStep.Log)
                }
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Closing.analyticsId())
                )
            }
            is RitualIntent.MoodSelected -> {
                handleMoodSelected(intent.mood)
            }
            is RitualIntent.SkipMood -> {
                handleMoodSelected(MoodTag.CALM, moodWasChosen = false)
            }
        }
    }

    private fun startBreathingTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _state.value.breathingSecondsRemaining > 0) {
                delay(1000)
                val newRemaining = _state.value.breathingSecondsRemaining - 1
                _state.update {
                    it.copy(breathingSecondsRemaining = newRemaining)
                }
                val elapsed = 60 - newRemaining
                if (elapsed > 0 && elapsed % 4 == 0) {
                    val isInhaleStart = (elapsed % 8) == 0
                    val choice = _state.value.soundChoice
                    val isBowl = choice is SoundChoice.BuiltIn && choice.style == MeditationSoundStyle.TIBETAN_BOWL
                    if (!_state.value.isSoundMuted && isBowl) {
                        audioPlayer.triggerBowlStrike(highPitch = isInhaleStart)
                    }
                }
            }
            if (_state.value.breathingSecondsRemaining <= 0) {
                onIntent(RitualIntent.BreathingFinished)
            }
        }
    }

    /**
     * [moodWasChosen] is false when the user skipped the step. Skipping still logs [MoodTag.CALM],
     * because the history needs something to show, but it is not the user saying the day went well
     * - so it must never be read as the good mood that earns a review request.
     */
    private fun handleMoodSelected(mood: MoodTag, moodWasChosen: Boolean = true) {
        _state.update { it.copy(selectedMood = mood) }
        viewModelScope.launch {
            val currentState = _state.value
            val finalAnswer = currentState.customAnswerText.trim().ifEmpty {
                currentState.selectedAnswer
            }
            val pathBefore = ritualRepository.getPathProgress().first()
            val skyBefore = ritualRepository.getSky().first()
            logMoodUseCase(
                selectedAnswer = finalAnswer,
                mood = mood,
                questionText = currentState.question.text(currentState.language)
            )
            val pathAfter = ritualRepository.getPathProgress().first()
            val skyAfter = ritualRepository.getSky().first()
            val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            _state.update {
                it.copy(
                    isCompletedAndSaved = true,
                    shouldRequestReview = moodWasChosen && shouldRequestReviewUseCase(mood),
                    reachedStage = pathAfter.stage.takeIf { stage -> stage != pathBefore.stage },
                    // Announced when this week's figure first gets a line, not merely when the week
                    // reaches its rhythm: a first, partial week can be in rhythm on a lone star.
                    rhythmConstellationNumber = SkyPolicy.constellationNumber(thisMonday).takeIf {
                        SkyPolicy.isLit(thisMonday, skyAfter.days, skyAfter.rhythmWeeks) &&
                            !SkyPolicy.isLit(thisMonday, skyBefore.days, skyBefore.rhythmWeeks)
                    },
                    skyTeaser = skyAfter.daysToFirstConstellation?.let { SkyTeaser(skyAfter.summary.stars, it) }
                )
            }
            // Read after the log is written, so the count includes today.
            val daysClosedThisWeek = ritualRepository.getStreakInfo().first().completedThisWeekCount
            analyticsTracker.track(
                AnalyticsEvent.RitualStepCompleted(RitualStep.Log.analyticsId())
            )
            analyticsTracker.track(
                AnalyticsEvent.RitualCompleted(
                    mood = mood.id,
                    daysClosedThisWeek = daysClosedThisWeek
                )
            )
        }
    }

    /** Clears the one-shot flag once the host has handed the request over to Play. */
    fun onReviewRequestHandled() {
        _state.update { it.copy(shouldRequestReview = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioPlayer.stopAmbient()
    }
}

/** The Settings values the ritual reacts to, combined so one emission updates the state once. */
private data class RitualPreferencesSnapshot(
    val language: AppLanguage,
    val soundEnabled: Boolean,
    val soundChoice: SoundChoice,
    val breathingStyle: BreathingStyle
)
