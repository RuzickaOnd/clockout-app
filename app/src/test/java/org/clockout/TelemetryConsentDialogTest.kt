package org.clockout

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.clockout.data.local.RitualPreferences
import org.clockout.domain.model.TelemetryConsent
import org.clockout.presentation.PadlaApp
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The opt-in itself, against the real app root and the real DataStore.
 *
 * Deliberately not seeding consent the way the navigation captures do: this is the one test whose
 * subject is the unanswered state. Each Robolectric class gets its own sandbox, so the store starts
 * empty here even though it is the process-wide one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-mdpi", sdk = [36])
class TelemetryConsentDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val preferences: RitualPreferences
        get() = RitualPreferences(ApplicationProvider.getApplicationContext<Context>())

    /**
     * The DataStore behind [PadlaApp] is the process-wide one, shared by every test in this class,
     * so an answer written by whichever test ran first would leave the others with no dialog to
     * find. Put back to unanswered before each.
     */
    @Before
    fun clearConsent() = runBlocking {
        preferences.setTelemetryConsent(TelemetryConsent.UNKNOWN)
    }

    private fun showApp() {
        composeTestRule.setContent { MyApplicationTheme { PadlaApp() } }
    }

    @Test
    fun `a fresh install is asked before anything is collected`() {
        showApp()

        composeTestRule.onNodeWithTag("telemetry_consent_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("telemetry_consent_allow").assertIsDisplayed()
        composeTestRule.onNodeWithTag("telemetry_consent_deny").assertIsDisplayed()
    }

    @Test
    fun `allowing records the grant and closes the question`() {
        showApp()

        composeTestRule.onNodeWithTag("telemetry_consent_allow").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            TelemetryConsent.GRANTED,
            runBlocking { preferences.telemetryConsentFlow.first { it != TelemetryConsent.UNKNOWN } }
        )
        composeTestRule.onNodeWithTag("telemetry_consent_dialog").assertDoesNotExist()
    }

    @Test
    fun `declining is remembered too, so the question is not asked again`() {
        showApp()

        composeTestRule.onNodeWithTag("telemetry_consent_deny").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            TelemetryConsent.DENIED,
            runBlocking { preferences.telemetryConsentFlow.first { it != TelemetryConsent.UNKNOWN } }
        )
        composeTestRule.onNodeWithTag("telemetry_consent_dialog").assertDoesNotExist()
    }
}
