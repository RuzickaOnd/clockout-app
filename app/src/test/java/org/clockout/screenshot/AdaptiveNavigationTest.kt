package org.clockout.screenshot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.onRoot
import org.clockout.presentation.PadlaApp
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The two secondary destinations live in the top bar on a phone and on a navigation rail from a
 * medium window upwards. They must never appear in both places at once, and a phone must keep
 * exactly what it had before the rail existed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-mdpi", sdk = [36])
class PhoneNavigationTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `a phone keeps the history and settings icons in the top bar and shows no rail`() {
        composeTestRule.setContent { MyApplicationTheme { PadlaApp() } }

        composeTestRule.onNodeWithTag("nav_history_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_settings_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertDoesNotExist()

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/formfactor/phone_portrait/app_no_rail.png"
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw800dp-w1280dp-h800dp-mdpi", sdk = [36])
class TabletNavigationTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `a tablet moves the destinations onto a rail and drops them from the top bar`() {
        composeTestRule.setContent { MyApplicationTheme { PadlaApp() } }

        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_history_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_settings_button").assertDoesNotExist()

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/formfactor/tablet_landscape/app_with_rail.png"
        )
    }

    @Test
    fun `the ritual keeps the whole window and hides the rail`() {
        composeTestRule.setContent { MyApplicationTheme { PadlaApp() } }

        composeTestRule.onNodeWithTag("start_ritual_button").performClick()

        composeTestRule.onNodeWithText("History").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ritual_close_button").assertIsDisplayed()
    }
}
