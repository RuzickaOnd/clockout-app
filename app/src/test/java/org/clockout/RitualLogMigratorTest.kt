package org.clockout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.LegacyRitualLogSource
import org.clockout.data.local.RitualLogMigrator
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.RitualLogDao
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualLogMigratorTest {

    /** In-memory stand-in for the DataStore JSON blob written by pre-Room versions. */
    private class FakeLegacySource(
        private var logs: List<RitualLogEntry>,
        private var migrated: Boolean = false
    ) : LegacyRitualLogSource {
        var readCount = 0
            private set

        override suspend fun readLegacyLogs(): List<RitualLogEntry> {
            readCount++
            return logs
        }

        override suspend fun isLogsMigratedToRoom(): Boolean = migrated

        override suspend fun markLogsMigratedToRoom() {
            migrated = true
            logs = emptyList()
        }

        fun isMarkedMigrated(): Boolean = migrated
    }

    private lateinit var database: ClockoutDatabase
    private lateinit var dao: RitualLogDao

    private fun legacyEntry(date: String, mood: MoodTag = MoodTag.CALM) = RitualLogEntry(
        date = date,
        questionText = "What went well today?",
        selectedAnswer = "Shipped the migration",
        moodTag = mood,
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        dao = database.ritualLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `legacy logs are imported into the room table`() = runTest {
        val legacy = FakeLegacySource(
            listOf(legacyEntry("2026-09-05", MoodTag.TIRED), legacyEntry("2026-09-06", MoodTag.TENSE))
        )

        RitualLogMigrator(legacy, dao).ensureMigrated()

        assertEquals(2, dao.count())
        assertEquals("TIRED", dao.getByDate("2026-09-05")?.moodTagId)
        assertEquals("TENSE", dao.getByDate("2026-09-06")?.moodTagId)
        assertTrue(legacy.isMarkedMigrated())
    }

    @Test
    fun `import runs only once even when called repeatedly`() = runTest {
        val legacy = FakeLegacySource(listOf(legacyEntry("2026-09-06")))
        val migrator = RitualLogMigrator(legacy, dao)

        migrator.ensureMigrated()
        dao.deleteAll()
        migrator.ensureMigrated()

        assertEquals(0, dao.count())
        assertEquals(1, legacy.readCount)
    }

    @Test
    fun `an already migrated install does not re-read the legacy blob`() = runTest {
        val legacy = FakeLegacySource(listOf(legacyEntry("2026-09-06")), migrated = true)

        RitualLogMigrator(legacy, dao).ensureMigrated()

        assertEquals(0, dao.count())
        assertEquals(0, legacy.readCount)
    }

    @Test
    fun `an empty legacy blob still marks the install as migrated`() = runTest {
        val legacy = FakeLegacySource(emptyList())

        RitualLogMigrator(legacy, dao).ensureMigrated()

        assertEquals(0, dao.count())
        assertTrue(legacy.isMarkedMigrated())
        assertFalse(legacy.readLegacyLogs().isNotEmpty())
    }
}
