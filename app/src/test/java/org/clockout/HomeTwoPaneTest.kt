package org.clockout

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.clockout.presentation.home.HomeScreen
import org.clockout.screenshot.Fixtures
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The dashboard's cards sit one under the other on a phone and side by side once the window is
 * wide enough for two columns. Asserting on where the cards actually land keeps both halves of
 * that promise honest.
 */
abstract class HomeLayoutTest {

    @get:Rule val composeTestRule = createComposeRule()

    protected fun showHome() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    state = Fixtures.homeOpen,
                    onStartRitual = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {}
                )
            }
        }
    }

    protected fun ComposeContentTestRule.heroBounds() =
        onNodeWithTag("home_hero_card").getBoundsInRoot()

    protected fun ComposeContentTestRule.weekBounds() =
        onNodeWithTag("weekly_streak_card").getBoundsInRoot()
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-mdpi", sdk = [36])
class HomeSingleColumnTest : HomeLayoutTest() {

    @Test
    fun `a phone stacks the week card under the hero card`() {
        showHome()

        val hero = composeTestRule.heroBounds()
        val week = composeTestRule.weekBounds()

        assertTrue(
            "the week card should sit below the hero card, got hero=$hero week=$week",
            week.top >= hero.bottom
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw800dp-w1280dp-h800dp-mdpi", sdk = [36])
class HomeTwoPaneTest : HomeLayoutTest() {

    @Test
    fun `a wide window puts the week card beside the hero card`() {
        showHome()

        val hero = composeTestRule.heroBounds()
        val week = composeTestRule.weekBounds()

        assertTrue(
            "the week card should sit beside the hero card, got hero=$hero week=$week",
            week.left >= hero.right
        )
    }
}
