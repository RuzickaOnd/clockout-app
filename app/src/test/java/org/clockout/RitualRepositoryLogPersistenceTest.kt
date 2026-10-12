package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.domain.model.MoodTag
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate

/**
 * Verifies the mood/day rating survives in the database rather than in a preferences blob:
 * a repository built over a fresh DAO instance still sees what an earlier one wrote.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualRepositoryLogPersistenceTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var container: AppContainer

    /** A new graph over the same storage, standing in for a repository built after the first one. */
    private fun repository() = AppContainer(
        context = context,
        preferences = container.preferences,
        database = container.database,
        todayFlow = flowOf(LocalDate.now())
    ).ritualRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = testAppContainer(preferencesFolder, context = context)
    }

    @After
    fun tearDown() {
        container.database.close()
    }

    @Test
    fun `a logged ritual is readable by a newly constructed repository`() = runTest {
        repository().logRitual(
            selectedAnswer = "Closed the last ticket",
            mood = MoodTag.SATISFIED,
            questionText = "What went well today?"
        )

        val logs = repository().getLogs().first()
        assertEquals(1, logs.size)
        assertEquals(MoodTag.SATISFIED, logs.first().moodTag)
        assertEquals("Closed the last ticket", logs.first().selectedAnswer)
    }

    @Test
    fun `logging twice on the same day keeps a single entry with the latest mood`() = runTest {
        val repository = repository()
        repository.logRitual(null, MoodTag.TENSE, "What went well today?")
        repository.logRitual("Better after a walk", MoodTag.CALM, "What went well today?")

        val logs = repository.getLogs().first()
        assertEquals(1, logs.size)
        assertEquals(MoodTag.CALM, logs.first().moodTag)
        assertEquals(repository.getTodayDateString(), logs.first().date)
    }

    @Test
    fun `clearLogs empties the history`() = runTest {
        val repository = repository()
        repository.logRitual(null, MoodTag.TIRED, "What went well today?")

        repository.clearLogs()

        assertTrue(repository.getLogs().first().isEmpty())
    }
}
