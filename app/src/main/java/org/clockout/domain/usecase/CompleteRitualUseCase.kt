package org.clockout.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.RitualCompletion
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SkyPolicy
import org.clockout.domain.model.SkyTeaser
import org.clockout.domain.model.mondayOfWeek
import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import java.time.LocalDate

/**
 * Writes a finished ritual and works out what it changed: a new stage on the path, a constellation
 * lit for the first time, the teaser before the first one, and whether this is the moment to ask
 * for a review.
 */
class CompleteRitualUseCase(
    private val logs: RitualLogRepository,
    private val progress: ProgressRepository,
    private val shouldRequestReview: ShouldRequestReviewUseCase,
    private val today: () -> LocalDate = LocalDate::now
) {
    /**
     * [moodWasChosen] is false when the user skipped the step. Skipping still logs [MoodTag.CALM],
     * because the history needs something to show, but it is not the user saying the day went well
     * - so it must never be read as the good mood that earns a review request.
     */
    suspend operator fun invoke(
        answer: String?,
        mood: MoodTag,
        questionText: String,
        moodWasChosen: Boolean
    ): RitualCompletion {
        val before = progressSnapshot()
        logs.logRitual(answer, mood, questionText)
        val after = progressSnapshot()
        val thisMonday = today().mondayOfWeek()
        return RitualCompletion(
            reachedStage = if (before != null && after != null) {
                after.path.stage.takeIf { it != before.path.stage }
            } else {
                null
            },
            // Announced when this week's figure first gets a line, not merely when the week
            // reaches its rhythm: a first, partial week can be in rhythm on a lone star.
            litConstellationNumber = if (before != null && after != null) {
                SkyPolicy.constellationNumber(thisMonday).takeIf {
                    SkyPolicy.isLit(thisMonday, after.sky.days, after.sky.rhythmWeeks) &&
                        !SkyPolicy.isLit(thisMonday, before.sky.days, before.sky.rhythmWeeks)
                }
            } else {
                null
            },
            skyTeaser = after?.sky?.daysToFirstConstellation?.let { SkyTeaser(after.sky.summary.stars, it) },
            shouldRequestReview = moodWasChosen && shouldRequestReview(mood),
            daysClosedThisWeek = closedDaysThisWeek()
        )
    }

    /**
     * Read after the log is written, so the count includes today. It feeds nothing but the
     * completion event, so a failure here - like one reading the path or the sky - must not cost
     * the completion itself.
     */
    private suspend fun closedDaysThisWeek(): Int? = try {
        logs.getStreakInfo().first().completedThisWeekCount
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        null
    }

    private class ProgressSnapshot(val path: PathProgress, val sky: SkyData)

    /**
     * The path and the sky, read on either side of the log write to tell what this ritual changed.
     * They only decorate the completion card: whatever goes wrong while reading them, the ritual
     * itself must still be written, so a failure here yields no celebration rather than no entry.
     */
    private suspend fun progressSnapshot(): ProgressSnapshot? = try {
        ProgressSnapshot(progress.getPathProgress().first(), progress.getSky().first())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        null
    }
}
