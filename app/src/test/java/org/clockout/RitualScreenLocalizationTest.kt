package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStep
import org.clockout.domain.model.SkyTeaser
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RitualScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    @Test
    fun `the breathing step follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            language = AppLanguage.ENGLISH,
                            currentStep = RitualStep.Breathing,
                            question = question
                        ),
                        onIntent = {},
                        onFinish = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Vědomý dech 4s / 4s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Krok 2 ze 4").assertIsDisplayed()
    }

    @Test
    fun `the completion card names a newly reached stage`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            question = question,
                            currentStep = RitualStep.Log,
                            selectedMood = MoodTag.CALM,
                            isCompletedAndSaved = true,
                            reachedStage = PathStage.DOWN_THE_STAIRS
                        ),
                        onIntent = {},
                        onFinish = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Nový stupeň cesty").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Schody dolů").assertIsDisplayed()
    }

    @Test
    fun `the completion card names the constellation a full week lit`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            question = question, currentStep = RitualStep.Log, selectedMood = MoodTag.CALM,
                            isCompletedAndSaved = true, rhythmConstellationNumber = 10
                        ),
                        onIntent = {}, onFinish = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Tento týden jsi v rytmu. Na obloze se rozsvítilo souhvězdí: Konvice.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the completion card tells how far the first constellation is`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            question = question, currentStep = RitualStep.Log, selectedMood = MoodTag.CALM,
                            isCompletedAndSaved = true, skyTeaser = SkyTeaser(stars = 2, daysToFirstConstellation = 3)
                        ),
                        onIntent = {}, onFinish = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Na noční obloze přibyla hvězda", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2 hvězdy · ještě 3 uzavřené dny do tvého prvního souhvězdí")
            .performScrollTo().assertIsDisplayed()
    }
}
