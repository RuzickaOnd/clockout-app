package org.clockout.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every screen rendered at the four window shapes the app has to survive: a phone held upright,
 * the same phone turned sideways (which is also what a tablet in split screen looks like), an
 * unfolded foldable and a tablet in landscape. The images are recorded at mdpi so a reference
 * costs a quarter of the pixels of a real device without changing the layout under test.
 *
 * Record with `./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"`,
 * check a change with `compareRoborazziDebug`.
 */
abstract class FormFactorScreenshotTest(private val device: String) {

    @get:Rule val composeTestRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeTestRule.captureScreen("$device/$name", content)
    }

    @Test fun homeOpen() = capture("home_open") {
        HomeScreen(state = Fixtures.homeOpen, onStartRitual = {}, onNavigateToHistory = {}, onNavigateToSettings = {})
    }

    @Test fun homeClosed() = capture("home_closed") {
        HomeScreen(state = Fixtures.homeClosed, onStartRitual = {}, onNavigateToHistory = {}, onNavigateToSettings = {})
    }

    @Test fun ritualQuestion() = capture("ritual_question") {
        RitualScreen(state = ritualState(RitualStep.Question), onIntent = {}, onFinish = {})
    }

    @Test fun ritualBreathing() = capture("ritual_breathing") {
        RitualScreen(state = ritualState(RitualStep.Breathing), onIntent = {}, onFinish = {})
    }

    @Test fun ritualClosing() = capture("ritual_closing") {
        RitualScreen(state = ritualState(RitualStep.Closing), onIntent = {}, onFinish = {})
    }

    @Test fun ritualLog() = capture("ritual_log") {
        RitualScreen(state = ritualState(RitualStep.Log), onIntent = {}, onFinish = {})
    }

    @Test fun history() = capture("history") {
        HistoryScreen(state = Fixtures.history, onBack = {}, onClearHistory = {})
    }

    @Test fun settings() = capture("settings") {
        SettingsScreen(
            state = Fixtures.settings,
            onBack = {},
            onLanguageOptionSelected = {},
            onNotificationToggled = {},
            onTimeSelected = { _, _ -> },
            onTestModeToggled = {},
            onIncludeWeekendsToggled = {},
            onSoundToggled = {},
            onSoundChoiceSelected = {},
            onTogglePreviewSound = {},
            onUploadCustomSound = {},
            onDeleteCustomSound = {},
            onDismissSoundImportError = {},
            onClearData = {}
        )
    }

    private fun ritualState(step: RitualStep) = RitualState(
        question = Fixtures.question,
        currentStep = step,
        selectedAnswer = if (step is RitualStep.Question) null else "Finished a big task"
    )
}

/**
 * The breathing halo and the closure bulb animate forever, so the clock is driven by hand to a
 * fixed point instead of waiting for an idle state that never arrives.
 */
internal fun ComposeContentTestRule.captureScreen(path: String, content: @Composable () -> Unit) {
    mainClock.autoAdvance = false
    setContent { MyApplicationTheme { content() } }
    mainClock.advanceTimeBy(2000)
    onRoot().captureRoboImage(filePath = "src/test/screenshots/formfactor/$path.png")
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-mdpi", sdk = [36])
class PhonePortraitScreenshotTest : FormFactorScreenshotTest("phone_portrait")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w914dp-h411dp-mdpi", sdk = [36])
class PhoneLandscapeScreenshotTest : FormFactorScreenshotTest("phone_landscape")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw600dp-w674dp-h841dp-mdpi", sdk = [36])
class FoldableScreenshotTest : FormFactorScreenshotTest("foldable")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw800dp-w1280dp-h800dp-mdpi", sdk = [36])
class TabletLandscapeScreenshotTest : FormFactorScreenshotTest("tablet_landscape")
