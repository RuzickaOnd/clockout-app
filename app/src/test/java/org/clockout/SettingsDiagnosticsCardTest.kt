package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.TelemetryConsent
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
 * The diagnostics card is the only way back from a "no" in the opt-in dialog, so it has to be
 * reachable and it has to show the answer the user actually gave. It sits below the fold, which is
 * why the settings screenshot baseline does not contain it and this test does the pinning instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsDiagnosticsCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val toggles = mutableListOf<Boolean>()
    private var privacyPolicyOpened = 0

    private fun show(language: AppLanguage, consent: TelemetryConsent) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(language = language, telemetryConsent = consent),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onTelemetryConsentToggled = { toggles += it },
                        onOpenPrivacyPolicy = { privacyPolicyOpened++ },
                        onClearData = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("diagnostics_card").performScrollTo()
    }

    @Test
    fun `the card is offered in Czech`() {
        show(AppLanguage.CZECH, TelemetryConsent.UNKNOWN)

        composeTestRule.onNodeWithText("Anonymní diagnostika").assertIsDisplayed()
        composeTestRule.onNodeWithTag("privacy_policy_button").assertIsDisplayed()
    }

    @Test
    fun `the card is offered in English`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.UNKNOWN)

        composeTestRule.onNodeWithText("Anonymous diagnostics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("privacy_policy_button").assertIsDisplayed()
    }

    @Test
    fun `an unanswered install shows the switch off`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.UNKNOWN)

        composeTestRule.onNodeWithTag("telemetry_consent_switch").assertIsOff()
    }

    @Test
    fun `a declined install shows the switch off`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.DENIED)

        composeTestRule.onNodeWithTag("telemetry_consent_switch").assertIsOff()
    }

    @Test
    fun `a granted install shows the switch on`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.GRANTED)

        composeTestRule.onNodeWithTag("telemetry_consent_switch").assertIsOn()
    }

    @Test
    fun `withdrawing consent reports false`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.GRANTED)

        composeTestRule.onNodeWithTag("telemetry_consent_switch").performClick()

        assertEquals(listOf(false), toggles)
    }

    @Test
    fun `the policy button asks the host to open it`() {
        show(AppLanguage.ENGLISH, TelemetryConsent.DENIED)

        composeTestRule.onNodeWithTag("privacy_policy_button").performClick()

        assertEquals(1, privacyPolicyOpened)
    }
}
