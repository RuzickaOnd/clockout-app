package org.clockout

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.clockout.util.todayFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Today's date has to follow the system clock even after the phone slept through midnight, which a
 * delay-based timer does not: its clock stops in deep sleep. The system broadcasts do not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayFlowTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun datesAfter(broadcast: String): List<LocalDate> {
        var now = LocalDate.of(2026, 10, 9)
        val dates = mutableListOf<LocalDate>()
        runTest {
            val collecting = launch(UnconfinedTestDispatcher(testScheduler)) {
                context.todayFlow { now }.toList(dates)
            }
            shadowOf(Looper.getMainLooper()).idle()
            now = LocalDate.of(2026, 10, 10)
            context.sendBroadcast(Intent(broadcast))
            shadowOf(Looper.getMainLooper()).idle()
            collecting.cancel()
        }
        return dates
    }

    @Test
    fun `a new day is picked up from the date change broadcast`() {
        assertEquals(listOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)), datesAfter(Intent.ACTION_DATE_CHANGED))
    }

    @Test
    fun `a clock set by hand is picked up`() {
        assertEquals(listOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)), datesAfter(Intent.ACTION_TIME_CHANGED))
    }

    @Test
    fun `a new time zone is picked up`() {
        assertEquals(listOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)), datesAfter(Intent.ACTION_TIMEZONE_CHANGED))
    }
}
