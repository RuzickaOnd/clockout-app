package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.presentation.settings.BreathingStyleScreen
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Settings card and the picker it opens. Between them they are the only way a user reaches
 * the breathing visual, so both the route in and the choice coming back out are pinned here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class BreathingStylePickerTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `the settings card names the style in use and opens the picker`() {
        var opened = 0
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(
                            language = AppLanguage.ENGLISH,
                            breathingStyle = BreathingStyle.AURORA
                        ),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onOpenBreathingStyle = { opened++ },
                        onClearData = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("breathing_style_card").performScrollTo().assertIsDisplayed()
        // The card has to say which style is live; a user should not have to open it to find out.
        composeTestRule.onNodeWithText("Waves").assertIsDisplayed()

        composeTestRule.onNodeWithTag("breathing_style_card").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `the picker offers every style and reports the one tapped`() {
        val chosen = mutableListOf<BreathingStyle>()
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    BreathingStyleScreen(
                        selectedStyle = BreathingStyle.CLASSIC_CIRCLE,
                        onStyleSelected = { chosen.add(it) },
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(500)

        // A style the picker cannot show is a style the user can never reach.
        BreathingStyle.entries.forEach { style ->
            composeTestRule.onNodeWithTag("breathing_style_${style.id}").performScrollTo().assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("breathing_style_${BreathingStyle.AURORA.id}").performClick()
        assertEquals(listOf(BreathingStyle.AURORA), chosen)
    }

    @Test
    fun `the picker always offers a way back, because no rail serves this screen`() {
        var back = 0
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    BreathingStyleScreen(
                        selectedStyle = BreathingStyle.CLASSIC_CIRCLE,
                        onStyleSelected = {},
                        onBack = { back++ }
                    )
                }
            }
        }
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithTag("breathing_style_back_button").performClick()
        assertTrue(back == 1)
    }
}
