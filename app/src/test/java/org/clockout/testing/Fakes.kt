package org.clockout.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.RitualStreakInfo
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.SoundImportResult
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FakeReminderScheduler : ReminderScheduler {
    // Written from DataStore's thread and polled from the test's, so both need cross-thread visibility.
    private val _scheduled = CopyOnWriteArrayList<Pair<Int, Int>>()
    private val cancels = AtomicInteger()
    val scheduled: List<Pair<Int, Int>> get() = _scheduled
    val cancelCount: Int get() = cancels.get()
    override fun schedule(hour: Int, minute: Int) { _scheduled += hour to minute }
    override fun cancel() { cancels.incrementAndGet() }
}

class FakeSoundPlayer : SoundPlayer {
    var ambient: SoundChoice? = null
    var stopCount = 0
    override fun startAmbient(choice: SoundChoice) { ambient = choice }
    override fun triggerBowlStrike(highPitch: Boolean) = Unit
    override fun playPreview(choice: SoundChoice, onFinished: () -> Unit) { ambient = choice }
    override fun stopAmbient() { ambient = null; stopCount++ }
}

class FakeSoundFileStore(
    var onImport: (existingCount: Int) -> SoundImportResult
) : SoundFileStore {
    val deleted = mutableListOf<CustomSound>()
    override suspend fun import(source: String, existingCount: Int) = onImport(existingCount)
    override suspend fun delete(sound: CustomSound) { deleted += sound }
}

class FakeRitualLogRepository(
    /** The day [logRitual] reports having written under. */
    var loggedDay: LocalDate = LocalDate.now(),
    var completedCount: Int = 0,
    var reviewShown: Boolean = false
) : RitualLogRepository {
    val logged = mutableListOf<Triple<String?, MoodTag, String>>()
    private val logs = MutableStateFlow<List<RitualLogEntry>>(emptyList())
    override fun getTodayDateString() = LocalDate.now().toString()
    override fun getLogs(): Flow<List<RitualLogEntry>> = logs
    override fun getStreakInfo(): Flow<RitualStreakInfo> = flowOf(
        RitualStreakInfo(
            completedThisWeekCount = logged.size,
            targetWorkDays = 5,
            isTodayCompleted = logged.isNotEmpty(),
            todayLog = null,
            weekDays = emptyList()
        )
    )
    override suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String): LocalDate {
        logged += Triple(selectedAnswer, mood, questionText)
        completedCount++
        return loggedDay
    }
    override suspend fun getCompletedRitualCount() = completedCount
    override suspend fun isReviewPromptShown() = reviewShown
    override suspend fun markReviewPromptShown() { reviewShown = true }
    override suspend fun clearLogs() = Unit
}

/** Answers with the snapshots in turn: the first read is "before" the log, the second "after". */
class FakeProgressRepository(
    private val paths: ArrayDeque<PathProgress>,
    private val skies: ArrayDeque<SkyData>,
    private val failure: Exception? = null
) : ProgressRepository {
    override fun getPathProgress(): Flow<PathProgress> = flow { failure?.let { throw it }; emit(paths.removeFirst()) }
    override fun getSky(): Flow<SkyData> = flow { failure?.let { throw it }; emit(skies.removeFirst()) }
}
