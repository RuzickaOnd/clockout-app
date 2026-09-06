package org.clockout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the visual closure step in both lamp states. The bulb's halo breathes
 * forever, so the test clock is driven by hand instead of waiting for idle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ClosureStepScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = emptyList(),
        suggestedAnswersCs = emptyList()
    )

    private fun closingState(triggered: Boolean) = RitualState(
        language = AppLanguage.CZECH,
        currentStep = RitualStep.Closing,
        question = question,
        isClosureTriggered = triggered
    )

    private fun captureClosingStep(triggered: Boolean, fileName: String) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MyApplicationTheme {
                RitualScreen(
                    state = closingState(triggered),
                    onIntent = {},
                    onFinish = {}
                )
            }
        }
        // Past the 1100 ms switch-off animation and its trailing copy delays.
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$fileName")
    }

    @Test
    fun closure_lampOn() {
        captureClosingStep(triggered = false, fileName = "closure_lamp_on.png")
    }

    @Test
    fun closure_lampOff() {
        captureClosingStep(triggered = true, fileName = "closure_lamp_off.png")
    }
}
