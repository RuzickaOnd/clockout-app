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
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.SkyPolicy
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
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

    /**
     * Closes [otherDays] other days of this week, plus a day a week earlier so this week is not the
     * first one and offers all its days. The days are picked so that, with today added, two of them
     * share a line of this week's figure - whatever weekday the test happens to run on.
     */
    private suspend fun seedThisWeek(otherDays: Int, sharingALine: Boolean = true): Boolean {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val todayIndex = today.dayOfWeek.value - 1
        val figure = ConstellationCatalog.forWeek(SkyPolicy.constellationNumber(monday))
        val others = (0 until 7).filter { it != todayIndex }
        val chosen = combinations(others, otherDays).firstOrNull { pick ->
            val closed = pick.toSet() + todayIndex
            figure.edges.any { (a, b) -> a in closed && b in closed } == sharingALine
        } ?: return false
        database.practiceDayDao().insertAll(
            (chosen.map { monday.plusDays(it.toLong()) } + monday.minusWeeks(1)).map { PracticeDayEntity(it.toString()) }
        )
        return true
    }

    private fun combinations(items: List<Int>, size: Int): List<List<Int>> =
        if (size == 0) listOf(emptyList())
        else items.indices.flatMap { i -> combinations(items.drop(i + 1), size - 1).map { listOf(items[i]) + it } }

    @Test
    fun `closing the day that fills the week lights this week's constellation`() = runTest(dispatcher) {
        seedThisWeek(otherDays = 3)

        val expected = SkyPolicy.constellationNumber(
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        )
        assertEquals(expected, finishRitual().state.value.rhythmConstellationNumber)
    }

    @Test
    fun `a ritual that leaves the week short lights nothing`() = runTest(dispatcher) {
        seedThisWeek(otherDays = 2)

        assertNull(finishRitual().state.value.rhythmConstellationNumber)
    }

    @Test
    fun `a week in rhythm without a single line announces no constellation`() = runTest(dispatcher) {
        // Not every figure has four stars without a line between them; then there is nothing to test.
        assumeTrue(seedThisWeek(otherDays = 3, sharingALine = false))

        assertNull(finishRitual().state.value.rhythmConstellationNumber)
    }
}
