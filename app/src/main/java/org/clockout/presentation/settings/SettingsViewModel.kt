package org.clockout.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.SoundImportResult
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: RitualRepository,
    private val audioPlayer: SoundPlayer,
    private val reminderScheduler: ReminderScheduler,
    /**
     * Copies a picked file into app storage, refusing once the slots are taken. A port rather than
     * a direct call, so a test can exercise the import and replace flows without a content
     * provider and a decodable audio file.
     */
    private val soundFiles: SoundFileStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        observeSettings()
        // Its own collector rather than a fifth flow in observeSettings: that combine already takes
        // four and adding another would mean restructuring the holder for one unrelated value.
        viewModelScope.launch {
            repository.getTelemetryConsent().collect { consent ->
                _state.update { it.copy(telemetryConsent = consent) }
            }
        }
    }

    fun setTelemetryConsent(granted: Boolean) {
        viewModelScope.launch {
            repository.setTelemetryConsent(
                if (granted) TelemetryConsent.GRANTED else TelemetryConsent.DENIED
            )
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            val notifAndTestFlow = combine(
                repository.getNotificationTime(),
                repository.getNotificationEnabled(),
                repository.getTestMode(),
                repository.getIncludeWeekends(),
                repository.getCustomNotificationTime()
            ) { time, enabled, testMode, includeWeekends, customTime ->
                NotificationSettings(time, enabled, testMode, includeWeekends, customTime)
            }

            val audioAndLangFlow = combine(
                repository.getSoundEnabled(),
                repository.getSoundChoice(),
                repository.getLanguage(),
                repository.getLanguageOption()
            ) { soundEnabled, soundChoice, language, languageOption ->
                AudioAndLang(soundEnabled, soundChoice, language, languageOption)
            }

            combine(
                notifAndTestFlow,
                audioAndLangFlow,
                repository.getCustomSounds(),
                repository.getBreathingStyle()
            ) { notif, audioLang, customSounds, breathingStyle ->
                SettingsDataHolder(
                    notifTime = notif.time,
                    notifEnabled = notif.enabled,
                    customNotifTime = notif.customTime,
                    testMode = notif.testMode,
                    includeWeekends = notif.includeWeekends,
                    soundEnabled = audioLang.soundEnabled,
                    soundChoice = audioLang.soundChoice,
                    customSounds = customSounds,
                    language = audioLang.language,
                    languageOption = audioLang.languageOption,
                    breathingStyle = breathingStyle
                )
            }.collect { data ->
                _state.update {
                    it.copy(
                        notificationHour = data.notifTime.first,
                        notificationMinute = data.notifTime.second,
                        isNotificationEnabled = data.notifEnabled,
                        customNotificationTime = data.customNotifTime,
                        includeWeekends = data.includeWeekends,
                        isTestModeEnabled = data.testMode,
                        isSoundEnabled = data.soundEnabled,
                        soundChoice = data.soundChoice,
                        customSounds = data.customSounds,
                        language = data.language,
                        selectedLanguageOption = data.languageOption,
                        breathingStyle = data.breathingStyle
                    )
                }
            }
        }
    }

    fun setLanguageOption(option: LanguageOption) {
        viewModelScope.launch {
            repository.setLanguageOption(option)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationEnabled(enabled)
            if (enabled) {
                val currentState = _state.value
                reminderScheduler.schedule(currentState.notificationHour, currentState.notificationMinute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setNotificationTime(hour, minute)
            if (_state.value.isNotificationEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun setCustomNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setCustomNotificationTime(hour, minute)
            if (_state.value.isNotificationEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun setIncludeWeekends(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIncludeWeekends(enabled)
        }
    }

    fun setTestMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTestMode(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
            if (!enabled) {
                audioPlayer.stopAmbient()
                _state.update { it.copy(isPreviewPlaying = false) }
            }
        }
    }

    fun setSoundChoice(choice: SoundChoice) {
        viewModelScope.launch {
            repository.setSoundChoice(choice)
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }
        }
    }

    fun togglePreviewSound(choice: SoundChoice) {
        if (_state.value.isPreviewPlaying) {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }
        } else {
            _state.update { it.copy(isPreviewPlaying = true) }
            audioPlayer.playPreview(choice) {
                _state.update { it.copy(isPreviewPlaying = false) }
            }
        }
    }

    /**
     * Copies the picked audio file into app storage, registers it and selects it
     * right away so the user hears their own sound on the next ritual.
     */
    fun importCustomSound(source: String?) {
        if (source == null) return
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isImportingSound = true, isPreviewPlaying = false, soundImportError = null) }

            val result = soundFiles.import(source, _state.value.customSounds.size)
            when (result) {
                is SoundImportResult.Success -> {
                    repository.addCustomSound(result.sound)
                    repository.setSoundChoice(SoundChoice.Custom(result.sound))
                    _state.update { it.copy(isImportingSound = false, soundImportError = null) }
                }

                is SoundImportResult.Failure -> {
                    _state.update { it.copy(isImportingSound = false, soundImportError = result.error) }
                }
            }
        }
    }

    /**
     * Puts a freshly picked file in the place of [replaced], whose audio a cloud backup left
     * behind. Keeping the id matters: preferences store the selected sound by id, so a new one
     * would strand that selection a second time.
     */
    fun replaceCustomSound(source: String?, replaced: CustomSound) {
        if (source == null) return
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isImportingSound = true, isPreviewPlaying = false, soundImportError = null) }

            // The entry being replaced holds no slot of its own - its file is what is missing.
            val occupiedSlots = repository.getCustomSounds().first().count { it.id != replaced.id }
            when (val result = soundFiles.import(source, occupiedSlots)) {
                is SoundImportResult.Success -> {
                    soundFiles.delete(replaced)
                    repository.addCustomSound(result.sound.copy(id = replaced.id))
                    _state.update { it.copy(isImportingSound = false, soundImportError = null) }
                }

                is SoundImportResult.Failure -> {
                    _state.update { it.copy(isImportingSound = false, soundImportError = result.error) }
                }
            }
        }
    }

    fun deleteCustomSound(sound: CustomSound) {
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }

            soundFiles.delete(sound)
            repository.removeCustomSound(sound.id)
            if (_state.value.soundChoice.id == sound.id) {
                repository.setSoundChoice(SoundChoice.Default)
            }
        }
    }

    fun dismissSoundImportError() {
        _state.update { it.copy(soundImportError = null) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAmbient()
    }

    private data class NotificationSettings(
        val time: Pair<Int, Int>,
        val enabled: Boolean,
        val testMode: Boolean,
        val includeWeekends: Boolean,
        val customTime: Pair<Int, Int>?
    )

    private data class AudioAndLang(
        val soundEnabled: Boolean,
        val soundChoice: SoundChoice,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )

    private data class SettingsDataHolder(
        val notifTime: Pair<Int, Int>,
        val notifEnabled: Boolean,
        val customNotifTime: Pair<Int, Int>?,
        val testMode: Boolean,
        val includeWeekends: Boolean,
        val soundEnabled: Boolean,
        val soundChoice: SoundChoice,
        val customSounds: List<CustomSound>,
        val language: AppLanguage,
        val languageOption: LanguageOption,
        val breathingStyle: BreathingStyle
    )
}
