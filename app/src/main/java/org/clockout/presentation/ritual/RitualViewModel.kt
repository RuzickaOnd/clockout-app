package org.clockout.presentation.ritual

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.RitualStep
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.sound.AmbientAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RitualViewModel(
    private val appContext: Context,
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val logMoodUseCase: LogMoodUseCase,
    private val ritualRepository: RitualRepository,
    private val audioPlayer: AmbientAudioPlayer = AmbientAudioPlayer()
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
                language = current.language,
                isSoundMuted = current.isSoundMuted,
                soundChoice = current.soundChoice
            )
        }
    }

    /** Leaves the ritual: the countdown stops and the ambient sound does not follow the user out. */
    fun stopSession() {
        timerJob?.cancel()
        timerJob = null
        audioPlayer.stopAmbient()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                ritualRepository.getLanguage(),
                ritualRepository.getSoundEnabled(),
                ritualRepository.getSoundChoice()
            ) { lang, soundEnabled, soundChoice ->
                Triple(lang, soundEnabled, soundChoice)
            }.collect { (lang, soundEnabled, soundChoice) ->
                _state.update {
                    it.copy(
                        language = lang,
                        isSoundMuted = !soundEnabled,
                        soundChoice = soundChoice
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
            }
            is RitualIntent.TriggerClosure -> {
                _state.update { it.copy(isClosureTriggered = true) }
            }
            is RitualIntent.ProceedToMoodLog -> {
                _state.update {
                    it.copy(currentStep = RitualStep.Log)
                }
            }
            is RitualIntent.MoodSelected -> {
                handleMoodSelected(intent.mood)
            }
            is RitualIntent.SkipMood -> {
                handleMoodSelected(MoodTag.CALM)
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

    private fun handleMoodSelected(mood: MoodTag) {
        _state.update { it.copy(selectedMood = mood) }
        viewModelScope.launch {
            val currentState = _state.value
            val finalAnswer = currentState.customAnswerText.trim().ifEmpty {
                currentState.selectedAnswer
            }
            logMoodUseCase(
                selectedAnswer = finalAnswer,
                mood = mood,
                questionText = currentState.question.text(currentState.language)
            )
            _state.update { it.copy(isCompletedAndSaved = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioPlayer.stopAmbient()
    }
}
