package org.clockout.data.local

import org.clockout.domain.model.RitualQuestion

object QuestionsData {
    val questions: List<RitualQuestion> = listOf(
        RitualQuestion(
            id = "q_success",
            textEn = "What went well today?",
            textCs = "Co se dnes povedlo?",
            suggestedAnswersEn = listOf(
                "Finished an important task",
                "Solved a tricky challenge",
                "Stayed calm and kept perspective",
                "Took time for a healthy break"
            ),
            suggestedAnswersCs = listOf(
                "Dokončil(a) jsem důležitý úkol",
                "Vyřešil(a) jsem zapeklitý problém",
                "Udržel(a) jsem klid a nadhled",
                "Udělal(a) jsem si čas na zdravou pauzu"
            )
        ),
        RitualQuestion(
            id = "q_let_go",
            textEn = "What are you leaving behind closed doors?",
            textCs = "Co necháváš za zavřenými dveřmi?",
            suggestedAnswersEn = listOf(
                "Unfinished emails & tasks until tomorrow",
                "Feeling like I should have done more",
                "Work tension and stressful calls",
                "All open browser tabs in my head"
            ),
            suggestedAnswersCs = listOf(
                "Nedokončené emaily a úkoly do zítřka",
                "Pocit, že jsem měl(a) stihnout víc",
                "Pracovní stres a náročné hovory",
                "Všechny otevřené pracovní taby v hlavě"
            )
        ),
        RitualQuestion(
            id = "q_proud",
            textEn = "What are you proud of today?",
            textCs = "Na co jsi dnes hrdý/á?",
            suggestedAnswersEn = listOf(
                "Navigated through a demanding day",
                "My patience and steady energy",
                "A small but meaningful step forward",
                "Saying no and protecting my boundaries"
            ),
            suggestedAnswersCs = listOf(
                "Že jsem náročný den zvládl(a)",
                "Na svou trpělivost a energii",
                "Na malý, ale důležitý krok vpřed",
                "Na to, že jsem řekl(a) ne a držel(a) hranice"
            )
        ),
        RitualQuestion(
            id = "q_tomorrow",
            textEn = "What do you want to remember tomorrow?",
            textCs = "Co si zítra chceš pamatovat?",
            suggestedAnswersEn = listOf(
                "Start the morning calm and unhurried",
                "Focus on one single thing at a time",
                "Keep clear boundaries between work and rest",
                "Tomorrow is another day; world won't end"
            ),
            suggestedAnswersCs = listOf(
                "Začít ráno v klidu bez spěchu",
                "Soustředit se na jednu věc po druhé",
                "Pevné hranice mezi prací a volnem",
                "Že zítra je taky den a svět se nezboří"
            )
        ),
        RitualQuestion(
            id = "q_signal",
            textEn = "What signal will you give your body and mind now?",
            textCs = "Jaký signál teď dáš svému tělu a mozku?",
            suggestedAnswersEn = listOf(
                "Closing the laptop and leaving the desk",
                "No more Slack, Teams, or inbox today",
                "Changing into comfortable evening clothes",
                "Stepping outside for a short fresh air walk"
            ),
            suggestedAnswersCs = listOf(
                "Zavírám notebook a odcházím od stolu",
                "Dnes už žádný Slack, Teams ani pošta",
                "Převlékám se do pohodlného oblečení",
                "Jdu se projít ven na čerstvý vzduch"
            )
        ),
        RitualQuestion(
            id = "q_evening",
            textEn = "How will you treat yourself this evening?",
            textCs = "Čím dnes potěšíš svůj večer?",
            suggestedAnswersEn = listOf(
                "A nourishing dinner without looking at screens",
                "A hot shower or relaxing bath",
                "A good book, music, or quiet downtime",
                "Quality time with family or friends"
            ),
            suggestedAnswersCs = listOf(
                "Dobrou večeří bez koukání do mobilu",
                "Horkou sprchou nebo vanou",
                "Knížkou, hudbou nebo tichým odpočinkem",
                "Časem s rodinou nebo přáteli"
            )
        )
    )

    fun getTodayQuestion(dayOfYear: Int): RitualQuestion {
        val index = (dayOfYear.coerceAtLeast(0)) % questions.size
        return questions[index]
    }
}
