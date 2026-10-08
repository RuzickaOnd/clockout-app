package org.clockout

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
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

/** The last chip in the workday end time row, which opens a time picker instead of a preset. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsCustomTimeChipTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val picked = mutableListOf<Pair<Int, Int>>()

    private fun setContent(
        hour: Int,
        minute: Int,
        customTime: Pair<Int, Int>?,
        language: AppLanguage = AppLanguage.ENGLISH,
        anytime: Boolean = true
    ) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(
                            language = language,
                            isNotificationEnabled = true,
                            notificationHour = hour,
                            notificationMinute = minute,
                            customNotificationTime = customTime,
                            isTestModeEnabled = anytime
                        ),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onCustomTimeSelected = { h, m -> picked.add(h to m) },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {}
                    )
                }
            }
        }
    }

    @Test
    fun `without a custom time the chip reads Custom`() {
        setContent(hour = 17, minute = 30, customTime = null)

        composeTestRule.onNodeWithTag("custom_time_chip", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeTestRule.onNodeWithText("Custom").assertIsDisplayed()
    }

    @Test
    fun `the Czech placeholder reads Vlastni`() {
        setContent(hour = 17, minute = 30, customTime = null, language = AppLanguage.CZECH)

        composeTestRule.onNodeWithText("Vlastní").assertIsDisplayed()
    }

    @Test
    fun `a stored custom time stays on the chip while a preset is selected`() {
        setContent(hour = 17, minute = 0, customTime = 20 to 15)

        composeTestRule.onNodeWithTag("custom_time_chip").assertIsNotSelected()
        composeTestRule.onNodeWithTag("custom_time_chip").assert(hasText("20:15"))
        composeTestRule.onNodeWithTag("preset_time_17_0").assertIsSelected()
    }

    @Test
    fun `the chip is selected while the custom time is the active one`() {
        setContent(hour = 20, minute = 15, customTime = 20 to 15)

        composeTestRule.onNodeWithTag("custom_time_chip").assertIsSelected()
    }

    @Test
    fun `reopening the picker offers the earlier time, so confirming alone keeps it`() {
        setContent(hour = 17, minute = 0, customTime = 20 to 15)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_time_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_time_confirm").performClick()

        assertEquals(listOf(20 to 15), picked)
        composeTestRule.onNodeWithTag("custom_time_dialog").assertDoesNotExist()
    }

    @Test
    fun `the first pick starts from the active time`() {
        setContent(hour = 18, minute = 30, customTime = null)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_time_confirm").performClick()

        assertEquals(listOf(18 to 30), picked)
    }

    @Test
    fun `cancelling the picker changes nothing`() {
        setContent(hour = 17, minute = 0, customTime = 20 to 15)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<Pair<Int, Int>>(), picked)
        composeTestRule.onNodeWithTag("custom_time_dialog").assertDoesNotExist()
    }

    @Test
    fun `with the ritual available anytime the picker names no range`() {
        setContent(hour = 17, minute = 0, customTime = 9 to 45)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_time_window_hint").assertDoesNotExist()
        composeTestRule.onNodeWithTag("custom_time_confirm").performClick()

        assertEquals(listOf(9 to 45), picked)
    }

    @Test
    fun `outside anytime mode the picker opens inside the window and names it`() {
        setContent(hour = 17, minute = 0, customTime = 9 to 45, anytime = false)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_time_window_hint")
            .assertIsDisplayed()
            .assert(hasText("16:00", substring = true))
            .assert(hasText("20:59", substring = true))
        composeTestRule.onNodeWithTag("custom_time_confirm").assertIsEnabled().performClick()

        assertEquals(listOf(16 to 45), picked)
    }

    @Test
    fun `outside anytime mode a time past the window cannot be confirmed`() {
        setContent(hour = 17, minute = 0, customTime = 18 to 0, anytime = false)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("custom_time_mode_toggle").performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("22")

        composeTestRule.onNodeWithTag("custom_time_confirm").assertIsNotEnabled()
    }

    @Test
    fun `the picker dialog keeps to the picker's width instead of spanning the window`() {
        setContent(hour = 17, minute = 0, customTime = 20 to 15, anytime = false)

        composeTestRule.onNodeWithTag("custom_time_chip").performScrollTo().performClick()

        // The range hint once measured itself against the whole window and dragged the dialog
        // edge to edge with it; the dialog is now as wide as the dial plus its own padding.
        val window = composeTestRule.onAllNodes(isRoot())[0].fetchSemanticsNode().size.width
        val dialog = composeTestRule.onNodeWithTag("custom_time_dialog").fetchSemanticsNode().size.width
        assertTrue("dialog $dialog px of a $window px window", dialog < window * 0.95f)
    }
}
