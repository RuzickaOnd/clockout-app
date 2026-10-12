# Clean-lite architektura — implementační plán

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Přestavět `:app` podle oficiálního Android architecture guide (UI → domain → data, jednosměrné závislosti) s prvky Clean Architecture tam, kde se vyplatí. Chování aplikace se nemění.

**Architecture:** Závislosti skládá jeden `AppContainer` (manuální DI, oficiálně dokumentovaný vzor) v `ClockoutApplication`. Composables ani Activity si repozitáře nestaví samy. Rozhraní repozitářů a platformních služeb leží v `domain/` a implementace v `data/`, `sound/` a `notification/`. Presentation zná jen domain a test to vynucuje. Monolitní `RitualRepository` se rozdělí podle typu dat metodou strangler: nejdřív se zúží spotřebitelé, až potom se rozdělí implementace. Přeposílací use case zmizí a logiku dokončení rituálu převezme `CompleteRitualUseCase`. Poslední, volitelný úkol vyčlení doménu do čistě JVM modulu `:core:domain`.

**Tech Stack:** Kotlin 2.4.21, AGP 9.4.1, Compose BOM 2026.09.00, Room 2.8.5, DataStore, kotlinx.coroutines 1.11.0, JUnit 4, Robolectric 4.17.

**Spec:** Bez samostatné specifikace. Plán vychází z architektonického posudku v konverzaci ze 2026-10-12 a z pravidel v `CLAUDE.md` / `AGENTS.md`; obojí čti spolu s plánem.

## Global Constraints

- Žádný commit, `git add` ani push bez výslovného souhlasu uživatele **v tomtéž kroku**. Každý krok „Commit“ znamená: zeptej se, počkej na „ano“, teprve pak commituj.
- Commit zprávy česky s prefixem `refactor:` / `feat:` / `docs:` / `chore:` / `CL:` a končí řádkem `Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>`.
- Každý úkol aktualizuje `CLAUDE.md` **i** `AGENTS.md` ve stejném commitu.
- Kód, KDoc i komentáře anglicky. Komentáře jen tam, kde je nezřejmé *proč*.
- `domain/` neobsahuje `R`, `android.*`, `androidx.*`, `org.clockout.data.*` ani `org.clockout.presentation.*`.
- Všechny verze jdou přes `gradle/libs.versions.toml`.
- `LocalContext` se nikdy nepřepisuje. `AppLocalization` dál poskytuje jen `LocalResources`.
- Statické `R.string.*` / `R.array.*`, žádné `getIdentifier`.
- `clearLogs()` nikdy nemaže `practice_days`. Ritual counter, review flag ani `telemetry_consent` se při mazání historie neresetují.
- `logRitual` zapisuje `ritual_logs` a `practice_days` v jedné transakci (`RitualLogDao.logDay`) při jednom čtení hodin.
- Repozitáře a cesta/obloha si datum berou z `todayFlow`. V testech je to `flowOf(LocalDate.now())`, nikdy smyčka s `delay`.
- Ověření každého úkolu: `./gradlew :app:compileDebugKotlin`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`. Na konci také `./gradlew :app:verifyRoborazziDebug`. O víkendu padají `PhoneNavigationTest` a `TabletNavigationTest` (viz CLAUDE.md); spusť je zvlášť a neber to jako regresi.

## Review Focus

1. **Dvojí migrace legacy logů:** po rozdělení repozitáře musí `RitualLogRepositoryImpl` i `ProgressRepositoryImpl` sdílet **jednu** instanci `RitualLogMigrator`. Jeho `migrated` flag a mutex jsou per-instance, takže dvě instance by import spustily dvakrát. Pinuje test v Task 5.
2. **Selhání oblohy nesmí zablokovat zápis:** výjimka z `ProgressRepository` při dokončení rituálu musí zápis do historie zachovat a jen vynechat oslavu. Pinuje test v Task 6.
3. **Přeskočená nálada nikdy nežádá o recenzi:** `SkipMood` loguje `CALM`, ale `moodWasChosen = false`. Pinuje test v Task 6.
4. **Plánování připomínky:** přepnutí notifikací ji naplánuje na aktuální čas, vypnutí ji zruší a změna času při vypnutých notifikacích nic neplánuje. Dosud se to netestovalo, protože testy předávaly `context = null`. Pinuje test v Task 3.
5. **Jedna instance repozitářů na proces:** `MainActivity`, `PathActivity`, `SkyActivity`, `BreathingStyleActivity` a `NotificationReceiver` musí dostat tytéž objekty z kontejneru. Pinuje test v Task 1.

---

## Mapa souborů

| Soubor | Úkol | Odpovědnost |
|---|---|---|
| `app/src/main/java/org/clockout/AppContainer.kt` (nový) | 1, 3, 4, 5, 6 | Skládá graf závislostí aplikace |
| `app/src/main/java/org/clockout/ClockoutApplication.kt` | 1, 2 | Drží kontejner a instaluje katalog otázek |
| `app/src/main/java/org/clockout/presentation/RitualDependencies.kt` | 1 | `appContainer()` pro composables (nahrazuje `rememberRitualRepository`) |
| `app/src/test/java/org/clockout/testing/TestAppContainer.kt` (nový) | 1 | Kontejner pro testy (in-memory Room, dočasná DataStore) |
| `app/src/main/java/org/clockout/domain/model/QuestionCatalog.kt` (nový) | 2 | Čistý katalog otázek: rotace a zpětná lokalizace |
| `app/src/main/java/org/clockout/data/local/QuestionResources.kt` (přejmenovaný `QuestionsData.kt`) | 2 | Načtení katalogu ze string resources |
| `app/src/main/java/org/clockout/domain/platform/{ReminderScheduler,SoundPlayer,SoundFileStore}.kt` (nové) | 3 | Porty na platformu |
| `app/src/main/java/org/clockout/domain/model/SoundImport.kt` (nový) | 3 | `SoundImportResult`, `SoundImportError` |
| `app/src/main/java/org/clockout/notification/AlarmReminderScheduler.kt` (nový) | 3 | `ReminderScheduler` nad `NotificationScheduler` |
| `app/src/test/java/org/clockout/testing/Fakes.kt` (nový) | 3, 6 | Testovací dvojníci portů a repozitářů |
| `app/src/main/java/org/clockout/domain/repository/*.kt` (nové) | 4 | `SettingsRepository`, `SoundRepository`, `RitualLogRepository`, `ProgressRepository` |
| `app/src/test/java/org/clockout/LayerDependencyTest.kt` (nový) | 4 | Hlídá směr závislostí mezi vrstvami |
| `app/src/main/java/org/clockout/data/repository/*RepositoryImpl.kt` (nové; `RitualRepository*` smazány) | 5 | Implementace podle typu dat |
| `app/src/main/java/org/clockout/domain/usecase/CompleteRitualUseCase.kt` (nový) | 6 | Zápis rituálu a výpočet oslav |
| `core/domain/**` (nový modul) | 7 (volitelný) | Čistě JVM doména |

---

### Task 0: Příprava větve a výchozí stav

**Files:** žádné

- [ ] **Step 1: Založ větev z `develop`**

```bash
git checkout develop && git pull --ff-only
git checkout -b feature/clean-architecture
```

- [ ] **Step 2: Ověř zelený výchozí stav**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug`
Expected: `BUILD SUCCESSFUL`. Pokud něco padá ještě před změnou, zapiš si to. Takové selhání není regrese tohoto plánu (pozor na víkend a navigační testy).

- [ ] **Step 3: Zaznamenej screenshot baseline**

Run: `./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"`
Pak hned `./gradlew :app:verifyRoborazziDebug`, mělo by projít. Refaktoring nemá měnit UI, takže tento baseline slouží jako regresní brána.

---

### Task 1: `AppContainer` — manuální DI na úrovni aplikace

Graf se dnes skládá v `rememberRitualRepository()` uvnitř composables, zvlášť pro každou Activity, a `NotificationReceiver` si staví vlastní `RitualPreferences`. Po této změně ho poskládá jediný kontejner v `Application`. Kontejner se staví z application contextu, který `AppLocalization` nikdy nepřepisuje, takže pravidlo „volej repozitář nad `AppLocalization`“ přestane být potřeba.

**Files:**
- Create: `app/src/main/java/org/clockout/AppContainer.kt`
- Modify: `app/src/main/java/org/clockout/ClockoutApplication.kt`
- Modify: `app/src/main/java/org/clockout/presentation/RitualDependencies.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt:110-130` a 4× `collectAsState()` → `collectAsStateWithLifecycle()`
- Modify: `app/src/main/java/org/clockout/{PathActivity,SkyActivity,BreathingStyleActivity}.kt`
- Modify: `app/src/main/java/org/clockout/receiver/NotificationReceiver.kt:37`
- Create: `app/src/test/java/org/clockout/testing/TestAppContainer.kt`
- Create: `app/src/test/java/org/clockout/AppContainerTest.kt`
- Modify: 14 testů, které dnes volají `RitualRepositoryImpl(` (seznam: `grep -rln "RitualRepositoryImpl(" app/src/test`)
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Produces:
  - `class AppContainer(context: Context, preferences: RitualPreferences = RitualPreferences(context), database: ClockoutDatabase = ClockoutDatabase.getInstance(context), todayFlow: Flow<LocalDate> = context.applicationContext.todayFlow(), soundFileExists: (String) -> Boolean = …)` s `val preferences`, `val database`, `val ritualRepository: RitualRepository`.
  - `val Context.appContainer: AppContainer`.
  - `@Composable fun appContainer(): AppContainer`.
  - `fun testAppContainer(folder: TemporaryFolder, soundFileExists: (String) -> Boolean = { true }): AppContainer`.

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/AppContainerTest.kt`:

```kotlin
package org.clockout

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppContainerTest {

    @Test
    fun `the application hands out one container per process`() {
        val app = ApplicationProvider.getApplicationContext<ClockoutApplication>()

        assertSame(app.container, app.container)
        assertSame(app.container.ritualRepository, app.container.ritualRepository)
    }

    @Test
    fun `an activity context reaches the same container as the application`() {
        val app = ApplicationProvider.getApplicationContext<ClockoutApplication>()
        val activity = Robolectric.buildActivity(PathActivity::class.java).create().get()

        assertSame(app.container, activity.appContainer)
    }
}
```

- [ ] **Step 2: Ověř, že test padá**

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.AppContainerTest"`
Expected: FAIL při kompilaci (`Unresolved reference 'container'`, `'appContainer'`).

- [ ] **Step 3: Implementuj kontejner**

`app/src/main/java/org/clockout/AppContainer.kt`:

```kotlin
package org.clockout

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.clockout.data.local.CustomSoundStorage
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.util.todayFlow
import java.time.LocalDate

/**
 * The app's object graph, built once per process from the application context. Every Activity,
 * the reminder receiver and the tests take their dependencies from here rather than wiring their
 * own, so DataStore and Room are never handed a locale-overridden context.
 */
class AppContainer(
    context: Context,
    val preferences: RitualPreferences = RitualPreferences(context.applicationContext),
    val database: ClockoutDatabase = ClockoutDatabase.getInstance(context.applicationContext),
    todayFlow: Flow<LocalDate> = context.applicationContext.todayFlow(),
    // A cloud backup restores the sound metadata but not the audio files, which travel only on a
    // device-to-device transfer, so what is on disk has the last word.
    soundFileExists: (fileName: String) -> Boolean = { fileName ->
        CustomSoundStorage.resolveFile(context.applicationContext, fileName).exists()
    }
) {
    val ritualRepository: RitualRepository = RitualRepositoryImpl(
        preferences = preferences,
        ritualLogDao = database.ritualLogDao(),
        practiceDayDao = database.practiceDayDao(),
        todayFlow = todayFlow,
        soundFileExists = soundFileExists
    )
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ClockoutApplication).container
```

`ClockoutApplication.kt`, uvnitř třídy:

```kotlin
    /** Built on first use, so a process started only for the reminder receiver pays for it too. */
    val container: AppContainer by lazy { AppContainer(this) }
```

`presentation/RitualDependencies.kt` celý nahraď:

```kotlin
package org.clockout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.clockout.AppContainer
import org.clockout.appContainer

/** The process-wide [AppContainer], reachable from any composable whatever its localization. */
@Composable
fun appContainer(): AppContainer = LocalContext.current.appContainer
```

V `PadlaApp.kt` nahraď `val repository = rememberRitualRepository(context)` za `val repository = appContainer().ritualRepository`. Všechna 4 `collectAsState()` / `collectAsState(initial = …)` nahraď `collectAsStateWithLifecycle()` / `collectAsStateWithLifecycle(initialValue = …)` (import `androidx.lifecycle.compose.collectAsStateWithLifecycle`; závislost `lifecycle-runtime-compose` už v projektu je).

V `PathActivity`, `SkyActivity` a `BreathingStyleActivity` nahraď `val repository = rememberRitualRepository()` za `val repository = appContainer().ritualRepository` a uprav importy.

V `NotificationReceiver.kt:37` nahraď `val preferences = RitualPreferences(context)` za `val preferences = context.appContainer.preferences` a odstraň import `RitualPreferences`.

- [ ] **Step 4: Vytvoř testovací kontejner a převeď na něj testy**

`app/src/test/java/org/clockout/testing/TestAppContainer.kt`:

```kotlin
package org.clockout.testing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.flowOf
import org.clockout.AppContainer
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/**
 * A container over an in-memory database and a DataStore of its own, so tests never share the
 * process-wide store. Today is read once: a context-driven date flow under runTest would never end.
 */
fun testAppContainer(
    folder: TemporaryFolder,
    soundFileExists: (String) -> Boolean = { true },
    context: Context = ApplicationProvider.getApplicationContext()
): AppContainer = AppContainer(
    context = context,
    preferences = RitualPreferences(
        PreferenceDataStoreFactory.create {
            folder.newFile("test.preferences_pb").apply { delete() }
        }
    ),
    database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build(),
    todayFlow = flowOf(LocalDate.now()),
    soundFileExists = soundFileExists
)
```

V každém testu ze seznamu `grep -rln "RitualRepositoryImpl(" app/src/test` nahraď ruční stavbu `RitualRepositoryImpl(...)` voláním `testAppContainer(preferencesFolder, …).also { container = it }.ritualRepository`. V `@After` zavolej `container.database.close()`. Testy, které schválně pracují nad sdílenou DataStore (`CustomSoundReplacementTest` s `RitualPreferences(context)` a jejím úklidem v `@After`), si předají `preferences` ručně přes konstruktor `AppContainer(...)`. Chování testů se tím nemění.

- [ ] **Step 5: Ověř, že testy procházejí**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (včetně `AppContainerTest`).

- [ ] **Step 6: Aktualizuj dokumentaci**

V `CLAUDE.md` i `AGENTS.md`:
- Architecture: odstavec o `PadlaApp` (graf už nestaví z „original context“, bere ho z `AppContainer`) a odstavec o `BreathingStyleActivity` (místo `rememberRitualRepository()` se volá `appContainer()`).
- What NOT to do: bod „Do not rebuild the window chrome or the repository…“ přepiš na „…take dependencies from `AppContainer` via `appContainer()` / `Context.appContainer`; never construct `RitualPreferences` or `ClockoutDatabase` outside it.“
- Persistence: zmínka, že testy staví graf přes `testAppContainer`.

- [ ] **Step 7: Lint a commit (jen se souhlasem)**

Run: `./gradlew :app:lintDebug` → `BUILD SUCCESSFUL`.
Zeptej se uživatele. Po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: AppContainer jako jediné místo skládání závislostí

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Katalog otázek do domény

`presentation/util/RitualLogLocalization.kt` dnes importuje `data.local.QuestionsData`. Katalog po načtení obsahuje jen čistá data (`RitualQuestion`), takže ho rozdělíme na dvě části: načítání z resources zůstane v `data` a dotazy nad katalogem se přesunou do `domain`.

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/QuestionCatalog.kt`
- Rename+Modify: `app/src/main/java/org/clockout/data/local/QuestionsData.kt` → `QuestionResources.kt`
- Modify: `app/src/main/java/org/clockout/ClockoutApplication.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt:73-79`
- Modify: `app/src/main/java/org/clockout/presentation/util/RitualLogLocalization.kt`
- Modify: všechny výskyty `QuestionsData.` v `app/src` (17 výskytů; `grep -rn "QuestionsData" app/src`)
- Create: `app/src/test/java/org/clockout/QuestionCatalogLookupTest.kt`
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Produces: `object QuestionCatalog { fun install(questions: List<RitualQuestion>); val questions: List<RitualQuestion>; fun getTodayQuestion(dayOfYear: Int, year: Int): RitualQuestion; fun localizeQuestionText(storedText: String, language: AppLanguage): String; fun localizeAnswer(storedAnswer: String, storedQuestionText: String?, language: AppLanguage): String }` a `object QuestionResources { fun load(context: Context): List<RitualQuestion> }`.

- [ ] **Step 1: Napiš padající test (čisté JVM, bez Robolectricu)**

```kotlin
package org.clockout

import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.QuestionCatalog
import org.clockout.domain.model.RitualQuestion
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The catalog answers from plain data alone - no resources, no Android. */
class QuestionCatalogLookupTest {

    private val calm = RitualQuestion(
        id = "q_calm",
        textEn = "What calmed you?",
        textCs = "Co tě uklidnilo?",
        suggestedAnswersEn = listOf("A walk", "Tea"),
        suggestedAnswersCs = listOf("Procházka", "Čaj")
    )

    @Before
    fun install() = QuestionCatalog.install(listOf(calm))

    @Test
    fun `a stored czech question reads in english`() {
        assertEquals("What calmed you?", QuestionCatalog.localizeQuestionText("Co tě uklidnilo?", AppLanguage.ENGLISH))
    }

    @Test
    fun `a suggestion is re-worded through its own question`() {
        assertEquals("Čaj", QuestionCatalog.localizeAnswer("Tea", "What calmed you?", AppLanguage.CZECH))
    }

    @Test
    fun `wording the catalog does not know is kept as it was saved`() {
        assertEquals("Something else", QuestionCatalog.localizeAnswer("Something else", null, AppLanguage.CZECH))
    }

    @Test
    fun `the same day of the same year always yields the same question`() {
        assertEquals(QuestionCatalog.getTodayQuestion(40, 2026), QuestionCatalog.getTodayQuestion(40, 2026))
    }
}
```

- [ ] **Step 2: Ověř, že test padá**

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.QuestionCatalogLookupTest"`
Expected: FAIL (`Unresolved reference 'QuestionCatalog'`).

- [ ] **Step 3: Implementuj `QuestionCatalog`**

`domain/model/QuestionCatalog.kt` vezme z `QuestionsData` beze změny chování třídu `Catalog` (přejmenovanou na `Index`), `rotationFor`, `getTodayQuestion`, `lookupKey`, `localizeQuestionText`, `localizeAnswer` a `indexOfAnswer`:

```kotlin
package org.clockout.domain.model

import kotlin.random.Random

/**
 * Today's question and the reverse lookups that let a log written in one language be read in the
 * other. Installed once per process from whatever loaded the questions; the domain never knows
 * they came out of string resources.
 */
object QuestionCatalog {

    /** The questions and their lookups, swapped together so a reader never mixes two builds. */
    private class Index(val questions: List<RitualQuestion>) {
        val questionsByText: Map<String, RitualQuestion> = buildMap {
            questions.forEach { question ->
                AppLanguage.entries.forEach { language -> put(question.text(language).lookupKey(), question) }
            }
        }
        val answersByText: Map<String, Pair<RitualQuestion, Int>> = buildMap {
            questions.forEach { question ->
                AppLanguage.entries.forEach { language ->
                    question.suggestedAnswers(language).forEachIndexed { index, answer ->
                        put(answer.lookupKey(), question to index)
                    }
                }
            }
        }
    }

    @Volatile
    private var index: Index? = null

    private val loaded: Index
        get() = index ?: error("QuestionCatalog has not been installed; the Application installs it on start.")

    fun install(questions: List<RitualQuestion>) {
        index = Index(questions)
    }

    val questions: List<RitualQuestion> get() = loaded.questions

    /**
     * The order the questions come round in, reshuffled once a year: the day's question is stable
     * and the whole pool is handed out before any of it repeats.
     */
    private fun rotationFor(year: Int): List<RitualQuestion> = questions.shuffled(Random(year))

    fun getTodayQuestion(dayOfYear: Int, year: Int): RitualQuestion {
        val rotation = rotationFor(year)
        return rotation[dayOfYear.coerceAtLeast(0) % rotation.size]
    }

    /** Wording the catalog no longer carries is returned unchanged rather than dropped. */
    fun localizeQuestionText(storedText: String, language: AppLanguage): String =
        loaded.questionsByText[storedText.lookupKey()]?.text(language) ?: storedText

    /**
     * The question decides which suggestion list to index into, so an answer is never re-worded
     * through another question that happens to offer the same phrase.
     */
    fun localizeAnswer(storedAnswer: String, storedQuestionText: String?, language: AppLanguage): String {
        val catalog = loaded
        val key = storedAnswer.lookupKey()
        val question = storedQuestionText?.let { catalog.questionsByText[it.lookupKey()] }
        val match = question?.let { it to it.indexOfAnswer(key) }?.takeIf { it.second >= 0 }
            ?: catalog.answersByText[key]
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

private fun String.lookupKey(): String = trim()
```

Přejmenuj soubor (`git mv app/src/main/java/org/clockout/data/local/QuestionsData.kt app/src/main/java/org/clockout/data/local/QuestionResources.kt`). Ponech v něm jen `QuestionResources` (původní privátní data class přejmenuj na `QuestionResourceIds`), `catalogResources` a načítání:

```kotlin
object QuestionResources {
    // private data class QuestionResourceIds + private val catalogResources: beze změny

    /**
     * Reads every language at once, because a log written in one language has to be found again
     * when the app runs in another.
     */
    fun load(context: Context): List<RitualQuestion> {
        val resourcesByLanguage = AppLanguage.entries.associateWith { context.localizedFor(it).resources }
        return catalogResources.map { question ->
            fun text(language: AppLanguage) = resourcesByLanguage.getValue(language).getString(question.text)
            fun answers(language: AppLanguage) =
                resourcesByLanguage.getValue(language).getStringArray(question.answers).toList()
            RitualQuestion(
                id = question.id,
                textEn = text(AppLanguage.ENGLISH),
                textCs = text(AppLanguage.CZECH),
                suggestedAnswersEn = answers(AppLanguage.ENGLISH),
                suggestedAnswersCs = answers(AppLanguage.CZECH)
            )
        }
    }
}
```

`ClockoutApplication.onCreate`: `QuestionsData.initialize(this)` → `QuestionCatalog.install(QuestionResources.load(this))`. Uprav i KDoc třídy.

`RitualLogLocalization.kt`: import `org.clockout.domain.model.QuestionCatalog`, tělo `QuestionsData.` → `QuestionCatalog.`.

`RitualRepositoryImpl.getTodayQuestion`: `QuestionsData.getTodayQuestion(…)` → `QuestionCatalog.getTodayQuestion(…)`.

Testy (`ExampleUnitTest`, `QuestionsCatalogTest`, `RitualLogLocalizationTest`):
`QuestionsData.initialize(x)` → `QuestionCatalog.install(QuestionResources.load(x))`, ostatní `QuestionsData.` → `QuestionCatalog.`.

- [ ] **Step 4: Ověř**

Run: `grep -rn "QuestionsData" app/src` → prázdný výstup.
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Dokumentace**

`CLAUDE.md` + `AGENTS.md`:
- Architecture: věta o `QuestionsData` teď říká, že `QuestionResources.load` načte katalog obou jazyků při startu a `QuestionCatalog` (`domain/model`) nad ním dělá rotaci a zpětné hledání.
- Strings: v bodu „Outside Compose (`QuestionsData`, `NotificationReceiver`)…“ nahraď jméno za `QuestionResources`.
- Module layout: v `data/local` místo „question catalog“ uveď „question resources“.

- [ ] **Step 6: Lint a commit (jen se souhlasem)**

`./gradlew :app:lintDebug`. Pak po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: katalog otázek přesunut do domény, načítání zůstává v datech

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Porty na platformu — připomínky, zvuk, soubory zvuků

ViewModely dnes drží `Context` a `Uri` a volají `NotificationScheduler`, `CustomSoundStorage` i `AmbientAudioPlayer` napřímo. Po této změně dostanou rozhraní z domény.

**Files:**
- Create: `app/src/main/java/org/clockout/domain/platform/ReminderScheduler.kt`, `SoundPlayer.kt`, `SoundFileStore.kt`
- Create: `app/src/main/java/org/clockout/domain/model/SoundImport.kt` (přesun `SoundImportError`, `SoundImportResult` z `data/local/CustomSoundStorage.kt:14-25`)
- Create: `app/src/main/java/org/clockout/notification/AlarmReminderScheduler.kt`
- Modify: `app/src/main/java/org/clockout/sound/AmbientAudioPlayer.kt` (context do konstruktoru, `: SoundPlayer`)
- Modify: `app/src/main/java/org/clockout/data/local/CustomSoundStorage.kt` (`: SoundFileStore`, `import(source: String, …)`)
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsViewModel.kt`, `SettingsState.kt`, `SettingsScreen.kt:107` (import)
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt` (stavba VM a callbacky Settings)
- Modify: `app/src/main/java/org/clockout/AppContainer.kt`
- Modify: `app/src/main/java/org/clockout/receiver/NotificationReceiver.kt`
- Create: `app/src/test/java/org/clockout/testing/Fakes.kt`
- Create: `app/src/test/java/org/clockout/SettingsReminderSchedulingTest.kt`
- Create: `app/src/test/java/org/clockout/RitualSoundTest.kt`
- Modify: testy stavějící `SettingsViewModel(` / `RitualViewModel(` (`grep -rln "SettingsViewModel(\|RitualViewModel(" app/src/test`)
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Consumes: `AppContainer` (Task 1).
- Produces:

```kotlin
interface ReminderScheduler { fun schedule(hour: Int, minute: Int); fun cancel() }
interface SoundPlayer {
    fun startAmbient(choice: SoundChoice)
    fun triggerBowlStrike(highPitch: Boolean)
    fun playPreview(choice: SoundChoice, onFinished: () -> Unit)
    fun stopAmbient()
}
interface SoundFileStore {
    suspend fun import(source: String, existingCount: Int): SoundImportResult
    suspend fun delete(sound: CustomSound)
}
```

  - `AppContainer.reminderScheduler: ReminderScheduler`, `AppContainer.soundFileStore: SoundFileStore`, `AppContainer.newSoundPlayer(): SoundPlayer` (nový přehrávač pro každý ViewModel, protože ho každý zastavuje v `onCleared`).
  - `SettingsViewModel(repository: RitualRepository, audioPlayer: SoundPlayer, reminderScheduler: ReminderScheduler, soundFiles: SoundFileStore)`. Metody bez `Context`: `setNotificationEnabled(enabled)`, `setNotificationTime(h, m)`, `setCustomNotificationTime(h, m)`, `togglePreviewSound(choice)`, `importCustomSound(source: String?)`, `replaceCustomSound(source: String?, replaced)`, `deleteCustomSound(sound)`.
  - `RitualViewModel(getTodayRitualUseCase, logMoodUseCase, shouldRequestReviewUseCase, ritualRepository, audioPlayer: SoundPlayer, analyticsTracker = NoOpAnalyticsTracker)`, tedy bez `appContext`.

- [ ] **Step 1: Napiš fakes a padající testy**

`app/src/test/java/org/clockout/testing/Fakes.kt`:

```kotlin
package org.clockout.testing

import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundChoice
import org.clockout.domain.model.SoundImportResult
import org.clockout.domain.platform.ReminderScheduler
import org.clockout.domain.platform.SoundFileStore
import org.clockout.domain.platform.SoundPlayer

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<Int, Int>>()
    var cancelCount = 0
    override fun schedule(hour: Int, minute: Int) { scheduled += hour to minute }
    override fun cancel() { cancelCount++ }
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
```

`app/src/test/java/org/clockout/SettingsReminderSchedulingTest.kt`:

```kotlin
package org.clockout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.domain.model.SoundImportError
import org.clockout.domain.model.SoundImportResult
import org.clockout.presentation.settings.SettingsViewModel
import org.clockout.testing.FakeReminderScheduler
import org.clockout.testing.FakeSoundFileStore
import org.clockout.testing.FakeSoundPlayer
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsReminderSchedulingTest {

    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = UnconfinedTestDispatcher()
    private val scheduler = FakeReminderScheduler()
    private lateinit var container: AppContainer

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(folder)
    }

    @After fun tearDown() {
        container.database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        repository = container.ritualRepository,
        audioPlayer = FakeSoundPlayer(),
        reminderScheduler = scheduler,
        soundFiles = FakeSoundFileStore { SoundImportResult.Failure(SoundImportError.READ_FAILED) }
    )

    @Test
    fun `switching reminders on schedules them at the stored time`() = runTest(dispatcher) {
        container.ritualRepository.setNotificationTime(18, 5)
        val vm = viewModel()
        vm.state.first { it.notificationHour == 18 && it.notificationMinute == 5 }

        vm.setNotificationEnabled(true)

        assertEquals(listOf(18 to 5), scheduler.scheduled)
    }

    @Test
    fun `switching reminders off cancels the alarm`() = runTest(dispatcher) {
        viewModel().setNotificationEnabled(false)

        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `a new time while reminders are off schedules nothing`() = runTest(dispatcher) {
        container.ritualRepository.setNotificationEnabled(false)
        val vm = viewModel()
        vm.state.first { !it.isNotificationEnabled }

        vm.setNotificationTime(19, 0)

        assertTrue(scheduler.scheduled.isEmpty())
    }
}
```

`app/src/test/java/org/clockout/RitualSoundTest.kt`. Konstruktor `RitualViewModel` zde odpovídá cílovému stavu Task 3:

```kotlin
package org.clockout

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.clockout.testing.FakeSoundPlayer
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualSoundTest {

    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = UnconfinedTestDispatcher()
    private val player = FakeSoundPlayer()
    private lateinit var container: AppContainer

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        container = testAppContainer(folder)
    }

    @After fun tearDown() {
        container.database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(): RitualViewModel {
        val repository = container.ritualRepository
        return RitualViewModel(
            getTodayRitualUseCase = GetTodayRitualUseCase(repository),
            logMoodUseCase = LogMoodUseCase(repository),
            shouldRequestReviewUseCase = ShouldRequestReviewUseCase(repository),
            ritualRepository = repository,
            audioPlayer = player
        ).also { it.startSession(1) }
    }

    @Test
    fun `breathing starts the ambient sound unless muted`() = runTest(dispatcher) {
        container.ritualRepository.setSoundEnabled(true)
        viewModel().onIntent(RitualIntent.ProceedToBreathing)

        assertNotNull(player.ambient)
    }

    @Test
    fun `leaving the ritual stops the sound`() = runTest(dispatcher) {
        container.ritualRepository.setSoundEnabled(true)
        val vm = viewModel()
        vm.onIntent(RitualIntent.ProceedToBreathing)

        vm.stopSession()

        assertNull(player.ambient)
    }

    @Test
    fun `a muted ritual stays silent`() = runTest(dispatcher) {
        container.ritualRepository.setSoundEnabled(false)
        viewModel().onIntent(RitualIntent.ProceedToBreathing)

        assertEquals(null, player.ambient)
    }
}
```

> Pozor: `ProceedToBreathing` spouští `startBreathingTimer` s `delay(1000)`. Pod `UnconfinedTestDispatcher` virtuální čas sám neběží, takže test neskončí čekáním 60 s. Konec `runTest` pak ale může hlásit nedokončenou korutinu. V tom případě na konci testu zavolej `vm.stopSession()`, který timer zruší.

- [ ] **Step 2: Ověř, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.SettingsReminderSchedulingTest" --tests "org.clockout.RitualSoundTest"`
Expected: FAIL při kompilaci (`Unresolved reference 'ReminderScheduler'`, `'SoundPlayer'`, …).

- [ ] **Step 3: Implementuj porty a adaptéry**

`domain/platform/ReminderScheduler.kt`:

```kotlin
package org.clockout.domain.platform

/** Plans the daily workday-end reminder; the alarm machinery behind it is the platform's business. */
interface ReminderScheduler {
    fun schedule(hour: Int, minute: Int)
    fun cancel()
}
```

`domain/platform/SoundPlayer.kt`:

```kotlin
package org.clockout.domain.platform

import org.clockout.domain.model.SoundChoice

interface SoundPlayer {
    fun startAmbient(choice: SoundChoice)
    fun triggerBowlStrike(highPitch: Boolean)
    fun playPreview(choice: SoundChoice, onFinished: () -> Unit)
    fun stopAmbient()
}
```

`domain/platform/SoundFileStore.kt`:

```kotlin
package org.clockout.domain.platform

import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.SoundImportResult

/**
 * Keeps user-picked audio in app storage. [source] is the picked document's URI as a string, so
 * the domain stays free of android.net.Uri.
 */
interface SoundFileStore {
    suspend fun import(source: String, existingCount: Int): SoundImportResult
    suspend fun delete(sound: CustomSound)
}
```

`domain/model/SoundImport.kt` obsahuje přesunuté `enum class SoundImportError` a `sealed interface SoundImportResult`, beze změny. Ze `CustomSoundStorage.kt` je smaž a doplň importy (`SettingsState.kt`, `SettingsScreen.kt:107`, `CustomSoundReplacementTest`).

`CustomSoundStorage`:

```kotlin
class CustomSoundStorage(context: Context) : SoundFileStore {
    // ...
    override suspend fun import(source: String, existingCount: Int): SoundImportResult =
        import(Uri.parse(source), existingCount)

    private suspend fun import(uri: Uri, existingCount: Int): SoundImportResult = withContext(Dispatchers.IO) {
        // původní tělo beze změny
    }

    override suspend fun delete(sound: CustomSound) {
        withContext(Dispatchers.IO) { /* původní tělo; výsledek Boolean zahoď */ }
    }
}
```

`AmbientAudioPlayer`:
- `class AmbientAudioPlayer(context: Context) : SoundPlayer` s `private val appContext = context.applicationContext`.
- `startAmbient(choice)` ztratí parametry `context` a `enabled` (jediný volající předává vždy `true`).
- `playPreview(choice, onFinished: () -> Unit)` ztratí `context`.
- `triggerBowlStrike(highPitch: Boolean)` přijde o výchozí hodnotu (rozhraní ji nemá).
- Uvnitř se `context` nahradí za `appContext`.
- Ke čtyřem veřejným metodám přidej `override`.

`notification/AlarmReminderScheduler.kt`:

```kotlin
package org.clockout.notification

import android.content.Context
import org.clockout.domain.platform.ReminderScheduler

class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    override fun schedule(hour: Int, minute: Int) =
        NotificationScheduler.scheduleDailyNotification(appContext, hour, minute)
    override fun cancel() = NotificationScheduler.cancelNotification(appContext)
}
```

`AppContainer`: v konstruktoru si ulož `private val appContext = context.applicationContext` a přidej:

```kotlin
    val reminderScheduler: ReminderScheduler = AlarmReminderScheduler(appContext)
    val soundFileStore: SoundFileStore = CustomSoundStorage(appContext)

    /** A fresh player per screen: each view model stops its own in onCleared. */
    fun newSoundPlayer(): SoundPlayer = AmbientAudioPlayer(appContext)
```

`SettingsViewModel`:
- konstruktor podle *Interfaces*;
- `importSound` seam zmizí, nahradí ho `soundFiles.import(source, count)`;
- `CustomSoundStorage(appContext).delete(…)` → `soundFiles.delete(…)`;
- `NotificationScheduler.scheduleDailyNotification(context, h, m)` → `reminderScheduler.schedule(h, m)`;
- `NotificationScheduler.cancelNotification(context)` → `reminderScheduler.cancel()`;
- podmínky `context != null` zmizí (scheduler se volá vždy, stejně jako dřív v produkci, kde context nikdy nebyl `null`);
- `audioPlayer.playPreview(context.applicationContext, choice) { … }` → `audioPlayer.playPreview(choice) { … }`;
- odstraň importy `android.content.Context`, `android.net.Uri`, `data.local.*`, `notification.*`, `sound.*`.

`RitualViewModel`: odstraň `appContext`, typ `audioPlayer: SoundPlayer` (bez výchozí hodnoty) a `audioPlayer.startAmbient(appContext, choice, true)` → `audioPlayer.startAmbient(choice)`.

`PadlaApp.kt`:

```kotlin
val container = appContainer()
// …
RitualViewModel(
    getTodayRitualUseCase, logMoodUseCase, shouldRequestReviewUseCase, repository,
    audioPlayer = container.newSoundPlayer(),
    analyticsTracker = analyticsTracker
)
// …
SettingsViewModel(
    repository = repository,
    audioPlayer = container.newSoundPlayer(),
    reminderScheduler = container.reminderScheduler,
    soundFiles = container.soundFileStore
)
// callbacky:
onNotificationToggled = { settingsViewModel.setNotificationEnabled(it) },
onTimeSelected = { h, m -> settingsViewModel.setNotificationTime(h, m) },
onCustomTimeSelected = { h, m -> settingsViewModel.setCustomNotificationTime(h, m) },
onTogglePreviewSound = { settingsViewModel.togglePreviewSound(it) },
onUploadCustomSound = { uri -> settingsViewModel.importCustomSound(uri?.toString()) },
onReplaceCustomSound = { uri, sound -> settingsViewModel.replaceCustomSound(uri?.toString(), sound) },
onDeleteCustomSound = { settingsViewModel.deleteCustomSound(it) },
```

`NotificationReceiver`: `NotificationScheduler.scheduleDailyNotification(context, hour, minute)` → `context.appContainer.reminderScheduler.schedule(hour, minute)` a `NotificationScheduler.cancelNotification(context)` → `context.appContainer.reminderScheduler.cancel()`. `showNotification` zůstává (je to UI notifikace, ne plánování).

Existující testy: v `RitualPathCelebrationTest`, `RitualReviewRequestTest`, `RitualSessionSurvivalTest` a `telemetry/RitualTelemetryTest` odstraň `appContext = context` a přidej `audioPlayer = FakeSoundPlayer()`. V `CustomSoundReplacementTest` nahraď `importSound = { _, _, n -> … }` za `soundFiles = FakeSoundFileStore { n -> … }` a doplň `audioPlayer = FakeSoundPlayer()` a `reminderScheduler = FakeReminderScheduler()`. Volání `replaceCustomSound(context, uri, …)` → `replaceCustomSound("content://test/forest.mp3", …)`. V `telemetry/SettingsTelemetryTest` doplň tytéž tři fakes.

- [ ] **Step 4: Ověř**

Run: `grep -rn "android.content.Context\|android.net.Uri" app/src/main/java/org/clockout/presentation/*/*ViewModel.kt` → prázdné.
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Dokumentace**

`CLAUDE.md` + `AGENTS.md`:
- Module layout: řádek `domain/model, domain/usecase` rozšiř o `domain/platform` (porty `ReminderScheduler`, `SoundPlayer`, `SoundFileStore`).
- Persistence: odstavec o `replaceCustomSound` ponech, jen uprav zmínku o `CustomSoundStorage` (nyní `SoundFileStore`).
- What NOT to do: nový bod „Do not hand a `Context` or `Uri` to a ViewModel — go through the ports in `domain/platform`.“

- [ ] **Step 6: Lint a commit (jen se souhlasem)**

`./gradlew :app:lintDebug`, pak po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: platformní porty pro připomínky, zvuk a soubory zvuků

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Úzká rozhraní repozitářů v doméně a strážce vrstev

Strangler, krok 1: `RitualRepository` se rozpadne na čtyři rozhraní v `domain/repository` a dočasně je všechna dědí. Spotřebitelé, tedy ViewModely, use case, Activity a `PadlaApp`, přejdou na úzká rozhraní. Implementace zůstává jedna. Test vrstev od teď hlídá, že presentation nesahá do `data`.

**Files:**
- Create: `app/src/main/java/org/clockout/domain/repository/SettingsRepository.kt`, `SoundRepository.kt`, `RitualLogRepository.kt`, `ProgressRepository.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepository.kt` (jen agregát)
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt` (odstraň `isWithinWorkdayCloseWindow`)
- Modify: `app/src/main/java/org/clockout/AppContainer.kt`
- Modify: všechny 4 soubory v `domain/usecase/`, všechny 4 `*ViewModel.kt`, `PadlaApp.kt`, `PathActivity.kt`, `SkyActivity.kt`, `BreathingStyleActivity.kt`
- Create: `app/src/test/java/org/clockout/LayerDependencyTest.kt`
- Modify: testy používající `container.ritualRepository` nebo `object : RitualRepository by …`
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Consumes: `AppContainer`, `SoundPlayer`, `ReminderScheduler`, `SoundFileStore` (Task 1, 3).
- Produces (balík `org.clockout.domain.repository`):

```kotlin
interface SettingsRepository {
    fun getIncludeWeekends(): Flow<Boolean>
    suspend fun setIncludeWeekends(enabled: Boolean)
    fun getTestMode(): Flow<Boolean>
    suspend fun setTestMode(enabled: Boolean)
    fun getNotificationTime(): Flow<Pair<Int, Int>>
    suspend fun setNotificationTime(hour: Int, minute: Int)
    fun getCustomNotificationTime(): Flow<Pair<Int, Int>?>
    suspend fun setCustomNotificationTime(hour: Int, minute: Int)
    fun getNotificationEnabled(): Flow<Boolean>
    suspend fun setNotificationEnabled(enabled: Boolean)
    fun getLanguage(): Flow<AppLanguage>
    fun getLanguageOption(): Flow<LanguageOption>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setLanguageOption(option: LanguageOption)
    fun getBreathingStyle(): Flow<BreathingStyle>
    suspend fun setBreathingStyle(style: BreathingStyle)
    fun getTelemetryConsent(): Flow<TelemetryConsent>
    suspend fun setTelemetryConsent(consent: TelemetryConsent)
}

interface SoundRepository {
    fun getSoundEnabled(): Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)
    fun getSoundChoice(): Flow<SoundChoice>
    suspend fun setSoundChoice(choice: SoundChoice)
    fun getCustomSounds(): Flow<List<CustomSound>>
    suspend fun addCustomSound(sound: CustomSound)
    suspend fun removeCustomSound(id: String)
}

interface RitualLogRepository {
    fun getTodayQuestion(): RitualQuestion
    fun getTodayDateString(): String
    fun getLogs(): Flow<List<RitualLogEntry>>
    fun getStreakInfo(): Flow<RitualStreakInfo>
    /** Records a finished ritual: the log entry, its path day, and the lifetime completion count. */
    suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String)
    suspend fun getCompletedRitualCount(): Int
    suspend fun isReviewPromptShown(): Boolean
    suspend fun markReviewPromptShown()
    /** Empties the history; the path, the counter and the review flag survive it. */
    suspend fun clearLogs()
}

interface ProgressRepository {
    /** The path's stage and weekly rhythm; survives [RitualLogRepository.clearLogs]. */
    fun getPathProgress(): Flow<PathProgress>
    /** The night sky: practice days, the entries still in the history, and the rhythm per week. */
    fun getSky(): Flow<SkyData>
}
```

  - Dočasně: `interface RitualRepository : SettingsRepository, SoundRepository, RitualLogRepository, ProgressRepository` (bez vlastních členů).
  - `AppContainer`: `val settings: SettingsRepository`, `val sounds: SoundRepository`, `val ritualLogs: RitualLogRepository`, `val progress: ProgressRepository`. V tomto úkolu všechny čtyři vrací tutéž instanci `RitualRepositoryImpl`. `ritualRepository` z kontejneru zmizí.
  - Konstruktory:
    - `HomeViewModel(logs: RitualLogRepository, settings: SettingsRepository, progress: ProgressRepository, getTodayRitualUseCase, getStreakUseCase, isWithinWindow: () -> Boolean = WorkdayCloseWindow::isOpenNow)`
    - `HistoryViewModel(logs: RitualLogRepository, settings: SettingsRepository, progress: ProgressRepository)`
    - `SettingsViewModel(settings: SettingsRepository, sounds: SoundRepository, logs: RitualLogRepository, audioPlayer, reminderScheduler, soundFiles)`
    - `RitualViewModel(getTodayRitualUseCase, logMoodUseCase, shouldRequestReviewUseCase, logs: RitualLogRepository, settings: SettingsRepository, sounds: SoundRepository, progress: ProgressRepository, audioPlayer, analyticsTracker)`
  - Use case berou `RitualLogRepository` místo `RitualRepository`.

- [ ] **Step 1: Napiš padající test vrstev**

`app/src/test/java/org/clockout/LayerDependencyTest.kt`:

```kotlin
package org.clockout

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the direction of dependencies: presentation sees the domain but never the data layer, and
 * the domain sees nothing Android at all. Reads the sources, so it needs no Robolectric.
 */
class LayerDependencyTest {

    private val root = File("src/main/java/org/clockout")

    private fun importsIn(layer: String): List<Pair<String, String>> =
        File(root, layer).walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().filter { it.startsWith("import ") }.map { file.relativeTo(root).path to it }
        }.toList()

    @Test
    fun `presentation never imports the data layer`() {
        val offending = importsIn("presentation").filter { (_, line) -> line.startsWith("import org.clockout.data.") }
        assertTrue("Presentation reaches into data:\n${offending.joinToString("\n")}", offending.isEmpty())
    }

    @Test
    fun `the domain imports nothing from android, the data layer or presentation`() {
        val forbidden = listOf(
            "import android.", "import androidx.", "import org.clockout.R",
            "import org.clockout.data.", "import org.clockout.presentation."
        )
        val offending = importsIn("domain").filter { (_, line) -> forbidden.any(line::startsWith) }
        assertTrue("The domain is not pure:\n${offending.joinToString("\n")}", offending.isEmpty())
    }
}
```

- [ ] **Step 2: Ověř, že test padá**

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.LayerDependencyTest"`
Expected: FAIL. Hlášení vyjmenuje `presentation/*ViewModel.kt` s `import org.clockout.data.repository.RitualRepository` a `domain/usecase/*.kt` se stejným importem. Jiné řádky se objevit nesmějí; Task 2 a 3 už zbytek odstranily. Pokud se objeví, vrať se k příslušnému úkolu.

- [ ] **Step 3: Implementuj rozhraní a zúžení spotřebitelů**

1. Vytvoř čtyři soubory v `domain/repository/` podle *Interfaces* (importy `kotlinx.coroutines.flow.Flow` a `org.clockout.domain.model.*`). KDoc u `setCustomNotificationTime` odkazuje na `RitualPreferences`, který doména nezná, takže ho přepiš: `/** Sets the active time from the picker and remembers it as the custom one, in one write. */`.
2. `data/repository/RitualRepository.kt` zredukuj na:

```kotlin
package org.clockout.data.repository

import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.repository.SoundRepository

/** Transitional: the one implementation still serves every narrow repository. */
interface RitualRepository : SettingsRepository, SoundRepository, RitualLogRepository, ProgressRepository
```

3. Z `RitualRepositoryImpl` smaž `isWithinWorkdayCloseWindow()` a import `WorkdayCloseWindow`.
4. `AppContainer`:

```kotlin
    private val ritualRepository = RitualRepositoryImpl(/* jako dosud */)
    val settings: SettingsRepository = ritualRepository
    val sounds: SoundRepository = ritualRepository
    val ritualLogs: RitualLogRepository = ritualRepository
    val progress: ProgressRepository = ritualRepository
```

5. Use case: `private val repository: RitualLogRepository` a import z `org.clockout.domain.repository`.
6. `HomeViewModel`: `ritualRepository.getTestMode()` / `getLanguage()` / `getIncludeWeekends()` / `setTestMode()` → `settings.…`, `getPathProgress()` → `progress.…`, `ritualRepository.isWithinWorkdayCloseWindow()` → `isWithinWindow()`.
7. `HistoryViewModel`: `getSky()` → `progress`, `getLanguage()` → `settings`, `clearLogs()` → `logs`.
8. `SettingsViewModel`: notifikace, test mode, víkendy, jazyk, styl dechu a souhlas → `settings`; zvuky → `sounds`; `clearLogs()` → `logs`.
9. `RitualViewModel`: `getLanguage()` / `getBreathingStyle()` → `settings`, `getSoundEnabled()` / `getSoundChoice()` → `sounds`, `getPathProgress()` / `getSky()` → `progress`, `getStreakInfo()` → `logs`.
10. `PadlaApp`: místo `repository` vezmi `val container = appContainer()` a předávej `container.settings` atd. Souhlas s telemetrií čti z `container.settings.getTelemetryConsent()`.
11. Activity: `PathActivity` → `settings.getLanguage()` + `progress.getPathProgress()`; `SkyActivity` → `settings.getLanguage()` + `progress.getSky()`; `BreathingStyleActivity` → `settings`.
12. Testy:
    - `container.ritualRepository.setX(…)` → `container.settings.setX(…)` / `container.sounds…` / `container.ritualLogs…` podle metody.
    - `HistoryViewModelSkyTest`: `object : RitualRepository by repository { override fun getLogs() … }` → `object : RitualLogRepository by container.ritualLogs { … }` a VM `HistoryViewModel(counting, container.settings, container.progress)`.
    - `RitualPathCelebrationTest.brokenSky`: `object : ProgressRepository by container.progress { override fun getSky() … }`.
    - Konstruktory VM uprav podle *Interfaces* (i v `RitualSoundTest` a `SettingsReminderSchedulingTest` z Task 3).

- [ ] **Step 4: Ověř**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS včetně `LayerDependencyTest`.

- [ ] **Step 5: Dokumentace**

`CLAUDE.md` + `AGENTS.md`:
- Module layout: přidej `domain/repository` (rozhraní repozitářů) a do `data/repository` uveď, že obsahuje implementace.
- Architecture: věty „`RitualRepository.getPathProgress()` feeds…“ a „`RitualRepository.getSky()` assembles…“ přepiš na `ProgressRepository`. U `HomeState.canStartRitual` nahraď `RitualRepositoryImpl.isWithinWorkdayCloseWindow()` za `WorkdayCloseWindow.isOpenNow()` předané do `HomeViewModel` jako `isWithinWindow`.
- Testing: odstavec o `LayerDependencyTest`, tj. co hlídá a že čte zdrojáky relativně k `app/`.

- [ ] **Step 6: Lint a commit (jen se souhlasem)**

`./gradlew :app:lintDebug`, pak po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: úzká rozhraní repozitářů v doméně a test směru závislostí

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Rozdělení implementace repozitáře

Strangler, krok 2: z `RitualRepositoryImpl` vzniknou čtyři třídy a agregát zmizí. Spotřebitele to neovlivní, protože od Task 4 vidí jen úzká rozhraní; mění se pouze `AppContainer`.

**Files:**
- Create: `app/src/main/java/org/clockout/data/repository/SettingsRepositoryImpl.kt`, `SoundRepositoryImpl.kt`, `RitualLogRepositoryImpl.kt`, `ProgressRepositoryImpl.kt`, `PracticeDayParsing.kt`
- Delete: `app/src/main/java/org/clockout/data/repository/RitualRepository.kt`, `RitualRepositoryImpl.kt`
- Modify: `app/src/main/java/org/clockout/AppContainer.kt`
- Create: `app/src/test/java/org/clockout/SharedMigratorTest.kt`
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Consumes: rozhraní z Task 4.
- Produces:
  - `class SettingsRepositoryImpl(preferences: RitualPreferences) : SettingsRepository`
  - `class SoundRepositoryImpl(preferences: RitualPreferences, soundFileExists: (String) -> Boolean = { true }) : SoundRepository`
  - `class RitualLogRepositoryImpl(preferences: RitualPreferences, ritualLogDao: RitualLogDao, practiceDayDao: PracticeDayDao, logMigrator: RitualLogMigrator) : RitualLogRepository`
  - `class ProgressRepositoryImpl(practiceDayDao: PracticeDayDao, logMigrator: RitualLogMigrator, logs: RitualLogRepository, includeWeekends: Flow<Boolean>, todayFlow: Flow<LocalDate>) : ProgressRepository`
  - `internal fun parsePracticeDay(date: String): LocalDate?`
  - `AppContainer.logMigrator: RitualLogMigrator`, jediná instance sdílená oběma repozitáři.

- [ ] **Step 1: Napiš padající test sdíleného migrátoru**

```kotlin
package org.clockout

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.clockout.testing.testAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The legacy import is guarded by a per-instance flag and mutex; two migrators would import the
 * pre-Room blob twice. The history and the path must therefore share one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SharedMigratorTest {

    @get:Rule val folder = TemporaryFolder()
    private lateinit var container: AppContainer

    @Before fun setUp() { container = testAppContainer(folder) }
    @After fun tearDown() { container.database.close() }

    @Test
    fun `history and path share one migrator`() {
        assertSame(container.logMigrator, container.logMigrator)
    }

    @Test
    fun `a logged ritual reaches both the history and the path`() = runTest {
        container.ritualLogs.logRitual("An answer", org.clockout.domain.model.MoodTag.CALM, "Q")

        assertEquals(1, container.ritualLogs.getLogs().first().size)
        assertEquals(1, container.progress.getPathProgress().first().closedDays)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.SharedMigratorTest"`
Expected: FAIL (`Unresolved reference 'logMigrator'`).

- [ ] **Step 2: Rozděl implementaci**

Metody se přesouvají 1:1 z `RitualRepositoryImpl`, beze změny těla:
- `SettingsRepositoryImpl`: všechny gettery a settery ze `SettingsRepository`, každý jako jednořádkový delegát na `preferences`.
- `SoundRepositoryImpl`: `customSoundsFlow` (s `soundFileExists`), `getSoundChoice` (combine), `getSoundEnabled` a `setSoundEnabled`, `getCustomSounds`, `add`/`remove`, `setSoundChoice`. KDoc k `soundFileExists` se přesouvá sem.
- `RitualLogRepositoryImpl`: `dateFormat`, `logsFlow`, `getTodayQuestion` (přes `QuestionCatalog`), `getTodayDateString`, `getStreakInfo` + `calculateStreakInfo` (víkendy z `preferences.includeWeekendsFlow`), `logRitual` (beze změny, včetně komentáře o jednom čtení hodin), `getCompletedRitualCount`, `isReviewPromptShown`, `markReviewPromptShown`, `clearLogs` (beze změny, **nemaže** `practice_days`).
- `ProgressRepositoryImpl`:

```kotlin
class ProgressRepositoryImpl(
    private val practiceDayDao: PracticeDayDao,
    private val logMigrator: RitualLogMigrator,
    private val logs: RitualLogRepository,
    private val includeWeekends: Flow<Boolean>,
    /** Today, re-emitted when the date changes under an open screen; see Context.todayFlow. */
    private val todayFlow: Flow<LocalDate>
) : ProgressRepository {

    private val practiceDaysFlow: Flow<Set<LocalDate>> = flow {
        logMigrator.ensureMigrated()
        emitAll(practiceDayDao.observeAll().map { dates -> dates.mapNotNull(::parsePracticeDay).toSet() })
    }

    override fun getPathProgress(): Flow<PathProgress> =
        practiceDaysFlow.combine(todayFlow) { days, today -> PathPolicy.progress(days = days, today = today) }
            // The rhythm walks the whole history; keep that off the main thread.
            .flowOn(Dispatchers.Default)

    override fun getSky(): Flow<SkyData> =
        combine(practiceDaysFlow, logs.getLogs(), todayFlow, includeWeekends) { days, entries, today, weekends ->
            SkyData(
                days = days,
                logs = entries.mapNotNull { entry -> parsePracticeDay(entry.date)?.let { it to entry } }.toMap(),
                rhythmWeeks = PathRhythm.evaluate(days, today).weeks,
                today = today,
                includeWeekends = weekends
            )
        }.flowOn(Dispatchers.Default)
}
```

- `PracticeDayParsing.kt`: `internal fun parsePracticeDay(date: String): LocalDate?`, tělo a KDoc přesunuté beze změny z `RitualRepositoryImpl`.

`AppContainer`:

```kotlin
    val logMigrator = RitualLogMigrator(preferences, database.ritualLogDao(), database.practiceDayDao())
    val settings: SettingsRepository = SettingsRepositoryImpl(preferences)
    val sounds: SoundRepository = SoundRepositoryImpl(preferences, soundFileExists)
    val ritualLogs: RitualLogRepository =
        RitualLogRepositoryImpl(preferences, database.ritualLogDao(), database.practiceDayDao(), logMigrator)
    val progress: ProgressRepository = ProgressRepositoryImpl(
        practiceDayDao = database.practiceDayDao(),
        logMigrator = logMigrator,
        logs = ritualLogs,
        includeWeekends = settings.getIncludeWeekends(),
        todayFlow = todayFlow
    )
```

Smaž `RitualRepository.kt` a `RitualRepositoryImpl.kt`. Testy, které typují `RitualRepositoryImpl` (`PracticeDayPersistenceTest`, `RitualPathCelebrationTest`, `HistoryViewModelSkyTest`), přepni na typy z kontejneru. `RitualLogMigratorTest` se nemění.

- [ ] **Step 3: Ověř**

Run: `grep -rn "RitualRepository\b\|RitualRepositoryImpl" app/src` → prázdné.
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. Pozornost věnuj zejména `PracticeDayPersistenceTest`, `RitualLogMigratorTest`, `ClockoutDatabaseMigrationTest`, `CustomSoundAvailabilityTest`, `HistoryViewModelSkyTest` a `RitualWeekOverviewTest`.

- [ ] **Step 4: Dokumentace**

`CLAUDE.md` + `AGENTS.md`, nahraď všechny výskyty `RitualRepositoryImpl` podle toho, kam metoda odešla:
- `logRitual` a počítadlo → `RitualLogRepositoryImpl`;
- `todayFlow` → `ProgressRepositoryImpl`;
- `soundFileExists` → `SoundRepositoryImpl`.

Do What NOT to do přidej: „Do not give the history and the path separate `RitualLogMigrator` instances — `AppContainer.logMigrator` is the only one.“

- [ ] **Step 5: Lint a commit (jen se souhlasem)**

`./gradlew :app:lintDebug`, pak po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: rozdělení repozitáře na nastavení, zvuky, historii a postup

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Use case — pryč s přeposíláním, `CompleteRitualUseCase`

`GetStreakUseCase`, `GetTodayRitualUseCase` a `LogMoodUseCase` jen přeposílají volání, což guide nedoporučuje. Skutečná doménová logika, tedy co dokončený rituál změnil na cestě a na obloze a zda požádat o recenzi, dnes sedí v `RitualViewModel.handleMoodSelected`.

**Files:**
- Delete: `app/src/main/java/org/clockout/domain/usecase/GetStreakUseCase.kt`, `GetTodayRitualUseCase.kt`, `LogMoodUseCase.kt`
- Create: `app/src/main/java/org/clockout/domain/usecase/CompleteRitualUseCase.kt`
- Create: `app/src/main/java/org/clockout/domain/model/RitualCompletion.kt` (+ přesun `SkyTeaser` z `presentation/ritual/RitualState.kt:36`)
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt`, `RitualState.kt`
- Modify: `app/src/main/java/org/clockout/presentation/home/HomeViewModel.kt`
- Modify: `app/src/main/java/org/clockout/AppContainer.kt`, `presentation/PadlaApp.kt`
- Create: `app/src/test/java/org/clockout/CompleteRitualUseCaseTest.kt`
- Modify: `app/src/test/java/org/clockout/testing/Fakes.kt`
- Modify: testy stavějící `RitualViewModel(` / `HomeViewModel(`
- Modify: `CLAUDE.md`, `AGENTS.md`

**Interfaces:**
- Consumes: `RitualLogRepository`, `ProgressRepository` (Task 4/5), `ShouldRequestReviewUseCase(repository: RitualLogRepository)`.
- Produces:

```kotlin
data class SkyTeaser(val stars: Int, val daysToFirstConstellation: Int)          // domain/model
data class RitualCompletion(
    val reachedStage: PathStage?,
    val litConstellationNumber: Int?,
    val skyTeaser: SkyTeaser?,
    val shouldRequestReview: Boolean,
    val daysClosedThisWeek: Int
)
class CompleteRitualUseCase(
    logs: RitualLogRepository,
    progress: ProgressRepository,
    shouldRequestReview: ShouldRequestReviewUseCase,
    today: () -> LocalDate = LocalDate::now
) { suspend operator fun invoke(answer: String?, mood: MoodTag, questionText: String, moodWasChosen: Boolean): RitualCompletion }
```

  - `RitualViewModel(logs, settings, sounds, completeRitual: CompleteRitualUseCase, audioPlayer, analyticsTracker = NoOpAnalyticsTracker)`
  - `HomeViewModel(logs, settings, progress, isWithinWindow = WorkdayCloseWindow::isOpenNow)`
  - `AppContainer.completeRitual: CompleteRitualUseCase`

- [ ] **Step 1: Napiš padající testy (čisté JVM s fakes)**

Do `testing/Fakes.kt` přidej:

```kotlin
class FakeRitualLogRepository(
    private val question: RitualQuestion,
    var completedCount: Int = 0,
    var reviewShown: Boolean = false
) : RitualLogRepository {
    val logged = mutableListOf<Triple<String?, MoodTag, String>>()
    private val logs = MutableStateFlow<List<RitualLogEntry>>(emptyList())
    override fun getTodayQuestion() = question
    override fun getTodayDateString() = LocalDate.now().toString()
    override fun getLogs(): Flow<List<RitualLogEntry>> = logs
    override fun getStreakInfo(): Flow<RitualStreakInfo> = flowOf(
        RitualStreakInfo(completedThisWeekCount = logged.size, targetWorkDays = 5,
            isTodayCompleted = logged.isNotEmpty(), todayLog = null, weekDays = emptyList())
    )
    override suspend fun logRitual(selectedAnswer: String?, mood: MoodTag, questionText: String) {
        logged += Triple(selectedAnswer, mood, questionText)
        completedCount++
    }
    override suspend fun getCompletedRitualCount() = completedCount
    override suspend fun isReviewPromptShown() = reviewShown
    override suspend fun markReviewPromptShown() { reviewShown = true }
    override suspend fun clearLogs() = Unit
}

/** Answers with [snapshots] in turn: the first read is "before" the log, the second "after". */
class FakeProgressRepository(
    private val paths: ArrayDeque<PathProgress>,
    private val skies: ArrayDeque<SkyData>,
    private val failure: Exception? = null
) : ProgressRepository {
    override fun getPathProgress(): Flow<PathProgress> = flow { failure?.let { throw it }; emit(paths.removeFirst()) }
    override fun getSky(): Flow<SkyData> = flow { failure?.let { throw it }; emit(skies.removeFirst()) }
}
```

> `PathProgress` a `SkyData` v testech nikdy neskládej ručně. Vždy je vezmi z reálné politiky: `PathPolicy.progress(days = …, today = …)` a `SkyData(days, logs = emptyMap(), rhythmWeeks = PathRhythm.evaluate(days, today).weeks, today, includeWeekends = false)`.

`CompleteRitualUseCaseTest.kt`:

```kotlin
package org.clockout

import kotlinx.coroutines.test.runTest
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathRhythm
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.SkyData
import org.clockout.domain.usecase.CompleteRitualUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.testing.FakeProgressRepository
import org.clockout.testing.FakeRitualLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import java.time.LocalDate
import org.junit.Test

class CompleteRitualUseCaseTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val question = RitualQuestion(
        id = "q", textEn = "Q?", textCs = "O?", suggestedAnswersEn = listOf("A"), suggestedAnswersCs = listOf("A")
    )

    private fun sky(days: Set<LocalDate>) =
        SkyData(days, emptyMap(), PathRhythm.evaluate(days, today).weeks, today, includeWeekends = false)

    private fun progress(before: Set<LocalDate>, after: Set<LocalDate>, failure: Exception? = null) =
        FakeProgressRepository(
            paths = ArrayDeque(listOf(PathPolicy.progress(before, today), PathPolicy.progress(after, today))),
            skies = ArrayDeque(listOf(sky(before), sky(after))),
            failure = failure
        )

    private fun useCase(logs: FakeRitualLogRepository, progress: FakeProgressRepository) =
        CompleteRitualUseCase(logs, progress, ShouldRequestReviewUseCase(logs), today = { today })

    @Test
    fun `the first closed day reaches the first stage`() = runTest {
        val logs = FakeRitualLogRepository(question)
        val result = useCase(logs, progress(emptySet(), setOf(today)))("A", MoodTag.CALM, "Q?", moodWasChosen = true)

        assertEquals(PathStage.LAPTOP_CLOSED, result.reachedStage)
        assertEquals(1, logs.logged.size)
    }

    @Test
    fun `a broken sky still writes the entry and celebrates nothing`() = runTest {
        val logs = FakeRitualLogRepository(question)
        val result = useCase(logs, progress(emptySet(), setOf(today), IllegalStateException("sky")))(
            "A", MoodTag.CALM, "Q?", moodWasChosen = true
        )

        assertEquals(1, logs.logged.size)
        assertNull(result.reachedStage)
        assertNull(result.litConstellationNumber)
        assertNull(result.skyTeaser)
    }

    @Test
    fun `a skipped mood never asks for a review`() = runTest {
        val logs = FakeRitualLogRepository(question, completedCount = 50)
        val result = useCase(logs, progress(emptySet(), setOf(today)))(null, MoodTag.CALM, "Q?", moodWasChosen = false)

        assertFalse(result.shouldRequestReview)
        assertFalse(logs.reviewShown)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "org.clockout.CompleteRitualUseCaseTest"`
Expected: FAIL (`Unresolved reference 'CompleteRitualUseCase'`).

- [ ] **Step 2: Implementuj use case**

`domain/model/RitualCompletion.kt` obsahuje `SkyTeaser` (přesunutý z `RitualState.kt`, beze změny) a `RitualCompletion` podle *Interfaces*.

`domain/usecase/CompleteRitualUseCase.kt`:

```kotlin
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
     * [moodWasChosen] is false when the user skipped the step: the history still gets CALM, but
     * that is not the user saying the day went well and must never earn a review request.
     */
    suspend operator fun invoke(
        answer: String?,
        mood: MoodTag,
        questionText: String,
        moodWasChosen: Boolean
    ): RitualCompletion {
        val before = snapshot()
        logs.logRitual(answer, mood, questionText)
        val after = snapshot()
        val thisMonday = today().mondayOfWeek()
        return RitualCompletion(
            reachedStage = if (before != null && after != null) {
                after.path.stage.takeIf { it != before.path.stage }
            } else {
                null
            },
            // Announced when this week's figure first gets a line, not merely when the week reaches
            // its rhythm: a first, partial week can be in rhythm on a lone star.
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
            // Read after the log is written, so the count includes today.
            daysClosedThisWeek = logs.getStreakInfo().first().completedThisWeekCount
        )
    }

    private class Snapshot(val path: PathProgress, val sky: SkyData)

    /**
     * The path and the sky only decorate the completion card: whatever goes wrong while reading
     * them, the ritual itself must still be written, so a failure yields no celebration.
     */
    private suspend fun snapshot(): Snapshot? = try {
        Snapshot(progress.getPathProgress().first(), progress.getSky().first())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        null
    }
}
```

`RitualViewModel.handleMoodSelected`:

```kotlin
    private fun handleMoodSelected(mood: MoodTag, moodWasChosen: Boolean = true) {
        _state.update { it.copy(selectedMood = mood) }
        viewModelScope.launch {
            val current = _state.value
            val completion = completeRitual(
                answer = current.customAnswerText.trim().ifEmpty { current.selectedAnswer },
                mood = mood,
                questionText = current.question.text(current.language),
                moodWasChosen = moodWasChosen
            )
            _state.update {
                it.copy(
                    isCompletedAndSaved = true,
                    shouldRequestReview = completion.shouldRequestReview,
                    reachedStage = completion.reachedStage,
                    rhythmConstellationNumber = completion.litConstellationNumber,
                    skyTeaser = completion.skyTeaser
                )
            }
            analyticsTracker.track(AnalyticsEvent.RitualStepCompleted(RitualStep.Log.analyticsId()))
            analyticsTracker.track(
                AnalyticsEvent.RitualCompleted(mood = mood.id, daysClosedThisWeek = completion.daysClosedThisWeek)
            )
        }
    }
```

Z `RitualViewModel` smaž `ProgressSnapshot` a `progressSnapshot()`. `getTodayRitualUseCase()` → `logs.getTodayQuestion()`, závislost na `progress` z konstruktoru odpadá. V `RitualState.kt` smaž `SkyTeaser` a importuj ho z `domain.model`.

`HomeViewModel`: `getTodayRitualUseCase()` → `logs.getTodayQuestion()`, `getStreakUseCase()` → `logs.getStreakInfo()`.

Smaž tři přeposílací use case. `AppContainer`:

```kotlin
    val completeRitual = CompleteRitualUseCase(ritualLogs, progress, ShouldRequestReviewUseCase(ritualLogs))
```

`PadlaApp`: smaž čtyři `remember { …UseCase(repository) }` a předávej `container.completeRitual`.

Testy: `RitualPathCelebrationTest`, `RitualReviewRequestTest`, `RitualSessionSurvivalTest`, `RitualSoundTest` a `telemetry/RitualTelemetryTest` dostanou `completeRitual = container.completeRitual`. `brokenSky` v `RitualPathCelebrationTest` sestaví vlastní `CompleteRitualUseCase(container.ritualLogs, brokenProgress, ShouldRequestReviewUseCase(container.ritualLogs))`.

- [ ] **Step 3: Ověř**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, a to včetně `RitualPathCelebrationTest`, `RitualReviewRequestTest`, `ReviewPromptFlowTest` a `telemetry/RitualTelemetryTest`. Ty pinují chování, které se právě přesunulo.

- [ ] **Step 4: Dokumentace**

`CLAUDE.md` + `AGENTS.md`:
- Architecture, odstavec o review: „`ShouldRequestReviewUseCase` applies the policy…; `CompleteRitualUseCase` calls it after the write, `RitualViewModel` raises…“.
- Architecture, odstavec o path: „`RitualViewModel` reads it before and after logging“ → „`CompleteRitualUseCase` reads…“.
- Persistence: „`RitualViewModel` reads the path and the sky around that write…“ → `CompleteRitualUseCase`.
- Telemetry: věta o gate na `selectedMood` zůstává (žije ve VM).

- [ ] **Step 5: Lint a commit (jen se souhlasem)**

`./gradlew :app:lintDebug`, pak po souhlasu:

```bash
git add -A app/src CLAUDE.md AGENTS.md
git commit -m "refactor: CompleteRitualUseCase místo přeposílacích use case

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 7 (volitelný): `:core:domain` jako čistě JVM modul

Vrstvení dosud hlídá `LayerDependencyTest`. Po této změně ho u domény vynutí kompilátor, protože JVM modul nemá na classpath Android. Úkol proveď jen tehdy, když o to uživatel výslovně požádá.

**Files:**
- Modify: `gradle/libs.versions.toml` (plugin `kotlin-jvm`)
- Modify: `build.gradle.kts` (root), `settings.gradle.kts`, `app/build.gradle.kts`
- Create: `core/domain/build.gradle.kts`
- Move: `app/src/main/java/org/clockout/domain/**` → `core/domain/src/main/kotlin/org/clockout/domain/**`
- Move: čisté doménové testy → `core/domain/src/test/kotlin/org/clockout/**`
- Modify: `app/src/test/java/org/clockout/LayerDependencyTest.kt`
- Modify: `CLAUDE.md`, `AGENTS.md`

- [ ] **Step 1: Ověř předpoklad**

Run: `grep -rnE "^import (android|androidx|org\.clockout\.(R|data|presentation|BuildConfig))" app/src/main/java/org/clockout/domain`
Expected: prázdné (zajistil Task 4). Pokud ne, oprav zdroj, ne tento krok.

- [ ] **Step 2: Gradle**

`gradle/libs.versions.toml` do `[plugins]`:

```toml
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

Root `build.gradle.kts` do `plugins { }`: `alias(libs.plugins.kotlin.jvm) apply false`.

`settings.gradle.kts`: `include(":core:domain")`.

`core/domain/build.gradle.kts`:

```kotlin
plugins { alias(libs.plugins.kotlin.jvm) }

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

dependencies {
  api(libs.kotlinx.coroutines.core)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
```

> AGP 9 má Kotlin vestavěný. Pokud Gradle zahlásí konflikt verze KGP mezi `kotlin-jvm` a AGP, ověř postup v aktuální dokumentaci AGP 9 („built-in Kotlin“ + čistě JVM moduly) a řiď se jí. Verzi nenatvrdo nepiš do build souboru.

`app/build.gradle.kts` do `dependencies`: `implementation(project(":core:domain"))`.

- [ ] **Step 3: Přesuň zdroje a testy**

```bash
mkdir -p core/domain/src/main/kotlin/org/clockout core/domain/src/test/kotlin/org/clockout
git mv app/src/main/java/org/clockout/domain core/domain/src/main/kotlin/org/clockout/domain
# Čisté doménové testy: bez Robolectricu, bez android/androidx a bez importů mimo domain
for f in $(grep -L "RobolectricTestRunner\|import android\|import androidx\|org.clockout.presentation\|org.clockout.data\|org.clockout.testing" app/src/test/java/org/clockout/*.kt); do
  grep -q "import org.clockout.domain" "$f" && echo "$f"
done
```

Vypsané soubory zkontroluj ručně (například `CompleteRitualUseCaseTest` používá `testing/Fakes.kt`, takže fakes repozitářů přesuň s ním do `core/domain/src/test/kotlin/org/clockout/testing/DomainFakes.kt`). Pak je přesuň `git mv` do `core/domain/src/test/kotlin/org/clockout/`.

`LayerDependencyTest`: test domény smaž (hlídá ho kompilátor), test presentation zůstává.

- [ ] **Step 4: Ověř**

Run: `./gradlew :core:domain:test :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Ověř i release build, protože R8 a shrinkResources se stejným kódem v jiném modulu chovají jinak: `./gradlew :app:assembleRelease`. Bez podepisovacího klíče stačí, že build projde konfigurací a R8.

- [ ] **Step 5: Dokumentace**

`CLAUDE.md` + `AGENTS.md`:
- Module layout: dva moduly, `:app` a `:core:domain` (čistě JVM).
- Commands: `./gradlew :core:domain:test`.
- Key stack versions: plugin `kotlin-jvm`.
- What NOT to do: „Do not add an Android dependency to `:core:domain`“.

- [ ] **Step 6: Commit (jen se souhlasem)**

```bash
git add -A settings.gradle.kts build.gradle.kts gradle/libs.versions.toml app core CLAUDE.md AGENTS.md
git commit -m "refactor: doména jako samostatný JVM modul :core:domain

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: Závěrečná verifikace

- [ ] **Step 1: Plná sada**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Screenshoty se změnit nesmějí, refaktoring nesahá na UI. Pokud se liší `PhoneNavigationTest` nebo `TabletNavigationTest`, nejdřív zkontroluj datum (rotace otázky) a den v týdnu, než to budeš považovat za regresi.

- [ ] **Step 2: Ruční smoke test na zařízení/emulátoru**

Pro každý bod sleduj logcat bez výjimek:
1. Dokonči rituál. Musí naskočit oslava etapy a záznam v historii.
2. Změň čas připomínky a vypni ji a zapni. Ověř `adb shell dumpsys alarm | grep org.clockout`.
3. Importuj vlastní zvuk a přehraj náhled.
4. Otevři Cestu, Oblohu a výběr dechu, změň jazyk a vrať se.
5. Restartuj zařízení (`adb reboot`) a ověř, že alarm je znovu naplánovaný.

- [ ] **Step 3: Kontrola dokumentace**

Run: `grep -n "RitualRepository\b\|RitualRepositoryImpl\|rememberRitualRepository\|QuestionsData\|GetStreakUseCase\|LogMoodUseCase\|GetTodayRitualUseCase" CLAUDE.md AGENTS.md`
Expected: prázdné.

- [ ] **Step 4: Dokončení větve**

Použij superpowers:finishing-a-development-branch. Merge nebo PR do `develop` jen se souhlasem uživatele.
