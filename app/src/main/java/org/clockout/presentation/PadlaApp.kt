package org.clockout.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.clockout.data.local.RitualPreferences
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.usecase.GetStreakUseCase
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.history.HistoryViewModel
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeViewModel
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsViewModel

sealed interface Screen {
    data object Home : Screen
    data object Ritual : Screen
    data object History : Screen
    data object Settings : Screen
}

@Composable
fun PadlaApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { RitualPreferences(context) }
    val repository = remember { RitualRepositoryImpl(preferences) }
    val getTodayRitualUseCase = remember { GetTodayRitualUseCase(repository) }
    val logMoodUseCase = remember { LogMoodUseCase(repository) }
    val getStreakUseCase = remember { GetStreakUseCase(repository) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // When inside Ritual, History, or Settings, pressing system back returns to Home
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(repository, getTodayRitualUseCase, getStreakUseCase)
                }
                val homeState by homeViewModel.state.collectAsState()

                HomeScreen(
                    state = homeState,
                    onStartRitual = { currentScreen = Screen.Ritual },
                    onNavigateToHistory = { currentScreen = Screen.History },
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
            }
            is Screen.Ritual -> {
                // Instantiate fresh RitualViewModel each time ritual is started
                val ritualViewModel = remember {
                    RitualViewModel(
                        context.applicationContext,
                        getTodayRitualUseCase,
                        logMoodUseCase,
                        repository
                    )
                }
                val ritualState by ritualViewModel.state.collectAsState()

                RitualScreen(
                    state = ritualState,
                    onIntent = { ritualViewModel.onIntent(it) },
                    onFinish = { currentScreen = Screen.Home }
                )
            }
            is Screen.History -> {
                val historyViewModel: HistoryViewModel = viewModel {
                    HistoryViewModel(repository)
                }
                val historyState by historyViewModel.state.collectAsState()

                HistoryScreen(
                    state = historyState,
                    onBack = { currentScreen = Screen.Home },
                    onClearHistory = { historyViewModel.clearAllHistory() }
                )
            }
            is Screen.Settings -> {
                val settingsViewModel: SettingsViewModel = viewModel {
                    SettingsViewModel(repository)
                }
                val settingsState by settingsViewModel.state.collectAsState()

                SettingsScreen(
                    state = settingsState,
                    onBack = { currentScreen = Screen.Home },
                    onLanguageOptionSelected = { settingsViewModel.setLanguageOption(it) },
                    onNotificationToggled = { enabled -> settingsViewModel.setNotificationEnabled(enabled, context) },
                    onTimeSelected = { h, m -> settingsViewModel.setNotificationTime(h, m, context) },
                    onTestModeToggled = { settingsViewModel.setTestMode(it) },
                    onSoundToggled = { settingsViewModel.setSoundEnabled(it) },
                    onSoundChoiceSelected = { settingsViewModel.setSoundChoice(it) },
                    onTogglePreviewSound = { settingsViewModel.togglePreviewSound(it, context) },
                    onUploadCustomSound = { uri -> settingsViewModel.importCustomSound(context, uri) },
                    onDeleteCustomSound = { item -> settingsViewModel.deleteCustomSound(context, item) },
                    onDismissSoundImportError = { settingsViewModel.dismissSoundImportError() },
                    onClearData = { settingsViewModel.clearAllData() }
                )
            }
        }
    }
}
