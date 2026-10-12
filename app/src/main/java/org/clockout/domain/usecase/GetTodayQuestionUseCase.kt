package org.clockout.domain.usecase

import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.RitualQuestion
import java.time.LocalDate

/** Today's question from the yearly rotation; the clock is a parameter so a test can pin the day. */
class GetTodayQuestionUseCase(
    private val today: () -> LocalDate = LocalDate::now
) {
    operator fun invoke(): RitualQuestion {
        val date = today()
        return QuestionCatalog.getTodayQuestion(dayOfYear = date.dayOfYear, year = date.year)
    }
}
