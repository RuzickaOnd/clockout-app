package com.example.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RitualRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.domain.model.MeditationSoundStyle
import com.example.notification.NotificationScheduler
import com.example.sound.AmbientAudioPlayer
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
                repository.getSoundStyle(),
                repository.getLanguage(),
                repository.getLanguageOption()
            ) { soundEnabled, soundStyle, language, languageOption ->
                AudioAndLang(soundEnabled, soundStyle, language, languageOption)
            }

            combine(notifAndTestFlow, audioAndLangFlow) { notif, audioLang ->
                SettingsDataHolder(
                    notifTime = notif.first,
                    notifEnabled = notif.second,
                    testMode = notif.third,
                    soundEnabled = audioLang.soundEnabled,
                    soundStyle = audioLang.soundStyle,
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
                        soundStyle = data.soundStyle,
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

    fun setSoundStyle(style: MeditationSoundStyle) {
        viewModelScope.launch {
            repository.setSoundStyle(style)
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }
        }
    }

    fun togglePreviewSound(style: MeditationSoundStyle) {
        if (_state.value.isPreviewPlaying) {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isPreviewPlaying = false) }
        } else {
            _state.update { it.copy(isPreviewPlaying = true) }
            audioPlayer.playPreview(style) {
                _state.update { it.copy(isPreviewPlaying = false) }
            }
        }
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
        val soundStyle: MeditationSoundStyle,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )

    private data class SettingsDataHolder(
        val notifTime: Pair<Int, Int>,
        val notifEnabled: Boolean,
        val testMode: Boolean,
        val soundEnabled: Boolean,
        val soundStyle: MeditationSoundStyle,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )
}
