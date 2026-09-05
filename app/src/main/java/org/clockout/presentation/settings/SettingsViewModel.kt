package org.clockout.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.SoundImportResult
import org.clockout.data.repository.RitualRepository
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.SoundChoice
import org.clockout.notification.NotificationScheduler
import org.clockout.sound.AmbientAudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: RitualRepository,
    private val audioPlayer: AmbientAudioPlayer = AmbientAudioPlayer()
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            val notifAndTestFlow = combine(
                repository.getNotificationTime(),
                repository.getNotificationEnabled(),
                repository.getTestMode()
            ) { time, enabled, testMode ->
                Triple(time, enabled, testMode)
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
                repository.getCustomSounds()
            ) { notif, audioLang, customSounds ->
                SettingsDataHolder(
                    notifTime = notif.first,
                    notifEnabled = notif.second,
                    testMode = notif.third,
                    soundEnabled = audioLang.soundEnabled,
                    soundChoice = audioLang.soundChoice,
                    customSounds = customSounds,
                    language = audioLang.language,
                    languageOption = audioLang.languageOption
                )
            }.collect { data ->
                _state.update {
                    it.copy(
                        notificationHour = data.notifTime.first,
                        notificationMinute = data.notifTime.second,
                        isNotificationEnabled = data.notifEnabled,
                        isTestModeEnabled = data.testMode,
                        isSoundEnabled = data.soundEnabled,
                        soundChoice = data.soundChoice,
                        customSounds = data.customSounds,
                        language = data.language,
                        selectedLanguageOption = data.languageOption
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

    fun setNotificationEnabled(enabled: Boolean, context: Context? = null) {
        viewModelScope.launch {
            repository.setNotificationEnabled(enabled)
            if (context != null) {
                if (enabled) {
                    val currentState = _state.value
                    NotificationScheduler.scheduleDailyNotification(
                        context,
                        currentState.notificationHour,
                        currentState.notificationMinute
                    )
                } else {
                    NotificationScheduler.cancelNotification(context)
                }
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int, context: Context? = null) {
        viewModelScope.launch {
            repository.setNotificationTime(hour, minute)
            if (context != null && _state.value.isNotificationEnabled) {
                NotificationScheduler.scheduleDailyNotification(context, hour, minute)
            }
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

    fun togglePreviewSound(choice: SoundChoice, context: Context) {
        if (_state.value.isPreviewPlaying) {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }
        } else {
            _state.update { it.copy(isPreviewPlaying = true) }
            audioPlayer.playPreview(context.applicationContext, choice) {
                _state.update { it.copy(isPreviewPlaying = false) }
            }
        }
    }

    /**
     * Copies the picked audio file into app storage, registers it and selects it
     * right away so the user hears their own sound on the next ritual.
     */
    fun importCustomSound(context: Context, uri: Uri?) {
        if (uri == null) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isImportingSound = true, isPreviewPlaying = false, soundImportError = null) }

            val result = CustomSoundStorage(appContext).import(uri, _state.value.customSounds.size)
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

    fun deleteCustomSound(context: Context, sound: CustomSound) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }

            CustomSoundStorage(appContext).delete(sound)
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

    private data class AudioAndLang(
        val soundEnabled: Boolean,
        val soundChoice: SoundChoice,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )

    private data class SettingsDataHolder(
        val notifTime: Pair<Int, Int>,
        val notifEnabled: Boolean,
        val testMode: Boolean,
        val soundEnabled: Boolean,
        val soundChoice: SoundChoice,
        val customSounds: List<CustomSound>,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )
}
