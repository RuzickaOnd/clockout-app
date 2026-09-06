package org.clockout

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.update.AppUpdateBanner
import org.clockout.update.AppUpdateState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateBannerTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val readyText = context.getString(R.string.update_ready)
    private val restartText = context.getString(R.string.update_restart)

    @Test
    fun `nothing is offered while no update is waiting`() {
        composeTestRule.setContent {
            MyApplicationTheme { AppUpdateBanner(state = AppUpdateState.Idle, onInstall = {}) }
        }

        composeTestRule.onNodeWithText(readyText).assertDoesNotExist()
    }

    @Test
    fun `a downloaded update offers a restart`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AppUpdateBanner(state = AppUpdateState.ReadyToInstall, onInstall = {})
            }
        }

        composeTestRule.onNodeWithText(readyText).assertIsDisplayed()
    }

    @Test
    fun `tapping restart installs the update`() {
        var installed = false
        composeTestRule.setContent {
            MyApplicationTheme {
                AppUpdateBanner(
                    state = AppUpdateState.ReadyToInstall,
                    onInstall = { installed = true }
                )
            }
        }

        composeTestRule.onNodeWithText(restartText).performClick()

        assertTrue(installed)
    }
}
