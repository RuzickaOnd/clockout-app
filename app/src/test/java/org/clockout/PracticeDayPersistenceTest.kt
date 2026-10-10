package org.clockout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
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
import java.time.LocalDate
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeDayPersistenceTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        repository = RitualRepositoryImpl(
            preferences = RitualPreferences(
                PreferenceDataStoreFactory.create {
                    preferencesFolder.newFile("padla_test.preferences_pb").apply { delete() }
                }
            ),
            ritualLogDao = database.ritualLogDao(),
            practiceDayDao = database.practiceDayDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `logging twice on the same day counts one practice day`() = runTest {
        repository.logRitual(null, MoodTag.CALM, "Q")
        repository.logRitual(null, MoodTag.TENSE, "Q")

        assertEquals(1, repository.getPathProgress().first().closedDays)
    }

    @Test
    fun `clearLogs keeps the practice days`() = runTest {
        repository.logRitual(null, MoodTag.CALM, "Q")

        repository.clearLogs()

        assertEquals(0, repository.getLogs().first().size)
        val progress = repository.getPathProgress().first()
        assertEquals(1, progress.closedDays)
        assertEquals(PathStage.LAPTOP_CLOSED, progress.stage)
    }

    @Test
    fun `earlier practice days add up with today`() = runTest {
        val today = LocalDate.now()
        database.practiceDayDao().insertAll(
            (1L..4L).map { PracticeDayEntity(today.minusDays(it * 7).toString()) }
        )

        repository.logRitual(null, MoodTag.CALM, "Q")

        assertEquals(PathStage.COAT_ON, repository.getPathProgress().first().stage)
    }

    @Test
    fun `an unparsable date is skipped rather than crashing`() = runTest {
        database.practiceDayDao().insert(PracticeDayEntity("not-a-date"))
        repository.logRitual(null, MoodTag.CALM, "Q")

        assertEquals(1, repository.getPathProgress().first().closedDays)
    }

    @Test
    fun `a date stored in locale digits still counts`() = runTest {
        // What SimpleDateFormat with Locale.getDefault() writes under an Arabic-Indic locale.
        database.practiceDayDao().insert(PracticeDayEntity("٢٠٢٦-١٠-٠٥"))

        assertEquals(1, repository.getPathProgress().first().closedDays)
    }

    @Test
    fun `a ritual logged under a locale with its own digits is stored in ISO digits`() = runTest {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("fa-IR"))
        try {
            val persianRepository = RitualRepositoryImpl(
                preferences = RitualPreferences(
                    PreferenceDataStoreFactory.create {
                        preferencesFolder.newFile("padla_fa.preferences_pb").apply { delete() }
                    }
                ),
                ritualLogDao = database.ritualLogDao(),
                practiceDayDao = database.practiceDayDao()
            )

            persianRepository.logRitual(null, MoodTag.CALM, "Q")
        } finally {
            Locale.setDefault(original)
        }

        assertEquals(listOf(LocalDate.now().toString()), database.practiceDayDao().observeAll().first())
    }

    @Test
    fun `the sky holds a logged day with its mood`() = runTest {
        repository.logRitual(null, MoodTag.SATISFIED, "Q")

        val sky = repository.getSky().first()
        assertEquals(1, sky.days.size)
        assertEquals(MoodTag.SATISFIED, sky.logs[LocalDate.now()]?.moodTag)
    }

    @Test
    fun `clearLogs keeps the sky but drops the moods`() = runTest {
        repository.logRitual(null, MoodTag.SATISFIED, "Q")

        repository.clearLogs()

        val sky = repository.getSky().first()
        assertEquals(setOf(LocalDate.now()), sky.days)
        assertTrue(sky.logs.isEmpty())
    }
}
