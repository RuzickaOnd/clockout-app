package org.clockout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkyPolicy
import org.clockout.domain.model.TelemetryConsent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * The real Activity, recreated the way a rotation recreates it. The sky arrives from Room after the
 * first frame, so this is the path a restored star has to survive - a test that only swaps the
 * saved state of a ready screen does not walk it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkyActivityRecreationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SkyActivity>()

    private val monday: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Before
    fun seed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        RitualPreferences(context).setTelemetryConsent(TelemetryConsent.DENIED)
        ClockoutDatabase.getInstance(context).practiceDayDao().insert(PracticeDayEntity(monday.toString()))
    }

    /** The twinkle never goes idle, so frames are advanced by hand while Room answers. */
    private fun awaitTag(tag: String) {
        repeat(100) {
            if (composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
            composeTestRule.mainClock.advanceTimeBy(50)
            Thread.sleep(20)
        }
        throw AssertionError("$tag never appeared")
    }

    @Test
    fun `a tapped star's day card survives a rotation`() {
        composeTestRule.mainClock.autoAdvance = false
        awaitTag("sky_canvas")
        val month = YearMonth.from(monday.plusDays(3))
        val weeks = generateSequence(month.atDay(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY)).minusDays(3)) {
            it.plusWeeks(1)
        }.takeWhile { SkyPolicy.monthOf(it) == month }.toList()
        val slot = SkyLayout.slots(month.year, weeks.map(SkyPolicy::constellationNumber))[weeks.indexOf(monday)]
        val star = SkyLayout.project(slot, ConstellationCatalog.forWeek(SkyPolicy.constellationNumber(monday)).stars[0])
        composeTestRule.onNodeWithTag("sky_canvas").performTouchInput { click(Offset(star.x * width, star.y * height)) }
        awaitTag("sky_day_card")

        composeTestRule.activityRule.scenario.recreate()
        awaitTag("sky_canvas")

        awaitTag("sky_day_card")
    }
}
