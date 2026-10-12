package org.clockout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.domain.model.MoodTag
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The legacy import is guarded by a per-instance flag and mutex; two migrators would import the
 * pre-Room blob twice. The history and the path must therefore share one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SharedMigratorTest {

    @get:Rule val folder = TemporaryFolder()
    private var container: AppContainer? = null

    @After fun tearDown() { container?.database?.close() }

    @Test
    fun `a logged ritual reaches both the history and the path`() = runTest {
        val container = testAppContainer(folder).also { this@SharedMigratorTest.container = it }

        container.ritualLogs.logRitual("An answer", MoodTag.CALM, "Q")

        assertEquals(1, container.ritualLogs.getLogs().first().size)
        assertEquals(1, container.progress.getPathProgress().first().closedDays)
    }

    @Test
    fun `a legacy blob is imported once however the history and the path race for it`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = PreferenceDataStoreFactory.create { folder.newFile("legacy.preferences_pb").apply { delete() } }
        val legacyJson = """[{"date":"2026-09-07","questionText":"Q","selectedAnswer":"A","moodTag":"CALM","timestamp":1}]"""
        dataStore.edit { it[stringPreferencesKey("ritual_logs_json")] = legacyJson }
        val container = AppContainer(
            context = context,
            preferences = RitualPreferences(dataStore),
            database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build(),
            todayFlow = flowOf(LocalDate.now())
        ).also { this@SharedMigratorTest.container = it }

        val results = listOf(
            async { container.progress.getPathProgress().first().closedDays },
            async { container.ritualLogs.getLogs().first().size }
        ).awaitAll()

        assertEquals(1, results[0])
        assertEquals(1, results[1])
        assertEquals(1, container.database.ritualLogDao().count())
        assertEquals(1, container.progress.getPathProgress().first().closedDays)
    }
}
