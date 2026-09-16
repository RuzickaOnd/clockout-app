package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(language: AppLanguage) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(language = AppLanguage.ENGLISH),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {}
                    )
                }
            }
        }
    }

    @Test
    fun `settings follow the provider language`() {
        show(AppLanguage.CZECH)

        composeTestRule.onNodeWithText("Nastavení").assertIsDisplayed()
        composeTestRule.onNodeWithText("Čas konce pracovního dne").assertIsDisplayed()
    }

    @Test
    fun `the language section header is translated`() {
        show(AppLanguage.CZECH)

        // Scrolled to rather than asserted where it lands: the card sits far enough down the
        // screen that every section added above it would otherwise break this test.
        composeTestRule.onNodeWithText("Jazyk aplikace").performScrollTo().assertIsDisplayed()
    }
}
