package org.clockout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.RitualLogDao
import org.clockout.data.local.db.RitualLogEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualLogDaoTest {

    private lateinit var database: ClockoutDatabase
    private lateinit var dao: RitualLogDao

    private fun entry(date: String, mood: String = "CALM", timestamp: Long = 0L) = RitualLogEntity(
        date = date,
        questionText = "What went well today?",
        selectedAnswer = "The refactor landed",
        moodTagId = mood,
        timestamp = timestamp
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
    fun `upsert stores an entry that can be read back by date`() = runTest {
        dao.upsert(entry("2026-09-06"))

        val stored = dao.getByDate("2026-09-06")
        assertEquals("2026-09-06", stored?.date)
        assertEquals("CALM", stored?.moodTagId)
        assertEquals("The refactor landed", stored?.selectedAnswer)
    }

    @Test
    fun `logging the same day twice replaces the entry instead of duplicating it`() = runTest {
        dao.upsert(entry("2026-09-06", mood = "TIRED", timestamp = 1L))
        dao.upsert(entry("2026-09-06", mood = "SATISFIED", timestamp = 2L))

        assertEquals(1, dao.count())
        assertEquals("SATISFIED", dao.getByDate("2026-09-06")?.moodTagId)
        assertEquals(2L, dao.getByDate("2026-09-06")?.timestamp)
    }

    @Test
    fun `observeAll emits the newest day first`() = runTest {
        dao.upsertAll(
            listOf(entry("2026-09-04"), entry("2026-09-06"), entry("2026-09-05"))
        )

        val dates = dao.observeAll().first().map { it.date }
        assertEquals(listOf("2026-09-06", "2026-09-05", "2026-09-04"), dates)
    }

    @Test
    fun `deleteAll clears the table`() = runTest {
        dao.upsertAll(listOf(entry("2026-09-05"), entry("2026-09-06")))

        dao.deleteAll()

        assertEquals(0, dao.count())
        assertNull(dao.getByDate("2026-09-06"))
    }

    @Test
    fun `a nullable answer round trips`() = runTest {
        dao.upsert(entry("2026-09-06").copy(selectedAnswer = null))

        assertNull(dao.getByDate("2026-09-06")?.selectedAnswer)
    }
}
