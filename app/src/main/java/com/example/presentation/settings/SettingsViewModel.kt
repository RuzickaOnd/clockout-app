package com.example.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RitualRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.LanguageOption
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: RitualRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            val notificationSettingsFlow = combine(
                repository.getNotificationTime(),
                repository.getNotificationEnabled()
            ) { time, enabled ->
                Pair(time, enabled)
            }

            combine(
                notificationSettingsFlow,
                repository.getTestMode(),
                repository.getSoundEnabled(),
                repository.getLanguage(),
                repository.getLanguageOption()
            ) { notifSetting, testMode, soundEnabled, language, languageOption ->
                SettingsDataHolder(
                    notifTime = notifSetting.first,
                    notifEnabled = notifSetting.second,
                    testMode = testMode,
                    soundEnabled = soundEnabled,
                    language = language,
                    languageOption = languageOption
                )
            }.collect { data ->
                _state.update {
                    it.copy(
                        notificationHour = data.notifTime.first,
                        notificationMinute = data.notifTime.second,
                        isNotificationEnabled = data.notifEnabled,
                        isTestModeEnabled = data.testMode,
                        isSoundEnabled = data.soundEnabled,
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
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    private data class SettingsDataHolder(
        val notifTime: Pair<Int, Int>,
        val notifEnabled: Boolean,
        val testMode: Boolean,
        val soundEnabled: Boolean,
        val language: AppLanguage,
        val languageOption: LanguageOption
    )
}
