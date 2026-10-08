package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Switching "Ritual available anytime" off while the reminder sits outside the 16-21 window has
 * to be agreed to, because the reminder then moves to the default time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsRestrictWindowDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val toggled = mutableListOf<Boolean>()
    private val timesSet = mutableListOf<Pair<Int, Int>>()

    private fun setContent(hour: Int, minute: Int) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(
                            language = AppLanguage.ENGLISH,
                            notificationHour = hour,
                            notificationMinute = minute,
                            isTestModeEnabled = true
                        ),
                        onBack = {},
                        onTimeSelected = { h, m -> timesSet.add(h to m) },
                        onTestModeToggled = { toggled.add(it) },
                        onSoundToggled = {},
                        onClearData = {}
                    )
                }
            }
        }
    }

    private fun switchOff() {
        composeTestRule.onNodeWithTag("test_mode_switch").performScrollTo().performClick()
    }

    @Test
    fun `a reminder inside the window switches off without asking`() {
        setContent(hour = 20, minute = 30)

        switchOff()

        composeTestRule.onNodeWithTag("restrict_window_dialog").assertDoesNotExist()
        assertEquals(listOf(false), toggled)
        assertEquals(emptyList<Pair<Int, Int>>(), timesSet)
    }

    @Test
    fun `a reminder outside the window asks first and changes nothing yet`() {
        setContent(hour = 10, minute = 15)

        switchOff()

        composeTestRule.onNodeWithTag("restrict_window_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("10:15", substring = true).assertIsDisplayed()
        assertEquals(emptyList<Boolean>(), toggled)
        assertEquals(emptyList<Pair<Int, Int>>(), timesSet)
    }

    @Test
    fun `twenty one is already outside the window`() {
        setContent(hour = 21, minute = 0)

        switchOff()

        composeTestRule.onNodeWithTag("restrict_window_dialog").assertIsDisplayed()
    }

    @Test
    fun `confirming moves the reminder to the default and switches off`() {
        setContent(hour = 10, minute = 15)

        switchOff()
        composeTestRule.onNodeWithTag("restrict_window_confirm").performClick()

        assertEquals(listOf(17 to 30), timesSet)
        assertEquals(listOf(false), toggled)
        composeTestRule.onNodeWithTag("restrict_window_dialog").assertDoesNotExist()
    }

    @Test
    fun `cancelling leaves both the switch and the reminder alone`() {
        setContent(hour = 10, minute = 15)

        switchOff()
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithTag("restrict_window_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("test_mode_switch").assertIsOn()
        assertEquals(emptyList<Boolean>(), toggled)
        assertEquals(emptyList<Pair<Int, Int>>(), timesSet)
    }
}
