package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.history.HistoryViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** History shows the entries and the sky from one read of ritual_logs, not two. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryViewModelSkyTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var container: AppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(preferencesFolder)
        database = container.database
        repository = container.ritualLogs
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `history reads its entries through the sky and nothing else`() = runTest(dispatcher) {
        repository.logRitual("An answer", MoodTag.SATISFIED, "Q")
        var logReads = 0
        val counting = object : RitualLogRepository by repository {
            override fun getLogs(): Flow<List<RitualLogEntry>> {
                logReads++
                return repository.getLogs()
            }
        }

        val viewModel = HistoryViewModel(counting, container.settings, container.progress)
        val state = viewModel.state.first { it.sky != null && it.logs.isNotEmpty() }

        assertEquals(0, logReads)
        assertEquals(1, state.totalCount)
        assertEquals("An answer", state.logs.single().selectedAnswer)
        assertEquals(1, state.moodCounts[MoodTag.SATISFIED])
    }
}
