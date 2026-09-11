package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * After a cloud backup restore the sound metadata is back but the audio file is not. The entry
 * keeps its name so the user recognises what they had, and offers to upload it again rather
 * than pretending it can still be played.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class CustomSoundMissingFileUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val restored = CustomSound(
        id = CustomSound.ID_PREFIX + "restored",
        displayName = "Ocean night",
        fileName = "restored.mp3",
        durationMs = 154_000,
        addedAt = 0L,
        isAvailable = false
    )

    private val present = restored.copy(
        id = CustomSound.ID_PREFIX + "present",
        displayName = "Forest",
        fileName = "present.mp3",
        isAvailable = true
    )

    private fun showSoundMenu(
        sounds: List<CustomSound>,
        onSoundChoiceSelected: (SoundChoice) -> Unit = {}
    ) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(language = AppLanguage.CZECH, customSounds = sounds),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onSoundChoiceSelected = onSoundChoiceSelected,
                        onClearData = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("sound_style_dropdown_trigger").performClick()
    }

    @Test
    fun `a sound whose file is gone says so instead of showing its duration`() {
        showSoundMenu(listOf(restored))

        composeTestRule.onNodeWithText("Ocean night").assertIsDisplayed()
        composeTestRule.onNodeWithText("Soubor chybí – klepnutím ho nahraj znovu").assertIsDisplayed()
    }

    @Test
    fun `a sound whose file is there keeps its usual description`() {
        showSoundMenu(listOf(present))

        composeTestRule.onNodeWithText("Vlastní nahrávka (2:34) přehrávaná ve smyčce s jemným náběhem hlasitosti").assertIsDisplayed()
    }

    @Test
    fun `tapping a sound whose file is gone does not select it`() {
        var selected: SoundChoice? = null
        showSoundMenu(listOf(restored), onSoundChoiceSelected = { selected = it })

        composeTestRule.onNodeWithTag("sound_style_${restored.id}").performClick()

        assertNull(selected)
    }
}
