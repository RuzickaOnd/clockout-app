package org.clockout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.local.db.RitualLogEntity
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeDayPersistenceTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var container: AppContainer
    private lateinit var database: ClockoutDatabase
    private lateinit var preferences: RitualPreferences
    private lateinit var logRepository: RitualLogRepository
    private lateinit var progressRepository: ProgressRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        container = testAppContainer(preferencesFolder)
        database = container.database
        logRepository = container.ritualLogs
        progressRepository = container.progress
        preferences = container.preferences
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `logging twice on the same day counts one practice day`() = runTest {
        logRepository.logRitual(null, MoodTag.CALM, "Q")
        logRepository.logRitual(null, MoodTag.TENSE, "Q")

        assertEquals(1, progressRepository.getPathProgress().first().closedDays)
    }

    @Test
    fun `clearLogs keeps the practice days`() = runTest {
        logRepository.logRitual(null, MoodTag.CALM, "Q")

        logRepository.clearLogs()

        assertEquals(0, logRepository.getLogs().first().size)
        val progress = progressRepository.getPathProgress().first()
        assertEquals(1, progress.closedDays)
        assertEquals(PathStage.LAPTOP_CLOSED, progress.stage)
    }

    @Test
    fun `earlier practice days add up with today`() = runTest {
        val today = LocalDate.now()
        database.practiceDayDao().insertAll(
            (1L..4L).map { PracticeDayEntity(today.minusDays(it * 7).toString()) }
        )

        logRepository.logRitual(null, MoodTag.CALM, "Q")

        assertEquals(PathStage.COAT_ON, progressRepository.getPathProgress().first().stage)
    }

    @Test
    fun `an unparsable date is skipped rather than crashing`() = runTest {
        database.practiceDayDao().insert(PracticeDayEntity("not-a-date"))
        logRepository.logRitual(null, MoodTag.CALM, "Q")

        assertEquals(1, progressRepository.getPathProgress().first().closedDays)
    }

    @Test
    fun `a date stored in locale digits still counts`() = runTest {
        // What SimpleDateFormat with Locale.getDefault() writes under an Arabic-Indic locale.
        database.practiceDayDao().insert(PracticeDayEntity("٢٠٢٦-١٠-٠٥"))

        assertEquals(1, progressRepository.getPathProgress().first().closedDays)
    }

    @Test
    fun `a ritual logged under a locale with its own digits is stored in ISO digits`() = runTest {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("fa-IR"))
        try {
            val persianRepository = AppContainer(
                context = ApplicationProvider.getApplicationContext(),
                preferences = RitualPreferences(
                    PreferenceDataStoreFactory.create {
                        preferencesFolder.newFile("padla_fa.preferences_pb").apply { delete() }
                    }
                ),
                database = database,
                todayFlow = flowOf(LocalDate.now())
            ).ritualLogs

            persianRepository.logRitual(null, MoodTag.CALM, "Q")
        } finally {
            Locale.setDefault(original)
        }

        assertEquals(listOf(LocalDate.now().toString()), database.practiceDayDao().observeAll().first())
    }

    @Test
    fun `the sky holds a logged day with its mood`() = runTest {
        logRepository.logRitual(null, MoodTag.SATISFIED, "Q")

        val sky = progressRepository.getSky().first()
        assertEquals(1, sky.days.size)
        assertEquals(MoodTag.SATISFIED, sky.logs[LocalDate.now()]?.moodTag)
    }

    @Test
    fun `clearLogs keeps the sky but drops the moods`() = runTest {
        logRepository.logRitual(null, MoodTag.SATISFIED, "Q")

        logRepository.clearLogs()

        val sky = progressRepository.getSky().first()
        assertEquals(setOf(LocalDate.now()), sky.days)
        assertTrue(sky.logs.isEmpty())
    }

    @Test
    fun `the sky and the path move on at midnight without a new ritual`() = runTest {
        val today = MutableStateFlow(LocalDate.of(2026, 10, 9))
        val clocked = AppContainer(
            context = ApplicationProvider.getApplicationContext(),
            preferences = RitualPreferences(
                PreferenceDataStoreFactory.create {
                    preferencesFolder.newFile("padla_clock.preferences_pb").apply { delete() }
                }
            ),
            database = database,
            todayFlow = today
        ).progress
        database.practiceDayDao().insert(PracticeDayEntity("2026-10-09"))
        assertEquals(LocalDate.of(2026, 10, 9), clocked.getSky().first().today)

        today.value = LocalDate.of(2026, 10, 10)

        assertEquals(LocalDate.of(2026, 10, 10), clocked.getSky().first().today)
    }

    @Test
    fun `the sky knows whether the ritual is offered at weekends`() = runTest {
        assertEquals(false, progressRepository.getSky().first().includeWeekends)

        preferences.setIncludeWeekends(true)

        assertEquals(true, progressRepository.getSky().first { it.includeWeekends }.includeWeekends)
    }

    @Test
    fun `a closed day writes its entry and its practice day together`() = runTest {
        val entry = RitualLogEntity("2026-10-09", "Q", null, "CALM", 1L)

        database.ritualLogDao().logDay(entry, PracticeDayEntity("2026-10-09"))

        assertEquals("CALM", database.ritualLogDao().getByDate("2026-10-09")?.moodTagId)
        assertEquals(listOf("2026-10-09"), database.practiceDayDao().observeAll().first())
    }
}
