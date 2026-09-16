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
import org.clockout.domain.model.BreathingStyle
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.SoundChoice
import org.clockout.notification.NotificationScheduler
import org.clockout.sound.AmbientAudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: RitualRepository,
    private val audioPlayer: AmbientAudioPlayer = AmbientAudioPlayer(),
    /**
     * Copies a picked file into app storage, refusing once [CustomSoundStorage.MAX_CUSTOM_SOUNDS]
     * slots are taken. A seam rather than a direct call, so a test can exercise the import and
     * replace flows without a content provider and a decodable audio file.
     */
    private val importSound: suspend (Context, Uri, Int) -> SoundImportResult =
        { context, uri, existingCount -> CustomSoundStorage(context).import(uri, existingCount) }
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
                repository.getTestMode(),
                repository.getIncludeWeekends()
            ) { time, enabled, testMode, includeWeekends ->
                NotificationSettings(time, enabled, testMode, includeWeekends)
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

            val result = importSound(appContext, uri, _state.value.customSounds.size)
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
    fun replaceCustomSound(context: Context, uri: Uri?, replaced: CustomSound) {
        if (uri == null) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            audioPlayer.stopAmbient()
            _state.update { it.copy(isImportingSound = true, isPreviewPlaying = false, soundImportError = null) }

            // The entry being replaced holds no slot of its own - its file is what is missing.
            val occupiedSlots = repository.getCustomSounds().first().count { it.id != replaced.id }
            when (val result = importSound(appContext, uri, occupiedSlots)) {
                is SoundImportResult.Success -> {
                    CustomSoundStorage(appContext).delete(replaced)
                    repository.addCustomSound(result.sound.copy(id = replaced.id))
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

    private data class NotificationSettings(
        val time: Pair<Int, Int>,
        val enabled: Boolean,
        val testMode: Boolean,
        val includeWeekends: Boolean
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
