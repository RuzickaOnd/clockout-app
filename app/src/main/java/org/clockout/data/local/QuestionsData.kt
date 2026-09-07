package org.clockout.data.local

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import kotlin.random.Random

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
        ),
        RitualQuestion(
            id = "q_enough",
            textEn = "What was enough today?",
            textCs = "Co dnes stačilo?",
            suggestedAnswersEn = listOf(
                "The hours I actually gave it",
                "Doing the important thing, not everything",
                "Showing up even when it was hard",
                "One honest effort, not a perfect one"
            ),
            suggestedAnswersCs = listOf(
                "Hodiny, které jsem tomu opravdu dal(a)",
                "Že jsem udělal(a) to důležité, ne všechno",
                "Že jsem tam byl(a), i když to šlo ztuha",
                "Poctivá snaha, ne dokonalý výsledek"
            )
        ),
        RitualQuestion(
            id = "q_hardest",
            textEn = "What was the hardest part of today?",
            textCs = "Co bylo na dnešku nejtěžší?",
            suggestedAnswersEn = listOf(
                "A conversation I was dreading",
                "Starting when I had no motivation",
                "Too many things landing at once",
                "Staying focused through the noise"
            ),
            suggestedAnswersCs = listOf(
                "Hovor, ze kterého jsem měl(a) obavy",
                "Začít, když se mi vůbec nechtělo",
                "Příliš mnoho věcí naráz",
                "Udržet pozornost v tom shonu"
            )
        ),
        RitualQuestion(
            id = "q_boundary",
            textEn = "Where did you hold a boundary today?",
            textCs = "Kde jsi dnes udržel(a) hranici?",
            suggestedAnswersEn = listOf(
                "I took a real lunch break",
                "I left a message for tomorrow",
                "I said no to one more meeting",
                "I stopped at the time I promised myself"
            ),
            suggestedAnswersCs = listOf(
                "Dal(a) jsem si opravdovou pauzu na oběd",
                "Nechal(a) jsem zprávu až na zítra",
                "Odmítl(a) jsem další schůzku",
                "Skončil(a) jsem v čase, který jsem si slíbil(a)"
            )
        ),
        RitualQuestion(
            id = "q_body",
            textEn = "What is your body telling you right now?",
            textCs = "Co ti teď říká tvoje tělo?",
            suggestedAnswersEn = listOf(
                "My shoulders are up by my ears",
                "I have been sitting far too long",
                "I am hungry and running on coffee",
                "It is calmer than my head is"
            ),
            suggestedAnswersCs = listOf(
                "Ramena mám až u uší",
                "Sedím o dost déle, než je zdrávo",
                "Mám hlad a jedu na kávě",
                "Je klidnější než moje hlava"
            )
        ),
        RitualQuestion(
            id = "q_learned",
            textEn = "What did today teach you?",
            textCs = "Co tě dnešek naučil?",
            suggestedAnswersEn = listOf(
                "Asking sooner would have saved hours",
                "A short break makes the work faster",
                "Not every fire is mine to put out",
                "The first version does not have to be right"
            ),
            suggestedAnswersCs = listOf(
                "Zeptat se dřív by ušetřilo hodiny",
                "Krátká pauza práci zrychlí",
                "Ne každý požár musím hasit já",
                "První verze nemusí být ta správná"
            )
        ),
        RitualQuestion(
            id = "q_worry",
            textEn = "Which worry stays at your desk?",
            textCs = "Která starost zůstává u tvého stolu?",
            suggestedAnswersEn = listOf(
                "A deadline that is still far enough away",
                "What someone might think of my work",
                "A decision I cannot make tonight",
                "A problem that will look smaller tomorrow"
            ),
            suggestedAnswersCs = listOf(
                "Termín, který je pořád dost daleko",
                "Co si o mojí práci možná někdo myslí",
                "Rozhodnutí, které dnes večer stejně neudělám",
                "Problém, který bude ráno menší"
            )
        ),
        RitualQuestion(
            id = "q_grateful",
            textEn = "What are you grateful for today?",
            textCs = "Za co jsi dnes vděčný/á?",
            suggestedAnswersEn = listOf(
                "A colleague who covered for me",
                "That the hard part is already done",
                "A quiet hour exactly when I needed one",
                "That I have somewhere to come home to"
            ),
            suggestedAnswersCs = listOf(
                "Za kolegu, který mě podržel",
                "Že to nejtěžší už mám za sebou",
                "Za hodinu klidu, když jsem ji potřeboval(a)",
                "Že se mám kam vrátit domů"
            )
        ),
        RitualQuestion(
            id = "q_people",
            textEn = "Who made your day easier today?",
            textCs = "Kdo ti dnes ulehčil den?",
            suggestedAnswersEn = listOf(
                "Someone who answered the moment I asked",
                "A colleague who listened without advice",
                "Someone at home who took a task off me",
                "A stranger who was simply kind"
            ),
            suggestedAnswersCs = listOf(
                "Někdo, kdo odpověděl hned, jak jsem se ozval(a)",
                "Kolega, který mě jen vyslechl",
                "Někdo doma, kdo za mě něco vzal",
                "Cizí člověk, který byl prostě milý"
            )
        ),
        RitualQuestion(
            id = "q_unfinished",
            textEn = "What stays unfinished, and that is fine?",
            textCs = "Co zůstává nedokončené a je to v pořádku?",
            suggestedAnswersEn = listOf(
                "A task that is not burning today",
                "A reply that can wait until morning",
                "A document nobody is waiting for",
                "The tidy-up I keep postponing"
            ),
            suggestedAnswersCs = listOf(
                "Úkol, který dnes na nic nehoří",
                "Odpověď, která počká do rána",
                "Dokument, na který nikdo nečeká",
                "Úklid, který pořád odkládám"
            )
        ),
        RitualQuestion(
            id = "q_pace",
            textEn = "What pace did today have?",
            textCs = "Jaké mělo dnešek tempo?",
            suggestedAnswersEn = listOf(
                "Rushed from the first minute",
                "Slow, and that suited me",
                "Uneven — quiet, then all at once",
                "Steady, the way I like it"
            ),
            suggestedAnswersCs = listOf(
                "Uspěchané od první minuty",
                "Pomalé, a mně to vyhovovalo",
                "Nevyrovnané — chvíli klid, pak všechno naráz",
                "Vyrovnané, jak to mám rád(a)"
            )
        ),
        RitualQuestion(
            id = "q_attention",
            textEn = "Where did most of your attention go today?",
            textCs = "Kam šla dnes většina tvé pozornosti?",
            suggestedAnswersEn = listOf(
                "Into one thing I actually cared about",
                "Into messages and notifications",
                "Into other people's urgent matters",
                "Into worrying about what comes next"
            ),
            suggestedAnswersCs = listOf(
                "Do jedné věci, na které mi opravdu záleželo",
                "Do zpráv a notifikací",
                "Do naléhavých věcí ostatních",
                "Do obav z toho, co přijde"
            )
        ),
        RitualQuestion(
            id = "q_small_joy",
            textEn = "What small thing made you smile today?",
            textCs = "Co malého tě dnes potěšilo?",
            suggestedAnswersEn = listOf(
                "The first coffee of the morning",
                "A message from someone I like",
                "Sunlight coming through the window",
                "Crossing one thing off the list"
            ),
            suggestedAnswersCs = listOf(
                "První ranní káva",
                "Zpráva od někoho, koho mám rád(a)",
                "Slunce, co svítilo do okna",
                "Škrtnutí jedné věci ze seznamu"
            )
        ),
        RitualQuestion(
            id = "q_permission",
            textEn = "What are you giving yourself permission to drop?",
            textCs = "Co si dnes dovolíš pustit z hlavy?",
            suggestedAnswersEn = listOf(
                "The need to have everything under control",
                "Comparing myself to everyone else",
                "Replaying one conversation over and over",
                "The feeling that I am behind"
            ),
            suggestedAnswersCs = listOf(
                "Potřebu mít všechno pod kontrolou",
                "Srovnávání se s ostatními",
                "Přehrávání jednoho rozhovoru dokola",
                "Pocit, že mám zpoždění"
            )
        ),
        RitualQuestion(
            id = "q_energy",
            textEn = "What gave you energy today?",
            textCs = "Co ti dnes dodalo energii?",
            suggestedAnswersEn = listOf(
                "Work that finally moved forward",
                "A walk or some fresh air",
                "A conversation that went well",
                "Music while I worked"
            ),
            suggestedAnswersCs = listOf(
                "Práce, která se konečně pohnula",
                "Procházka nebo čerstvý vzduch",
                "Rozhovor, který dobře dopadl",
                "Hudba při práci"
            )
        ),
        RitualQuestion(
            id = "q_kindness",
            textEn = "What kindness did you show or receive today?",
            textCs = "Jaká laskavost tě dnes potkala?",
            suggestedAnswersEn = listOf(
                "Someone thanked me and meant it",
                "I helped without being asked",
                "I was patient when I did not feel like it",
                "I was finally gentle with myself too"
            ),
            suggestedAnswersCs = listOf(
                "Někdo mi upřímně poděkoval",
                "Pomohl(a) jsem, aniž o to někdo řekl",
                "Byl(a) jsem trpělivý/á, i když se mi nechtělo",
                "Byl(a) jsem konečně laskavý/á i k sobě"
            )
        ),
        RitualQuestion(
            id = "q_noise",
            textEn = "What turned out to be just noise today?",
            textCs = "Co byl dnes nakonec jen šum?",
            suggestedAnswersEn = listOf(
                "An urgent email that was not urgent",
                "A meeting that could have been a message",
                "A worry that never happened",
                "Half of my notifications"
            ),
            suggestedAnswersCs = listOf(
                "Naléhavý email, který naléhavý nebyl",
                "Schůzka, která mohla být zprávou",
                "Obava, která se vůbec nenaplnila",
                "Půlka mých notifikací"
            )
        ),
        RitualQuestion(
            id = "q_next_hour",
            textEn = "How do you want to spend the next hour?",
            textCs = "Jak chceš strávit nejbližší hodinu?",
            suggestedAnswersEn = listOf(
                "Doing nothing at all, on purpose",
                "Moving my body a little",
                "With someone I barely spoke to today",
                "On something with no purpose whatsoever"
            ),
            suggestedAnswersCs = listOf(
                "Záměrně úplně ničím",
                "Trochou pohybu",
                "S někým, s kým jsem si dnes pořádně nepromluvil(a)",
                "Něčím, co nemá vůbec žádný účel"
            )
        ),
        RitualQuestion(
            id = "q_place",
            textEn = "Where does your free time begin today?",
            textCs = "Kde dnes začíná tvoje volno?",
            suggestedAnswersEn = listOf(
                "The moment I step away from the desk",
                "On the doorstep, outside",
                "In the kitchen, making dinner",
                "Under the shower"
            ),
            suggestedAnswersCs = listOf(
                "Ve chvíli, kdy odejdu od stolu",
                "Za dveřmi, venku",
                "V kuchyni u večeře",
                "Pod sprchou"
            )
        ),
        RitualQuestion(
            id = "q_role",
            textEn = "Who are you now that work is over?",
            textCs = "Kým jsi teď, když práce skončila?",
            suggestedAnswersEn = listOf(
                "Someone who is simply at home",
                "A parent, a partner, a friend",
                "Just myself, with no role at all",
                "Someone with a whole evening ahead"
            ),
            suggestedAnswersCs = listOf(
                "Někdo, kdo je prostě doma",
                "Rodič, partner, kamarád",
                "Jenom já, bez jakékoli role",
                "Někdo, kdo má před sebou celý večer"
            )
        ),
        RitualQuestion(
            id = "q_slow_down",
            textEn = "What helps you slow down tonight?",
            textCs = "Co ti dnes pomůže zpomalit?",
            suggestedAnswersEn = listOf(
                "Putting the phone in another room",
                "Doing one thing at a time",
                "Warmth — a shower, tea, a blanket",
                "Quiet, with nothing playing"
            ),
            suggestedAnswersCs = listOf(
                "Odložit telefon do jiné místnosti",
                "Dělat jednu věc po druhé",
                "Teplo — sprcha, čaj, deka",
                "Ticho, bez puštěného pozadí"
            )
        ),
        RitualQuestion(
            id = "q_week",
            textEn = "How is this week treating you so far?",
            textCs = "Jaký je zatím tvůj týden?",
            suggestedAnswersEn = listOf(
                "Better than it looked on Monday",
                "Long, and it is not over yet",
                "Full, but in a good way",
                "I have lost track of the days"
            ),
            suggestedAnswersCs = listOf(
                "Lepší, než jak vypadal v pondělí",
                "Dlouhý, a ještě není konec",
                "Nabitý, ale v dobrém",
                "Ztratil(a) jsem přehled o dnech"
            )
        ),
        RitualQuestion(
            id = "q_ask",
            textEn = "What could you have asked someone for help with?",
            textCs = "S čím sis mohl(a) říct o pomoc?",
            suggestedAnswersEn = listOf(
                "Something I have been stuck on for days",
                "A decision I do not want to make alone",
                "Work that is simply too much for one person",
                "Nothing — I asked in time today"
            ),
            suggestedAnswersCs = listOf(
                "S něčím, na čem trčím už dny",
                "S rozhodnutím, které nechci dělat sám/sama",
                "S prací, která je prostě na víc lidí",
                "S ničím — dnes jsem si řekl(a) včas"
            )
        ),
        RitualQuestion(
            id = "q_repeat",
            textEn = "What from today would you do again tomorrow?",
            textCs = "Co bys ze dneška zopakoval(a) i zítra?",
            suggestedAnswersEn = listOf(
                "Starting with the hardest thing",
                "Taking a break before I needed one",
                "Keeping the morning free of meetings",
                "Finishing on time"
            ),
            suggestedAnswersCs = listOf(
                "Začít tím nejtěžším",
                "Dát si pauzu dřív, než jsem ji potřeboval(a)",
                "Nechat si ráno bez schůzek",
                "Skončit včas"
            )
        ),
        RitualQuestion(
            id = "q_tomorrow_self",
            textEn = "What would tomorrow-you thank you for right now?",
            textCs = "Za co by ti zítřejší ty teď poděkoval(a)?",
            suggestedAnswersEn = listOf(
                "Going to bed at a decent hour",
                "Leaving the desk tidy behind me",
                "Not opening the laptop again tonight",
                "Resting instead of pushing on"
            ),
            suggestedAnswersCs = listOf(
                "Že půjdu spát v rozumnou hodinu",
                "Že jsem po sobě uklidil(a) stůl",
                "Že už dnes notebook neotevřu",
                "Že si odpočinu, místo abych tlačil(a) dál"
            )
        )
    )

    /**
     * The order the questions come round in, reshuffled once a year. Walking the catalog in
     * declaration order made tomorrow's question the next one down the list; a permutation keeps
     * the day's question stable and still hands out the whole pool before repeating any of it.
     */
    private fun rotationFor(year: Int): List<RitualQuestion> = questions.shuffled(Random(year))

    fun getTodayQuestion(dayOfYear: Int, year: Int): RitualQuestion {
        val rotation = rotationFor(year)
        return rotation[dayOfYear.coerceAtLeast(0) % rotation.size]
    }

    private fun String.lookupKey(): String = trim()

    private val questionsByText: Map<String, RitualQuestion> = buildMap {
        questions.forEach { question ->
            AppLanguage.entries.forEach { language ->
                put(question.text(language).lookupKey(), question)
            }
        }
    }

    private val answersByText: Map<String, Pair<RitualQuestion, Int>> = buildMap {
        questions.forEach { question ->
            AppLanguage.entries.forEach { language ->
                question.suggestedAnswers(language).forEachIndexed { index, answer ->
                    put(answer.lookupKey(), question to index)
                }
            }
        }
    }

    /**
     * A ritual log keeps the question as the plain text shown that evening, so reading it back in
     * another language means finding it in the catalog again. Wording this catalog no longer
     * carries - a custom entry, or a phrase an older app version worded differently - is returned
     * unchanged rather than dropped.
     */
    fun localizeQuestionText(storedText: String, language: AppLanguage): String =
        questionsByText[storedText.lookupKey()]?.text(language) ?: storedText

    /**
     * The same for the suggestion the user picked. The question it belongs to decides which
     * suggestion list to index into, so an answer is never re-worded through another question that
     * happens to offer the same phrase; answers the user typed themselves are left as they are.
     */
    fun localizeAnswer(
        storedAnswer: String,
        storedQuestionText: String?,
        language: AppLanguage
    ): String {
        val key = storedAnswer.lookupKey()
        val question = storedQuestionText?.let { questionsByText[it.lookupKey()] }
        val match = question?.let { it to it.indexOfAnswer(key) }?.takeIf { it.second >= 0 }
            ?: answersByText[key]
            ?: return storedAnswer
        return match.first.suggestedAnswers(language)[match.second]
    }

    private fun RitualQuestion.indexOfAnswer(key: String): Int {
        AppLanguage.entries.forEach { language ->
            val index = suggestedAnswers(language).indexOfFirst { it.lookupKey() == key }
            if (index >= 0) return index
        }
        return -1
    }
}
