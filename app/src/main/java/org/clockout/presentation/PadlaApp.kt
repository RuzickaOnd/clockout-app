package org.clockout.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import android.content.Intent
import org.clockout.BreathingStyleActivity
import org.clockout.PathActivity
import org.clockout.SkyActivity
import org.clockout.R
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.telemetry.UserProperty
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.history.HistoryViewModel
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeViewModel
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsViewModel
import org.clockout.presentation.components.TelemetryConsentDialog
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextSlate
import org.clockout.util.PRIVACY_POLICY_URL
import org.clockout.util.openUrl
import kotlinx.coroutines.launch

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
fun PadlaApp(
    modifier: Modifier = Modifier,
    /**
     * Hands a finished ritual over to the Play in-app review flow, which needs an Activity and so
     * can only be supplied by [org.clockout.MainActivity]. Previews and tests leave it a no-op.
     */
    onRequestReview: () -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val container = appContainer()
    val analyticsTracker = rememberAnalyticsTracker(context)
    val scope = rememberCoroutineScope()

    // Null until DataStore has actually emitted. Starting at UNKNOWN instead would flash the opt-in
    // dialog on every launch for users who answered it long ago, the same trap
    // HomeState.isTestModeEnabled documents.
    val consent by container.settings.getTelemetryConsent().collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(consent) {
        analyticsTracker.setCollectionEnabled(consent == TelemetryConsent.GRANTED)
    }

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

    val language by container.settings.getLanguage().collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)

    // Reported from here rather than from the Application class: consent may arrive long after
    // startup, and these have to be sent when it does, not only when a setting next changes.
    val breathingStyle by container.settings.getBreathingStyle().collectAsStateWithLifecycle(initialValue = null)
    val soundEnabled by container.sounds.getSoundEnabled().collectAsStateWithLifecycle(initialValue = null)
    val includeWeekends by container.settings.getIncludeWeekends().collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(consent, breathingStyle, soundEnabled, includeWeekends, language) {
        if (consent != TelemetryConsent.GRANTED) return@LaunchedEffect
        breathingStyle?.let { analyticsTracker.setUserProperty(UserProperty.BREATHING_STYLE, it.id) }
        soundEnabled?.let {
            analyticsTracker.setUserProperty(UserProperty.SOUND_ENABLED, it.toString())
        }
        includeWeekends?.let {
            analyticsTracker.setUserProperty(UserProperty.INCLUDE_WEEKENDS, it.toString())
        }
        analyticsTracker.setUserProperty(UserProperty.APP_LANGUAGE, language.code)
    }

    AppLocalization(language) {
        Box(modifier = Modifier.fillMaxSize()) {
            // A phone keeps the two icons in its top bar, which is where they have always been and where
            // a thumb can reach them. From a medium window upwards the same two destinations move to a
            // rail along the leading edge, as the large-screen guidance asks - far easier to hit on a
            // tablet than the top right corner. The ritual is a flow the user is inside of, so it keeps
            // the whole window to itself either way.
            val isCompactWidth = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass ==
                WindowWidthSizeClass.COMPACT
            val showRail = !isCompactWidth && currentScreen != Screen.Ritual

            // The rail follows the frosted glass palette rather than the stock Material one, so it reads
            // as part of the same surface as the cards next to it.
            val railItemColors = NavigationSuiteDefaults.itemColors(
                navigationRailItemColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = IndigoAccent,
                    selectedTextColor = IndigoAccent,
                    unselectedIconColor = TextSlate,
                    unselectedTextColor = TextSlate,
                    indicatorColor = Color(0x406366F1)
                )
            )

            NavigationSuiteScaffold(
                layoutType = if (showRail) NavigationSuiteType.NavigationRail else NavigationSuiteType.None,
                containerColor = Color.Transparent,
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    navigationRailContainerColor = Color.Transparent
                ),
                navigationSuiteItems = {
                    RailDestination.entries.forEach { destination ->
                        item(
                            selected = currentScreen == destination.screen,
                            onClick = { currentScreen = destination.screen },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            colors = railItemColors
                        )
                    }
                }
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition",
                    modifier = modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        is Screen.Home -> {
                            val homeViewModel: HomeViewModel = viewModel {
                                HomeViewModel(container.ritualLogs, container.settings, container.progress)
                            }
                            val homeState by homeViewModel.state.collectAsStateWithLifecycle()

                            HomeScreen(
                                state = homeState,
                                onStartRitual = {
                                    ritualSession++
                                    currentScreen = Screen.Ritual
                                },
                                onNavigateToHistory = { currentScreen = Screen.History },
                                onOpenPath = {
                                    context.startActivity(Intent(context, PathActivity::class.java))
                                },
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                showTopBarNavigation = !showRail
                            )
                        }
                        is Screen.Ritual -> {
                            // Kept by the ViewModelStore rather than by the composition, so a rotation or a
                            // fold in the middle of the ritual does not throw away the answer and the timer.
                            val ritualViewModel: RitualViewModel = viewModel {
                                RitualViewModel(
                                    container.ritualLogs,
                                    container.settings,
                                    container.sounds,
                                    container.completeRitual,
                                    audioPlayer = container.newSoundPlayer(),
                                    analyticsTracker = analyticsTracker
                                )
                            }
                            // Resets the ritual only when the user actually started it again.
                            LaunchedEffect(ritualSession) { ritualViewModel.startSession(ritualSession) }
                            val ritualState by ritualViewModel.state.collectAsStateWithLifecycle()

                            // The request goes out on the completion card rather than on the way
                            // home: the user is done, nothing is interrupted, and the good mood
                            // that earned the request is still on screen.
                            LaunchedEffect(ritualState.shouldRequestReview) {
                                if (ritualState.shouldRequestReview) {
                                    onRequestReview()
                                    ritualViewModel.onReviewRequestHandled()
                                }
                            }

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
                                HistoryViewModel(container.ritualLogs, container.settings, container.progress)
                            }
                            val historyState by historyViewModel.state.collectAsStateWithLifecycle()

                            HistoryScreen(
                                state = historyState,
                                onBack = { currentScreen = Screen.Home },
                                onClearHistory = { historyViewModel.clearAllHistory() },
                                onOpenSky = { context.startActivity(Intent(context, SkyActivity::class.java)) },
                                showBackButton = !showRail
                            )
                        }
                        is Screen.Settings -> {
                            val settingsViewModel: SettingsViewModel = viewModel {
                                SettingsViewModel(
                                    settings = container.settings,
                                    sounds = container.sounds,
                                    logs = container.ritualLogs,
                                    audioPlayer = container.newSoundPlayer(),
                                    reminderScheduler = container.reminderScheduler,
                                    soundFiles = container.soundFileStore
                                )
                            }
                            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

                            SettingsScreen(
                                state = settingsState,
                                onBack = { currentScreen = Screen.Home },
                                onLanguageOptionSelected = { settingsViewModel.setLanguageOption(it) },
                                onNotificationToggled = { enabled -> settingsViewModel.setNotificationEnabled(enabled) },
                                onTimeSelected = { h, m -> settingsViewModel.setNotificationTime(h, m) },
                                onCustomTimeSelected = { h, m -> settingsViewModel.setCustomNotificationTime(h, m) },
                                onTestModeToggled = { settingsViewModel.setTestMode(it) },
                                onIncludeWeekendsToggled = { settingsViewModel.setIncludeWeekends(it) },
                                onSoundToggled = { settingsViewModel.setSoundEnabled(it) },
                                onSoundChoiceSelected = { settingsViewModel.setSoundChoice(it) },
                                onTogglePreviewSound = { settingsViewModel.togglePreviewSound(it) },
                                onUploadCustomSound = { uri -> settingsViewModel.importCustomSound(uri?.toString()) },
                                onReplaceCustomSound = { uri, sound -> settingsViewModel.replaceCustomSound(uri?.toString(), sound) },
                                onDeleteCustomSound = { item -> settingsViewModel.deleteCustomSound(item) },
                                onDismissSoundImportError = { settingsViewModel.dismissSoundImportError() },
                                onOpenBreathingStyle = {
                                    context.startActivity(Intent(context, BreathingStyleActivity::class.java))
                                },
                                onOpenPath = {
                                    context.startActivity(Intent(context, PathActivity::class.java))
                                },
                                onTelemetryConsentToggled = { settingsViewModel.setTelemetryConsent(it) },
                                onOpenPrivacyPolicy = { context.openUrl(PRIVACY_POLICY_URL) },
                                onClearData = { settingsViewModel.clearAllData() },
                                showBackButton = !showRail
                            )
                        }
                    }
                }
            }
            if (consent == TelemetryConsent.UNKNOWN) {
                TelemetryConsentDialog(
                    onGrant = {
                        scope.launch { container.settings.setTelemetryConsent(TelemetryConsent.GRANTED) }
                    },
                    onDeny = {
                        scope.launch { container.settings.setTelemetryConsent(TelemetryConsent.DENIED) }
                    },
                    onOpenPrivacyPolicy = { context.openUrl(PRIVACY_POLICY_URL) }
                )
            }
            overlay()
        }
    }
}

/** The destinations a navigation rail offers once the window is wide enough to show one. */
private enum class RailDestination(
    val screen: Screen,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Home(Screen.Home, Icons.Default.Nightlight, R.string.nav_home),
    History(Screen.History, Icons.Default.History, R.string.nav_history),
    Settings(Screen.Settings, Icons.Default.Settings, R.string.nav_settings)
}
