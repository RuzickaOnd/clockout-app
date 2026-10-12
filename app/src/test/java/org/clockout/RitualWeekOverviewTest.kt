package org.clockout

import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The week strip on the home screen only shows the days the ritual is offered on, so the
 * "x of N days closed" summary counts against the same set of days.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualWeekOverviewTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: AppContainer
    private val preferences get() = container.preferences

    private fun repository() = container.ritualLogs

    @Before
    fun setUp() {
        container = testAppContainer(preferencesFolder)
    }

    @After
    fun tearDown() {
        container.database.close()
    }

    @Test
    fun `excluding weekends leaves monday to friday in the week strip`() = runTest {
        preferences.setIncludeWeekends(false)

        val streakInfo = repository().getStreakInfo().first()

        assertEquals(5, streakInfo.weekDays.size)
        assertEquals(5, streakInfo.targetWorkDays)
        // dayIndex is Monday-relative (0 = Monday), matching the order of day_names_short.
        assertEquals(listOf(0, 1, 2, 3, 4), streakInfo.weekDays.map { it.dayIndex })
    }

    @Test
    fun `including weekends adds saturday and sunday to the week strip`() = runTest {
        preferences.setIncludeWeekends(true)

        val streakInfo = repository().getStreakInfo().first()

        assertEquals(7, streakInfo.weekDays.size)
        assertEquals(7, streakInfo.targetWorkDays)
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6),
            streakInfo.weekDays.map { it.dayIndex }
        )
    }
}
