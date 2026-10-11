package org.clockout

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.domain.model.TelemetryConsent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Room answers after the first frame. Until then the path screen must show nothing rather than a
 * newcomer's path - a user eighty days in would otherwise see "Still at the desk" flash past.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PathActivityLoadingTest {

    private val composeTestRule = createAndroidComposeRule<PathActivity>()

    /** The days are stored before the Activity starts, so its very first frame already has data to wait for. */
    private val seed = TestRule { base, _: Description ->
        object : Statement() {
            override fun evaluate() {
                runBlocking {
                    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                    RitualPreferences(context).setTelemetryConsent(TelemetryConsent.DENIED)
                    val start = LocalDate.of(2026, 1, 5)
                    ClockoutDatabase.getInstance(context).practiceDayDao()
                        .insertAll((0L until 80L).map { PracticeDayEntity(start.plusDays(it).toString()) })
                }
                base.evaluate()
            }
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(composeTestRule)

    @Test
    fun `the path screen never shows a newcomer while it loads`() {
        composeTestRule.mainClock.autoAdvance = false
        var sawNewcomer = false
        var sawRealStage = false
        run frames@{
            repeat(100) {
                if (composeTestRule.onAllNodesWithText("Still at the Desk").fetchSemanticsNodes().isNotEmpty()) {
                    sawNewcomer = true
                }
                if (composeTestRule.onAllNodesWithText("Over the Bridge").fetchSemanticsNodes().isNotEmpty()) {
                    sawRealStage = true
                    return@frames
                }
                composeTestRule.mainClock.advanceTimeBy(16)
                Thread.sleep(10)
            }
        }
        assertTrue("the real stage never appeared", sawRealStage)
        assertTrue("a newcomer's path flashed past", !sawNewcomer)
    }
}
