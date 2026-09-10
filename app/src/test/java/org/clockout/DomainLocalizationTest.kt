package org.clockout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import org.clockout.presentation.util.AppLocalization
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.localizedDescription
import org.clockout.presentation.util.localizedLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DomainLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(language: AppLanguage, content: @Composable () -> Unit) {
        composeTestRule.setContent { AppLocalization(language) { content() } }
    }

    @Test
    fun `a mood is named in the app language`() {
        show(AppLanguage.CZECH) { Text(stringResource(MoodTag.CALM.labelRes)) }

        composeTestRule.onNodeWithText("Klidný").assertIsDisplayed()
    }

    @Test
    fun `every mood tag maps to its own label resource`() {
        show(AppLanguage.CZECH) {
            MoodTag.entries.forEach { mood -> Text(stringResource(mood.labelRes)) }
        }

        composeTestRule.onNodeWithText("Klidný").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unavený").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spokojený").assertIsDisplayed()
        composeTestRule.onNodeWithText("Napjatý").assertIsDisplayed()
    }

    @Test
    fun `a built-in sound is named in the app language`() {
        show(AppLanguage.ENGLISH) {
            Text(SoundChoice.BuiltIn(MeditationSoundStyle.TIBETAN_BOWL).localizedLabel())
        }

        composeTestRule.onNodeWithText("Tibetan Singing Bowl & Gong").assertIsDisplayed()
    }

    @Test
    fun `a custom sound keeps the file name and reports its duration`() {
        val sound = CustomSound(
            id = "custom:1",
            displayName = "Ocean night",
            fileName = "ocean.mp3",
            durationMs = 154_000L,
            addedAt = 0L
        )

        show(AppLanguage.CZECH) {
            Text(SoundChoice.Custom(sound).localizedLabel())
            Text(SoundChoice.Custom(sound).localizedDescription())
        }

        composeTestRule.onNodeWithText("Ocean night").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Vlastní nahrávka (2:34) přehrávaná ve smyčce s jemným náběhem hlasitosti")
            .assertIsDisplayed()
    }

    @Test
    fun `the system language option is translated but endonyms are not`() {
        show(AppLanguage.CZECH) {
            Text(LanguageOption.SYSTEM.localizedLabel())
            Text(LanguageOption.ENGLISH.localizedLabel())
            Text(LanguageOption.CZECH.localizedLabel())
        }

        composeTestRule.onNodeWithText("Dle systému").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Čeština").assertIsDisplayed()
    }

    @Test
    fun `the system language option is translated in english too`() {
        show(AppLanguage.ENGLISH) { Text(LanguageOption.SYSTEM.localizedLabel()) }

        composeTestRule.onNodeWithText("System default").assertIsDisplayed()
    }
}
