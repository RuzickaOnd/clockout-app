package com.example.presentation

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
import com.example.data.local.RitualPreferences
import com.example.data.repository.RitualRepositoryImpl
import com.example.domain.usecase.GetStreakUseCase
import com.example.domain.usecase.GetTodayRitualUseCase
import com.example.domain.usecase.LogMoodUseCase
import com.example.presentation.history.HistoryScreen
import com.example.presentation.history.HistoryViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.ritual.RitualScreen
import com.example.presentation.ritual.RitualViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel

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
                    RitualViewModel(getTodayRitualUseCase, logMoodUseCase, repository)
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
                    onSoundStyleSelected = { settingsViewModel.setSoundStyle(it) },
                    onTogglePreviewSound = { settingsViewModel.togglePreviewSound(it) },
                    onClearData = { settingsViewModel.clearAllData() }
                )
            }
        }
    }
}
