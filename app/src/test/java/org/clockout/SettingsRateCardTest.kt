package org.clockout

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsRateCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(language: AppLanguage) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(language = language),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {}
                    )
                }
            }
        }
        // The card sits near the bottom of a long scrolling column.
        composeTestRule.onNodeWithTag("rate_app_card").performScrollTo()
    }

    @Test
    fun `the card is offered in Czech`() {
        show(AppLanguage.CZECH)

        composeTestRule.onNodeWithText("Ohodnotit aplikaci").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rate_app_button").assertIsDisplayed()
    }

    @Test
    fun `the card is offered in English`() {
        show(AppLanguage.ENGLISH)

        composeTestRule.onNodeWithText("Rate the app").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rate_app_button").assertIsDisplayed()
    }

    @Test
    fun `the button sends the user to this app's Play Store listing`() {
        show(AppLanguage.ENGLISH)

        composeTestRule.onNodeWithTag("rate_app_button").performClick()

        val launched = shadowOf(
            ApplicationProvider.getApplicationContext<android.app.Application>()
        ).nextStartedActivity
        assertNotNull("no activity was started", launched)
        assertEquals(Intent.ACTION_VIEW, launched.action)
        assertEquals("market://details?id=org.clockout", launched.data.toString())
    }
}
