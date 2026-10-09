package org.clockout

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.local.db.PracticeDayEntity
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualPathCelebrationTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: ClockoutDatabase
    private lateinit var preferences: RitualPreferences
    private lateinit var repository: RitualRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        preferences = RitualPreferences(
            PreferenceDataStoreFactory.create {
                preferencesFolder.newFile("padla_test.preferences_pb").apply { delete() }
            }
        )
        repository = RitualRepositoryImpl(
            preferences = preferences,
            ritualLogDao = database.ritualLogDao(),
            practiceDayDao = database.practiceDayDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = RitualViewModel(
        appContext = context,
        getTodayRitualUseCase = GetTodayRitualUseCase(repository),
        logMoodUseCase = LogMoodUseCase(repository),
        shouldRequestReviewUseCase = ShouldRequestReviewUseCase(repository),
        ritualRepository = repository
    )

    private suspend fun finishRitual(): RitualViewModel = viewModel().also { vm ->
        vm.onIntent(RitualIntent.MoodSelected(MoodTag.CALM))
        vm.state.first { it.isCompletedAndSaved }
    }

    @Test
    fun `the first ritual ever reaches the first stage`() = runTest(dispatcher) {
        assertEquals(PathStage.LAPTOP_CLOSED, finishRitual().state.value.reachedStage)
    }

    @Test
    fun `a second ritual on the same day celebrates nothing`() = runTest(dispatcher) {
        finishRitual()

        assertNull(finishRitual().state.value.reachedStage)
    }

    /** A day a week earlier, so this week is not the first one and offers all seven days. */
    private suspend fun seedThisWeek(otherDays: Int) {
        preferences.setIncludeWeekends(true)
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val days = (0L until 7L).map { monday.plusDays(it) }.filter { it != today }.take(otherDays)
        database.practiceDayDao().insertAll(
            (days + monday.minusWeeks(1)).map { PracticeDayEntity(it.toString()) }
        )
    }

    @Test
    fun `closing the day that fills the week marks the rhythm`() = runTest(dispatcher) {
        seedThisWeek(otherDays = 3)

        assertTrue(finishRitual().state.value.rhythmWeekReached)
    }

    @Test
    fun `a ritual that leaves the week short marks no rhythm`() = runTest(dispatcher) {
        seedThisWeek(otherDays = 2)

        assertFalse(finishRitual().state.value.rhythmWeekReached)
    }
}
