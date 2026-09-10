package org.clockout

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.util.AppLocalization
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Jazyk vybraný v aplikaci se může lišit od systémového locale, takže resources musí
 * jít o zvolený jazyk požádat i na zařízení nastaveném jinak.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LocalizationProviderTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `czech is served under a czech provider`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.app_subtitle))
            }
        }

        composeTestRule.onNodeWithText("Přechod práce → volno").assertIsDisplayed()
    }

    @Test
    fun `english is served under an english provider`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.app_subtitle))
            }
        }

        composeTestRule.onNodeWithText("Work → Leisure transition").assertIsDisplayed()
    }

    @Test
    fun `a nested provider wins over the one above it`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                AppLocalization(AppLanguage.CZECH) {
                    Text(stringResource(R.string.settings_title))
                }
            }
        }

        composeTestRule.onNodeWithText("Nastavení").assertIsDisplayed()
    }
}
