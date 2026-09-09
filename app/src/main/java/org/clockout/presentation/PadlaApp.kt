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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
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

/**
 * Which screen the user is on has to outlive the composition: rotating a tablet or folding a
 * foldable rebuilds it from scratch, and without this the app would silently jump back to Home.
 * The screens are written down as plain numbers rather than by class name, because R8 rewrites
 * class names in a release build and the restored value has to survive that too.
 */
private val ScreenSaver = Saver<Screen, Int>(
    save = { screen ->
        when (screen) {
            Screen.Home -> 0
            Screen.Ritual -> 1
            Screen.History -> 2
            Screen.Settings -> 3
        }
    },
    restore = { id ->
        when (id) {
            1 -> Screen.Ritual
            2 -> Screen.History
            3 -> Screen.Settings
            else -> Screen.Home
        }
    }
)

@Composable
fun PadlaApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { RitualPreferences(context) }
    val repository = remember {
        RitualRepositoryImpl(
            preferences = preferences,
            ritualLogDao = ClockoutDatabase.getInstance(context).ritualLogDao()
        )
    }
    val getTodayRitualUseCase = remember { GetTodayRitualUseCase(repository) }
    val logMoodUseCase = remember { LogMoodUseCase(repository) }
    val getStreakUseCase = remember { GetStreakUseCase(repository) }

    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Home)
    }
    // Counts the runs of the ritual. A new number means "start over"; the same number handed back
    // after a rotation or a fold means "this is still the same run", so the ritual is not restarted.
    var ritualSession by rememberSaveable { mutableIntStateOf(0) }

    // From History and Settings, pressing system back returns to Home. The ritual handles back
    // itself, because leaving it also has to stop its countdown and its ambient sound.
    BackHandler(enabled = currentScreen == Screen.History || currentScreen == Screen.Settings) {
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
                    onStartRitual = {
                        ritualSession++
                        currentScreen = Screen.Ritual
                    },
                    onNavigateToHistory = { currentScreen = Screen.History },
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
            }
            is Screen.Ritual -> {
                // Kept by the ViewModelStore rather than by the composition, so a rotation or a
                // fold in the middle of the ritual does not throw away the answer and the timer.
                val ritualViewModel: RitualViewModel = viewModel {
                    RitualViewModel(
                        context.applicationContext,
                        getTodayRitualUseCase,
                        logMoodUseCase,
                        repository
                    )
                }
                // Resets the ritual only when the user actually started it again.
                LaunchedEffect(ritualSession) { ritualViewModel.startSession(ritualSession) }
                val ritualState by ritualViewModel.state.collectAsState()

                val leaveRitual = {
                    ritualViewModel.stopSession()
                    currentScreen = Screen.Home
                }
                BackHandler { leaveRitual() }

                RitualScreen(
                    state = ritualState,
                    onIntent = { ritualViewModel.onIntent(it) },
                    onFinish = leaveRitual
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
                    onIncludeWeekendsToggled = { settingsViewModel.setIncludeWeekends(it) },
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
