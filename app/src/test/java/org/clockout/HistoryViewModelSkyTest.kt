package org.clockout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.presentation.history.HistoryViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** History shows the entries and the sky from one read of ritual_logs, not two. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryViewModelSkyTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        repository = RitualRepositoryImpl(
            preferences = RitualPreferences(
                PreferenceDataStoreFactory.create {
                    preferencesFolder.newFile("padla_history.preferences_pb").apply { delete() }
                }
            ),
            ritualLogDao = database.ritualLogDao(),
            practiceDayDao = database.practiceDayDao()
        )
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
        val counting = object : RitualRepository by repository {
            override fun getLogs(): Flow<List<RitualLogEntry>> {
                logReads++
                return repository.getLogs()
            }
        }

        val viewModel = HistoryViewModel(counting)
        val state = viewModel.state.first { it.sky != null && it.logs.isNotEmpty() }

        assertEquals(0, logReads)
        assertEquals(1, state.totalCount)
        assertEquals("An answer", state.logs.single().selectedAnswer)
        assertEquals(1, state.moodCounts[MoodTag.SATISFIED])
    }
}
