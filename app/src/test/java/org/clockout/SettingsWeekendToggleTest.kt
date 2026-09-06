package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The settings section that decides whether the weekend takes part in the ritual. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsWeekendToggleTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val toggled = mutableListOf<Boolean>()

    private fun setContent(includeWeekends: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(
                    state = SettingsState(
                        language = AppLanguage.ENGLISH,
                        includeWeekends = includeWeekends
                    ),
                    onBack = {},
                    onTimeSelected = { _, _ -> },
                    onTestModeToggled = {},
                    onIncludeWeekendsToggled = { toggled.add(it) },
                    onSoundToggled = {},
                    onClearData = {}
                )
            }
        }
    }

    @Test
    fun `the switch is off while weekends are excluded`() {
        setContent(includeWeekends = false)

        composeTestRule.onNodeWithTag("include_weekends_switch").assertIsDisplayed().assertIsOff()
    }

    @Test
    fun `the switch is on while weekends are included`() {
        setContent(includeWeekends = true)

        composeTestRule.onNodeWithTag("include_weekends_switch").assertIsOn()
    }

    @Test
    fun `turning the switch on reports the change`() {
        setContent(includeWeekends = false)

        composeTestRule.onNodeWithTag("include_weekends_switch").performClick()

        assertEquals(listOf(true), toggled)
    }
}
