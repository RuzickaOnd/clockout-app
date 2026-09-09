package org.clockout

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A phone turned sideways, a tablet in split screen and a half-opened foldable all leave the
 * ritual about 400 dp of height - less than its content needs. Every step must still let the
 * user reach the control that moves the ritual forward, otherwise the ritual cannot be finished
 * on those windows at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w914dp-h411dp-mdpi", sdk = [36])
class RitualShortWindowTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q1",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished a big task", "Helped a colleague", "Learned something new"),
        suggestedAnswersCs = listOf("Dokončil jsem velký úkol", "Pomohl jsem kolegovi", "Naučil jsem se něco nového")
    )

    private val intents = mutableListOf<RitualIntent>()

    private fun setStep(step: RitualStep, isClosureTriggered: Boolean = false) {
        composeTestRule.setContent {
            MyApplicationTheme {
                RitualScreen(
                    state = RitualState(
                        language = AppLanguage.ENGLISH,
                        question = question,
                        currentStep = step,
                        isClosureTriggered = isClosureTriggered
                    ),
                    onIntent = { intents += it },
                    onFinish = {}
                )
            }
        }
    }

    private fun reach(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun `the question step lets the user reach the continue button`() {
        setStep(RitualStep.Question)
        reach("proceed_to_breathing_button")
    }

    @Test
    fun `the breathing step lets the user reach the skip button and use it`() {
        setStep(RitualStep.Breathing)
        reach("finish_breathing_button")
        composeTestRule.onNodeWithTag("finish_breathing_button").performClick()
        assertTrue(intents.any { it is RitualIntent.BreathingFinished })
    }

    @Test
    fun `the closing step lets the user reach the lamp and the continue button`() {
        setStep(RitualStep.Closing, isClosureTriggered = true)
        reach("closure_card")
        reach("proceed_to_mood_button")
    }

    @Test
    fun `the mood step lets the user reach every mood and the skip button`() {
        setStep(RitualStep.Log)
        MoodTag.entries.forEach { mood -> reach("mood_card_${mood.id}") }
        reach("skip_mood_log_button")
        composeTestRule.onNodeWithTag("mood_card_${MoodTag.TENSE.id}").performScrollTo().performClick()
        assertTrue(intents.any { it is RitualIntent.MoodSelected && it.mood == MoodTag.TENSE })
    }
}
