# Cesta — stupně a týdenní rytmus: implementační plán

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Přidat do Padla deset stupňů „Cesty domů" a týdenní rytmus, odvozené z nové tabulky `practice_days`, která přežije smazání historie.

**Architecture:** Čistá doména (`PathStage`, `PathRhythm`, `PathPolicy` nad `java.time.LocalDate`) počítá `PathProgress` z množiny dat. Data dodává nová Room tabulka `practice_days` (DB v2, migrace ji naplní z `ritual_logs`); `RitualRepository.getPathProgress()` ji kombinuje s nastavením víkendů. UI: karta na Home, oslava na dokončovací kartě rituálu a samostatná `PathActivity` po vzoru `BreathingStyleActivity`.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Room 2.8.5 + KSP, DataStore, Robolectric 4.17, Roborazzi 1.76.0.

**Spec:** `docs/superpowers/specs/2026-10-09-cesta-gamifikace-design.md`

## Global Constraints

- **Git:** nikdy necommitovat, nestageovat ani nepushovat bez výslovného souhlasu uživatele v témže tahu (CLAUDE.md). Krok „Commit" v každém úkolu znamená: ukázat diff, zeptat se, a teprve po „ano" commitnout. Commit message česky s prefixem (`feat:`, `test:`, `docs:` …) a řádkem `Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>`.
- Práce probíhá na větvi `feature/CL_cesta` odbočené z `develop`.
- `domain/` neobsahuje `R`, androidx importy ani texty pro uživatele.
- Texty jen v `res/values/strings.xml` a `res/values-cs/strings.xml`, vždy se stejnou sadou klíčů; české plurály `one`/`few`/`many`/`other`.
- Jen statické `R.string.*` / `R.plurals.*` / `R.drawable.*` (release běží s `isShrinkResources = true`).
- Testy obrazovek obalit `AppLocalization(language)`; test měřící text potřebuje `@GraphicsMode(GraphicsMode.Mode.NATIVE)` a aserci na `lineCount`.
- Robolectric testy mají `@Config(sdk = [36])`.
- Verze knihoven jen přes `gradle/libs.versions.toml`.
- `practice_days` drží **jen datum**; `clearLogs()` ji **nesmí** smazat.
- Prahy stupňů: 0 / 1 / 5 / 10 / 20 / 35 / 50 / 75 / 100 / 150 / 250 dní.
- Práh týdne v rytmu = `max(1, nabízené dny − 1)`; rytmus ukončí až dva nesplněné ukončené týdny po sobě; rozpracovaný aktuální týden neláme.
- Každá změna kódu jde se současnou úpravou `CLAUDE.md` **a** `AGENTS.md` (Úkol 8).

## Review Focus

1. **Uživatel z nejstarší verze (historie v DataStore blobu)** — po aktualizaci musí mít správný stupeň; legacy migrátor běží až po Room migraci, takže sám plní i `practice_days`. Test: Úkol 3, krok 1 (`legacy logs also become practice days`).
2. **Opakovaný rituál v jeden den** — nesmí přidat druhý den ani podruhé oslavit stupeň. Testy: Úkol 3 (`logging twice on the same day counts one practice day`), Úkol 7 (`a second ritual on the same day celebrates nothing`).
3. **Smazání historie z Nastavení i z Historie** — stupeň i rytmus zůstanou. Test: Úkol 3 (`clearLogs keeps the practice days`).
4. **Neparsovatelné datum v tabulce** (ruční zásah, budoucí bug formátu) — nesmí shodit Home; řádek se přeskočí. Test: Úkol 3 (`an unparsable date is skipped rather than crashing`).
5. **Nejdelší český název stupně na úzkém telefonu** („Zaklapnutý notebook") — vejde se nejvýš na dva řádky. Test: Úkol 5 (`PathCardTitleFitsTest`).

---

### Task 0: Větev

- [ ] **Step 1: Vytvořit větev**

```bash
git switch develop && git switch -c feature/CL_cesta
```

---

### Task 1: Doména — stupně, rytmus, postup

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/PathStage.kt`
- Create: `app/src/main/java/org/clockout/domain/model/PathRhythm.kt`
- Create: `app/src/main/java/org/clockout/domain/model/PathProgress.kt`
- Test: `app/src/test/java/org/clockout/PathStageTest.kt`
- Test: `app/src/test/java/org/clockout/PathRhythmTest.kt`

**Interfaces:**
- Produces:
  - `enum class PathStage(val id: String, val minDays: Int)` s `val next: PathStage?` a `companion fun forDays(days: Int): PathStage`
  - `object PathRhythm { data class Result(val current: Int, val longest: Int, val isCurrentWeekInRhythm: Boolean); fun evaluate(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): Result }`
  - `data class PathProgress(closedDays: Int, stage: PathStage, nextStage: PathStage?, daysToNextStage: Int?, rhythmWeeks: Int, longestRhythmWeeks: Int, isCurrentWeekInRhythm: Boolean)` s `companion val EMPTY` a `object PathPolicy { fun progress(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): PathProgress }`

- [ ] **Step 1: Napsat failing testy stupňů**

`app/src/test/java/org/clockout/PathStageTest.kt`:

```kotlin
package org.clockout

import org.clockout.domain.model.PathPolicy
import org.clockout.domain.model.PathStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PathStageTest {

    @Test
    fun `stages switch exactly at their thresholds`() {
        assertEquals(PathStage.AT_DESK, PathStage.forDays(0))
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.forDays(1))
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.forDays(4))
        assertEquals(PathStage.COAT_ON, PathStage.forDays(5))
        assertEquals(PathStage.THRESHOLD, PathStage.forDays(249))
        assertEquals(PathStage.HOME, PathStage.forDays(250))
        assertEquals(PathStage.HOME, PathStage.forDays(1000))
    }

    @Test
    fun `thresholds match the spec and rise strictly`() {
        assertEquals(
            listOf(0, 1, 5, 10, 20, 35, 50, 75, 100, 150, 250),
            PathStage.entries.map { it.minDays }
        )
    }

    @Test
    fun `the last stage has no next one`() {
        assertEquals(PathStage.LAPTOP_CLOSED, PathStage.AT_DESK.next)
        assertNull(PathStage.HOME.next)
    }

    @Test
    fun `progress counts the days left to the next stage`() {
        val monday = LocalDate.of(2026, 10, 5)
        val days = (0L..2L).map { monday.plusDays(it) }.toSet()

        val progress = PathPolicy.progress(days, includeWeekends = false, today = monday.plusDays(2))

        assertEquals(3, progress.closedDays)
        assertEquals(PathStage.LAPTOP_CLOSED, progress.stage)
        assertEquals(PathStage.COAT_ON, progress.nextStage)
        assertEquals(2, progress.daysToNextStage)
    }

    @Test
    fun `progress on the last stage has nothing left to reach`() {
        val start = LocalDate.of(2025, 1, 1)
        val days = (0L until 250L).map { start.plusDays(it) }.toSet()

        val progress = PathPolicy.progress(days, includeWeekends = true, today = start.plusDays(249))

        assertEquals(PathStage.HOME, progress.stage)
        assertNull(progress.nextStage)
        assertNull(progress.daysToNextStage)
    }
}
```

- [ ] **Step 2: Napsat failing testy rytmu**

`app/src/test/java/org/clockout/PathRhythmTest.kt`:

```kotlin
package org.clockout

import org.clockout.domain.model.PathRhythm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PathRhythmTest {

    // 2026-09-14 is a Monday; W4 is the week of TODAY.
    private val w1 = LocalDate.of(2026, 9, 14)
    private val w2 = w1.plusWeeks(1)
    private val w3 = w1.plusWeeks(2)
    private val w4 = w1.plusWeeks(3)
    private val today = w4.plusDays(2) // Wednesday

    private fun days(monday: LocalDate, vararg offsets: Int) = offsets.map { monday.plusDays(it.toLong()) }
    private fun workweek(monday: LocalDate) = days(monday, 0, 1, 2, 3, 4)

    private fun eval(
        days: Collection<LocalDate>,
        includeWeekends: Boolean = false,
        today: LocalDate = this.today
    ) = PathRhythm.evaluate(days.toSet(), includeWeekends, today)

    @Test
    fun `no days means no rhythm`() {
        assertEquals(PathRhythm.Result(0, 0, false), eval(emptyList()))
    }

    @Test
    fun `three full weeks make a rhythm of three`() {
        val result = eval(workweek(w1) + workweek(w2) + workweek(w3))
        assertEquals(3, result.current)
        assertEquals(3, result.longest)
        assertFalse(result.isCurrentWeekInRhythm)
    }

    @Test
    fun `four of five days is enough for a week`() {
        assertEquals(3, eval(workweek(w1) + days(w2, 0, 1, 2, 3) + workweek(w3)).current)
    }

    @Test
    fun `one weak week is tolerated but not counted`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(2, result.current)
        assertEquals(2, result.longest)
    }

    @Test
    fun `two weak weeks in a row end the rhythm`() {
        val result = eval(workweek(w1))
        assertEquals(0, result.current)
        assertEquals(1, result.longest)
    }

    @Test
    fun `the week in progress neither breaks nor extends the rhythm until it is met`() {
        val inProgress = eval(workweek(w3) + days(w4, 0, 1))
        assertEquals(1, inProgress.current)
        assertFalse(inProgress.isCurrentWeekInRhythm)

        val met = eval(workweek(w3) + days(w4, 0, 1, 2, 3), today = w4.plusDays(3))
        assertEquals(2, met.current)
        assertTrue(met.isCurrentWeekInRhythm)
    }

    @Test
    fun `the first week only offers the days from the first ritual on`() {
        assertEquals(1, eval(days(w3, 2, 3, 4)).current)
        assertEquals(1, eval(days(w3, 4)).current)
    }

    @Test
    fun `with weekends on a week needs six of seven days`() {
        // W3 is the first week and has only its five weekdays: below the threshold of six.
        assertEquals(0, eval(workweek(w3), includeWeekends = true).current)
        assertEquals(1, eval(days(w3, 0, 1, 2, 3, 4, 5), includeWeekends = true).current)
    }

    @Test
    fun `weekend days do not count while weekends are off`() {
        val result = eval(workweek(w2) + days(w3, 0, 1, 2, 5, 6))
        assertEquals(1, result.current)
        assertEquals(1, result.longest)
    }

    @Test
    fun `the longest rhythm survives a break`() {
        val e1 = w1.minusWeeks(4)
        val history = workweek(e1) + workweek(e1.plusWeeks(1)) + workweek(e1.plusWeeks(2)) + workweek(w2)
        val result = eval(history)
        assertEquals(1, result.current)
        assertEquals(3, result.longest)
    }
}
```

- [ ] **Step 3: Ověřit, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "*PathStageTest*" --tests "*PathRhythmTest*"`
Expected: FAIL při kompilaci — `Unresolved reference: PathStage` / `PathRhythm` / `PathPolicy`.

- [ ] **Step 4: Implementovat `PathStage`**

`app/src/main/java/org/clockout/domain/model/PathStage.kt`:

```kotlin
package org.clockout.domain.model

/**
 * The stages of the way home: the commute that working from home took away, rebuilt one closed
 * day at a time. [AT_DESK] is the state before the first closed day, so a new install has an
 * emblem of its own. Names, verses and icons
 * live in `presentation/util/Localization.kt`, the same way [MoodTag] keeps its strings out.
 */
enum class PathStage(val id: String, val minDays: Int) {
    AT_DESK("AT_DESK", 0),
    LAPTOP_CLOSED("LAPTOP_CLOSED", 1),
    COAT_ON("COAT_ON", 5),
    KEY_IN_LOCK("KEY_IN_LOCK", 10),
    DOWN_THE_STAIRS("DOWN_THE_STAIRS", 20),
    STREET_AT_DUSK("STREET_AT_DUSK", 35),
    THROUGH_THE_PARK("THROUGH_THE_PARK", 50),
    OVER_THE_BRIDGE("OVER_THE_BRIDGE", 75),
    GARDEN_GATE("GARDEN_GATE", 100),
    THRESHOLD("THRESHOLD", 150),
    HOME("HOME", 250);

    val next: PathStage?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forDays(days: Int): PathStage = entries.last { it.minDays <= days.coerceAtLeast(0) }
    }
}
```

- [ ] **Step 5: Implementovat `PathRhythm`**

`app/src/main/java/org/clockout/domain/model/PathRhythm.kt`:

```kotlin
package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Counts rhythm in weeks rather than days, so a single missed day never breaks anything.
 *
 * A week is in rhythm when all but one of its offered days were closed. Only two weak weeks in a
 * row end a rhythm - one week of illness or holiday is tolerated, it just does not count. The week
 * in progress is counted once it reaches the threshold and otherwise left out, so the rhythm
 * cannot drop on a Monday morning. The week of the very first closed day offers only the days from
 * that day on: whoever starts on a Wednesday was not absent on Monday, the app was.
 */
object PathRhythm {

    data class Result(val current: Int, val longest: Int, val isCurrentWeekInRhythm: Boolean)

    private const val WEAK_WEEKS_THAT_END_A_RHYTHM = 2

    fun evaluate(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): Result {
        if (days.isEmpty()) return Result(0, 0, false)
        val firstDay = days.min()
        val currentWeek = today.mondayOfWeek()

        val pastWeeks = generateSequence(firstDay.mondayOfWeek()) { it.plusWeeks(1) }
            .takeWhile { it.isBefore(currentWeek) }
            .mapNotNull { isWeekMet(it, days, includeWeekends, firstDay) }
            .toList()
        val isCurrentWeekInRhythm = isWeekMet(currentWeek, days, includeWeekends, firstDay) == true

        var run = 0
        var longest = 0
        var weakInARow = 0
        for (met in pastWeeks + listOfNotNull(true.takeIf { isCurrentWeekInRhythm })) {
            if (met) {
                run++
                weakInARow = 0
                longest = maxOf(longest, run)
            } else {
                weakInARow++
                if (weakInARow >= WEAK_WEEKS_THAT_END_A_RHYTHM) run = 0
            }
        }
        return Result(current = run, longest = longest, isCurrentWeekInRhythm = isCurrentWeekInRhythm)
    }

    /** Null for a week that offered no day at all, which neither counts nor breaks anything. */
    private fun isWeekMet(
        monday: LocalDate,
        days: Set<LocalDate>,
        includeWeekends: Boolean,
        firstDay: LocalDate
    ): Boolean? {
        val offered = (0L until if (includeWeekends) 7L else 5L)
            .map { monday.plusDays(it) }
            .filterNot { it.isBefore(firstDay) }
        if (offered.isEmpty()) return null
        val threshold = maxOf(1, offered.size - 1)
        return offered.count { it in days } >= threshold
    }

    private fun LocalDate.mondayOfWeek(): LocalDate =
        with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
```

- [ ] **Step 6: Implementovat `PathProgress` a `PathPolicy`**

`app/src/main/java/org/clockout/domain/model/PathProgress.kt`:

```kotlin
package org.clockout.domain.model

import java.time.LocalDate

data class PathProgress(
    val closedDays: Int,
    val stage: PathStage,
    val nextStage: PathStage?,
    /** Null on the last stage, where there is nothing left to reach. */
    val daysToNextStage: Int?,
    val rhythmWeeks: Int,
    val longestRhythmWeeks: Int,
    val isCurrentWeekInRhythm: Boolean
) {
    companion object {
        val EMPTY = PathPolicy.progress(emptySet(), includeWeekends = false, today = LocalDate.MIN)
    }
}

object PathPolicy {

    fun progress(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): PathProgress {
        val stage = PathStage.forDays(days.size)
        val next = stage.next
        val rhythm = PathRhythm.evaluate(days, includeWeekends, today)
        return PathProgress(
            closedDays = days.size,
            stage = stage,
            nextStage = next,
            daysToNextStage = next?.let { it.minDays - days.size },
            rhythmWeeks = rhythm.current,
            longestRhythmWeeks = rhythm.longest,
            isCurrentWeekInRhythm = rhythm.isCurrentWeekInRhythm
        )
    }
}
```

- [ ] **Step 7: Ověřit, že testy prochází**

Run: `./gradlew :app:testDebugUnitTest --tests "*PathStageTest*" --tests "*PathRhythmTest*"`
Expected: PASS (15 testů).

- [ ] **Step 8: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/java/org/clockout/domain/model/PathStage.kt app/src/main/java/org/clockout/domain/model/PathRhythm.kt app/src/main/java/org/clockout/domain/model/PathProgress.kt app/src/test/java/org/clockout/PathStageTest.kt app/src/test/java/org/clockout/PathRhythmTest.kt
git commit -m "feat: doménová pravidla stupňů cesty a týdenního rytmu"
```

---

### Task 2: Room — tabulka `practice_days` a migrace 1 → 2

**Files:**
- Create: `app/src/main/java/org/clockout/data/local/db/PracticeDayEntity.kt`
- Create: `app/src/main/java/org/clockout/data/local/db/PracticeDayDao.kt`
- Modify: `app/src/main/java/org/clockout/data/local/db/ClockoutDatabase.kt`
- Modify: `gradle/libs.versions.toml` (sekce `[libraries]`, za `androidx-room-compiler`)
- Modify: `app/build.gradle.kts` (`sourceSets` za řádkem `sourceSets["androidTest"]…`, `testImplementation` blok)
- Create (generuje KSP): `app/schemas/org.clockout.data.local.db.ClockoutDatabase/2.json`
- Test: `app/src/test/java/org/clockout/ClockoutDatabaseMigrationTest.kt`

**Interfaces:**
- Produces:
  - `@Entity(tableName = "practice_days") data class PracticeDayEntity(@PrimaryKey val date: String)`
  - `interface PracticeDayDao { fun observeAll(): Flow<List<String>>; suspend fun insert(day: PracticeDayEntity); suspend fun insertAll(days: List<PracticeDayEntity>) }`
  - `ClockoutDatabase.practiceDayDao(): PracticeDayDao`, `ClockoutDatabase.MIGRATION_1_2: Migration`

- [ ] **Step 1: Přidat `room-testing`**

`gradle/libs.versions.toml`, za `androidx-room-compiler`:

```toml
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomRuntime" }
```

`app/build.gradle.kts`, do `testImplementation` bloku (abecedně za `libs.androidx.junit`):

```kotlin
  testImplementation(libs.androidx.room.testing)
```

a za `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")`:

```kotlin
  // MigrationTestHelper reads the exported schemas from assets, and the migration test runs on
  // Robolectric, not on a device.
  sourceSets["test"].assets.srcDir("$projectDir/schemas")
```

- [ ] **Step 2: Napsat failing test migrace**

`app/src/test/java/org/clockout/ClockoutDatabaseMigrationTest.kt`:

```kotlin
package org.clockout

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.clockout.data.local.db.ClockoutDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClockoutDatabaseMigrationTest {

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClockoutDatabase::class.java
    )

    @Test
    fun `migration 1 to 2 seeds practice days from the ritual history`() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                "INSERT INTO ritual_logs(date, question_text, selected_answer, mood_tag, timestamp) " +
                    "VALUES ('2026-10-05', 'Q', NULL, 'CALM', 1), ('2026-10-06', 'Q', 'A', 'TENSE', 2)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 2, true, ClockoutDatabase.MIGRATION_1_2)

        val dates = migrated.query("SELECT date FROM practice_days ORDER BY date").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("2026-10-05", "2026-10-06"), dates)
        val logCount = migrated.query("SELECT COUNT(*) FROM ritual_logs").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, logCount)
    }
}
```

- [ ] **Step 3: Ověřit, že test padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*ClockoutDatabaseMigrationTest*"`
Expected: FAIL při kompilaci — `Unresolved reference: MIGRATION_1_2`.

- [ ] **Step 4: Entita a DAO**

`app/src/main/java/org/clockout/data/local/db/PracticeDayEntity.kt`:

```kotlin
package org.clockout.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day the workday was closed, kept apart from [RitualLogEntity] so the path survives
 * "Clear data and history". It deliberately holds the date alone: clearing the history has to
 * remove everything personal, and the mood, question and answer all are.
 */
@Entity(tableName = "practice_days")
data class PracticeDayEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String // format "YYYY-MM-DD", the same as ritual_logs
)
```

`app/src/main/java/org/clockout/data/local/db/PracticeDayDao.kt`:

```kotlin
package org.clockout.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDayDao {

    @Query("SELECT date FROM practice_days")
    fun observeAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: PracticeDayEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<PracticeDayEntity>)
}
```

- [ ] **Step 5: Databáze v2 s migrací**

V `ClockoutDatabase.kt`: `entities = [RitualLogEntity::class, PracticeDayEntity::class]`, `version = 2`, nová abstraktní metoda a migrace, builder dostane `.addMigrations(MIGRATION_1_2)`:

```kotlin
    abstract fun practiceDayDao(): PracticeDayDao

    companion object {
        const val DATABASE_NAME = "clockout.db"

        /** Seeds the path from the history that already exists, so an update keeps its stage. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `practice_days` (`date` TEXT NOT NULL, PRIMARY KEY(`date`))")
                db.execSQL("INSERT OR IGNORE INTO practice_days(date) SELECT date FROM ritual_logs")
            }
        }
        // … instance / getInstance beze změny …
```

```kotlin
            Room.databaseBuilder(context, ClockoutDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                // TRUNCATE keeps … (komentář beze změny)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
```

Importy: `androidx.room.migration.Migration`, `androidx.sqlite.db.SupportSQLiteDatabase`.

- [ ] **Step 6: Zkompilovat a ověřit vygenerované schéma**

Run: `./gradlew :app:compileDebugKotlin && ls app/schemas/org.clockout.data.local.db.ClockoutDatabase/`
Expected: `1.json 2.json`. Ověř, že `2.json` obsahuje tabulku `practice_days` se sloupcem `date` (`TEXT`, `notNull: true`) — SQL v migraci musí odpovídat `createSql` ze schématu, jinak validace v testu selže.

- [ ] **Step 7: Ověřit, že test prochází**

Run: `./gradlew :app:testDebugUnitTest --tests "*ClockoutDatabaseMigrationTest*"`
Expected: PASS. Pokud selže s „Cannot find the schema file in the assets folder", nahraď v kroku 1 `sourceSets["test"]` za `sourceSets["debug"]` (Robolectric čte assety sloučené pro variantu) a spusť znovu.

- [ ] **Step 8: Commit (po souhlasu uživatele)**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/schemas app/src/main/java/org/clockout/data/local/db app/src/test/java/org/clockout/ClockoutDatabaseMigrationTest.kt
git commit -m "feat: tabulka practice_days a migrace databáze na verzi 2"
```

---

### Task 3: Repozitář — zápis, přežití smazání, `getPathProgress()`

**Files:**
- Modify: `app/src/main/java/org/clockout/data/local/RitualLogMigrator.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepository.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt:30-41,130-141`
- Modify: `app/src/main/java/org/clockout/presentation/RitualDependencies.kt`
- Modify (konstruktor): všech 11 testů volajících `RitualRepositoryImpl(` a `RitualLogMigratorTest.kt`
- Test: `app/src/test/java/org/clockout/PracticeDayPersistenceTest.kt`, `app/src/test/java/org/clockout/RitualLogMigratorTest.kt`

**Interfaces:**
- Consumes: `PracticeDayDao`, `PracticeDayEntity`, `ClockoutDatabase.practiceDayDao()` (Task 2); `PathPolicy.progress`, `PathProgress` (Task 1)
- Produces:
  - `RitualRepository.getPathProgress(): Flow<PathProgress>`
  - `RitualRepositoryImpl(preferences, ritualLogDao, practiceDayDao: PracticeDayDao, logMigrator = RitualLogMigrator(preferences, ritualLogDao, practiceDayDao), soundFileExists)`
  - `RitualLogMigrator(legacySource, dao, practiceDayDao: PracticeDayDao)`

- [ ] **Step 1: Napsat failing testy**

`app/src/test/java/org/clockout/PracticeDayPersistenceTest.kt`:

```kotlin
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

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
}
```

Do `RitualLogMigratorTest.kt` přidat test (a všechna volání `RitualLogMigrator(legacy, dao)` změnit na `RitualLogMigrator(legacy, dao, database.practiceDayDao())`):

```kotlin
    @Test
    fun `legacy logs also become practice days`() = runTest {
        val legacy = FakeLegacySource(listOf(legacyEntry("2025-03-03"), legacyEntry("2025-03-04")))

        RitualLogMigrator(legacy, dao, database.practiceDayDao()).ensureMigrated()

        assertEquals(
            setOf("2025-03-03", "2025-03-04"),
            database.practiceDayDao().observeAll().first().toSet()
        )
    }
```

(Import `kotlinx.coroutines.flow.first`, pokud chybí.)

- [ ] **Step 2: Ověřit, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "*PracticeDayPersistenceTest*" --tests "*RitualLogMigratorTest*"`
Expected: FAIL při kompilaci — `No parameter with name 'practiceDayDao'`, `Unresolved reference: getPathProgress`.

- [ ] **Step 3: Migrátor plní i `practice_days`**

V `RitualLogMigrator.kt`: konstruktor `(legacySource: LegacyRitualLogSource, dao: RitualLogDao, practiceDayDao: PracticeDayDao)` a v bloku importu:

```kotlin
                if (legacyLogs.isNotEmpty()) {
                    dao.upsertAll(legacyLogs.map { it.toEntity() })
                    // The Room migration that seeds the path ran before this import, so the days
                    // that only now reach ritual_logs have to be written to the path here.
                    practiceDayDao.insertAll(legacyLogs.map { PracticeDayEntity(it.date) })
                }
```

- [ ] **Step 4: Repozitář**

`RitualRepository.kt` — za `getStreakInfo()`:

```kotlin
    /** The path's stage and weekly rhythm; survives [clearLogs], which only empties the history. */
    fun getPathProgress(): Flow<PathProgress>
```

`RitualRepositoryImpl.kt` — konstruktor:

```kotlin
class RitualRepositoryImpl(
    private val preferences: RitualPreferences,
    private val ritualLogDao: RitualLogDao,
    private val practiceDayDao: PracticeDayDao,
    private val logMigrator: RitualLogMigrator = RitualLogMigrator(preferences, ritualLogDao, practiceDayDao),
    /** … stávající KDoc soundFileExists … */
    private val soundFileExists: (fileName: String) -> Boolean = { true }
) : RitualRepository {
```

Za `getStreakInfo()`:

```kotlin
    override fun getPathProgress(): Flow<PathProgress> = flow {
        logMigrator.ensureMigrated()
        emitAll(
            practiceDayDao.observeAll().combine(preferences.includeWeekendsFlow) { dates, includeWeekends ->
                PathPolicy.progress(
                    days = dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
                    includeWeekends = includeWeekends,
                    today = LocalDate.now()
                )
            }
        )
    }
```

V `logRitual` za `ritualLogDao.upsert(entry.toEntity())`:

```kotlin
        practiceDayDao.insert(PracticeDayEntity(entry.date))
```

`clearLogs()` se nemění. Importy: `org.clockout.data.local.db.PracticeDayDao`, `org.clockout.data.local.db.PracticeDayEntity`, `org.clockout.domain.model.PathPolicy`, `org.clockout.domain.model.PathProgress`, `java.time.LocalDate`.

- [ ] **Step 5: Wiring a stávající testy**

`RitualDependencies.kt`:

```kotlin
    return remember(context) {
        val database = ClockoutDatabase.getInstance(context)
        RitualRepositoryImpl(
            preferences = preferences,
            ritualLogDao = database.ritualLogDao(),
            practiceDayDao = database.practiceDayDao(),
            // … soundFileExists beze změny …
        )
    }
```

Ve všech testech, které staví `RitualRepositoryImpl`, doplnit argument (všechny mají v dosahu `database`):

```bash
cd app/src/test/java/org/clockout
perl -0pi -e 's/^(\s*)(ritualLogDao = database\.ritualLogDao\(\))(,?)$/$1$2,\n$1practiceDayDao = database.practiceDayDao()$3/mg' \
  ReviewPromptFlowTest.kt CustomSoundReplacementTest.kt CustomSoundAvailabilityTest.kt RitualSessionSurvivalTest.kt \
  RitualWeekOverviewTest.kt TelemetryConsentPersistenceTest.kt telemetry/SettingsTelemetryTest.kt \
  RitualReviewRequestTest.kt RitualRepositoryLogPersistenceTest.kt telemetry/RitualTelemetryTest.kt
```

`CustomReminderTimeTest.kt:49` má volání na jednom řádku — uprav ručně na
`RitualRepositoryImpl(preferences = preferences, ritualLogDao = database.ritualLogDao(), practiceDayDao = database.practiceDayDao())`.

Kontrola: `grep -rn "RitualRepositoryImpl(" app/src/test -A4 | grep -c practiceDayDao` → `11`.

- [ ] **Step 6: Ověřit, že prochází nové i všechny stávající testy**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS celé sady (screenshoty se tu neporovnávají). O víkendu viz CLAUDE.md k `TabletNavigationTest`/`PhoneNavigationTest` — spusť je případně samostatně.

- [ ] **Step 7: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/java/org/clockout/data app/src/main/java/org/clockout/presentation/RitualDependencies.kt app/src/test/java/org/clockout
git commit -m "feat: repozitář zapisuje dny cesty a počítá postup, smazání historie je zachová"
```

---

### Task 4: Zdroje — texty, znaky stupňů, mapování

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Create: `app/src/main/res/drawable/ic_path_stage_0.xml` … `ic_path_stage_10.xml` (11 souborů)
- Modify: `app/src/main/java/org/clockout/presentation/util/Localization.kt`
- Create: `app/src/main/java/org/clockout/presentation/components/PathStageEmblem.kt`
- Test: `app/src/test/java/org/clockout/DomainLocalizationTest.kt`

**Interfaces:**
- Consumes: `PathStage` (Task 1)
- Produces:
  - `val PathStage.titleRes: Int`, `val PathStage.verseRes: Int`, `val PathStage.descriptionRes: Int` (`@get:StringRes`), `val PathStage.iconRes: Int` (`@get:DrawableRes`)
  - `@Composable fun PathStageEmblem(stage: PathStage, modifier: Modifier = Modifier, size: Dp = 48.dp, isDimmed: Boolean = false)`
  - string/plural klíče vyjmenované v kroku 3

- [ ] **Step 1: Napsat failing test**

Do `DomainLocalizationTest.kt`:

```kotlin
    @Test
    fun `a path stage is named in the app language`() {
        show(AppLanguage.CZECH) { Text(stringResource(PathStage.LAPTOP_CLOSED.titleRes)) }

        composeTestRule.onNodeWithText("Zaklapnutý notebook").assertIsDisplayed()
    }

    @Test
    fun `every path stage has its own title, verse, description and icon`() {
        val stages = PathStage.entries
        assertEquals(stages.size, stages.map { it.titleRes }.toSet().size)
        assertEquals(stages.size, stages.map { it.verseRes }.toSet().size)
        assertEquals(stages.size, stages.map { it.descriptionRes }.toSet().size)
        assertEquals(stages.size, stages.map { it.iconRes }.toSet().size)
    }
```

(Importy `org.clockout.domain.model.PathStage`, `org.clockout.presentation.util.titleRes` atd., `org.junit.Assert.assertEquals`.)

- [ ] **Step 2: Ověřit, že test padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*DomainLocalizationTest*"`
Expected: FAIL při kompilaci — `Unresolved reference: titleRes`.

- [ ] **Step 3: Texty**

`values/strings.xml` (před `</resources>`):

```xml
    <!-- Path: the way home -->
    <string name="path_stage_0_title">Still at the Desk</string>
    <string name="path_stage_0_verse">The screen is still on, and the day has not ended.</string>
    <string name="path_stage_0_desc">No day closed yet. The way home begins the first time you close up work.</string>
    <string name="path_stage_1_title">Laptop Closed</string>
    <string name="path_stage_1_verse">The click of the lid is the first step.</string>
    <string name="path_stage_1_desc">Your first closed day. Work got a clear ending, even though you are still in the same room.</string>
    <string name="path_stage_2_title">Coat On</string>
    <string name="path_stage_2_verse">Not leaving yet, but getting ready.</string>
    <string name="path_stage_2_desc">Five closed days. Your body is starting to remember that the ritual means it is time to leave.</string>
    <string name="path_stage_3_title">Key in the Lock</string>
    <string name="path_stage_3_verse">Behind the door stays everything that can wait until tomorrow.</string>
    <string name="path_stage_3_desc">Ten days. You are learning to lock work away where it belongs.</string>
    <string name="path_stage_4_title">Down the Stairs</string>
    <string name="path_stage_4_verse">Every step down leaves your head a little lighter.</string>
    <string name="path_stage_4_desc">Twenty days. Thoughts of work still call out, but they fade with every step.</string>
    <string name="path_stage_5_title">The Street at Dusk</string>
    <string name="path_stage_5_verse">The street lamps come on, and nobody needs anything from you.</string>
    <string name="path_stage_5_desc">Thirty-five days. Closing the day stops being an effort and becomes a habit.</string>
    <string name="path_stage_6_title">Through the Park</string>
    <string name="path_stage_6_verse">You slow down. The way no longer hurries anywhere.</string>
    <string name="path_stage_6_desc">Fifty days. Between work and home you have a stretch of calm of your own.</string>
    <string name="path_stage_7_title">Over the Bridge</string>
    <string name="path_stage_7_verse">The river flows under the bridge, and the workday stays on the far bank.</string>
    <string name="path_stage_7_desc">Seventy-five days. You now cross the line between work and your own time without thinking about it.</string>
    <string name="path_stage_8_title">The Garden Gate</string>
    <string name="path_stage_8_verse">The creak of the gate sounds like home.</string>
    <string name="path_stage_8_desc">One hundred days. Your evenings belong to you and the people around you again.</string>
    <string name="path_stage_9_title">The Threshold</string>
    <string name="path_stage_9_verse">Your shoes stay outside, and so does the whole day.</string>
    <string name="path_stage_9_desc">One hundred and fifty days. You know that moment of crossing so well that nothing can disturb it.</string>
    <string name="path_stage_10_title">Home</string>
    <string name="path_stage_10_verse">There is a light in the window, and calm inside.</string>
    <string name="path_stage_10_desc">Two hundred and fifty days. You carry the way home inside you, wherever you work from.</string>

    <string name="path_card_title">The Path</string>
    <string name="path_card_beginning">The path begins with your first closed day</string>
    <string name="path_card_final">You\'re home. You know the way by heart now.</string>
    <string name="path_next_stage">Next: %s</string>
    <plurals name="path_days_to_next">
        <item quantity="one">%d more day to the next stage</item>
        <item quantity="other">%d more days to the next stage</item>
    </plurals>
    <plurals name="path_rhythm_current">
        <item quantity="one">Rhythm: %d week</item>
        <item quantity="other">Rhythm: %d weeks</item>
    </plurals>
    <string name="path_rhythm_longest">longest %d</string>
    <plurals name="path_closed_days">
        <item quantity="one">%d closed day</item>
        <item quantity="other">%d closed days</item>
    </plurals>
    <plurals name="path_stage_threshold">
        <item quantity="one">from %d day</item>
        <item quantity="other">from %d days</item>
    </plurals>
    <string name="path_open_description">Open the path</string>
    <string name="path_screen_title">The Way Home</string>
    <string name="path_current_badge">Your stage</string>
    <string name="path_origin_title">What the path is about</string>
    <string name="path_origin_text">The way home from work used to close the day by itself: shut the laptop, put on a coat, lock up, take the stairs and walk home. Working from home, that way is missing, and work easily spills into the evening.\n\nEvery day you close moves you a little further along a way you rebuild one day at a time. At the end of it you are home, which is where the evening belongs.</string>
    <string name="path_stages_title">Ten stages</string>
    <string name="path_rhythm_title">How the rhythm works</string>
    <string name="path_rhythm_text">A week is in rhythm when you close all but one of the days the ritual is offered on: four of five, or six of seven with weekends on. One weaker week will not break the rhythm, it just does not count; only two in a row do. The week you are in counts once it reaches the threshold.\n\nYour stage and your rhythm stay even when you clear the history. They remember only the dates you closed the day, nothing of what you wrote.</string>
    <string name="settings_about_path_button">The Way Home</string>
    <string name="ritual_path_stage_reached">A new stage on the path</string>
    <string name="ritual_path_rhythm_week">You\'re in rhythm this week</string>
```

`values-cs/strings.xml`:

```xml
    <!-- Cesta domů -->
    <string name="path_stage_0_title">Ještě u stolu</string>
    <string name="path_stage_0_verse">Monitor svítí a den ještě neskončil.</string>
    <string name="path_stage_0_desc">Zatím není uzavřený žádný den. Cesta domů začne, až poprvé zaklapneš práci.</string>
    <string name="path_stage_1_title">Zaklapnutý notebook</string>
    <string name="path_stage_1_verse">Cvaknutí víka je první krok.</string>
    <string name="path_stage_1_desc">První uzavřený den. Práce dostala jasný konec, i když jsi pořád ve stejném pokoji.</string>
    <string name="path_stage_2_title">Kabát na ramenou</string>
    <string name="path_stage_2_verse">Ještě nejdeš, ale už se chystáš.</string>
    <string name="path_stage_2_desc">Pět uzavřených dní. Tělo si začíná pamatovat, že po rituálu přichází odchod.</string>
    <string name="path_stage_3_title">Klíč v zámku</string>
    <string name="path_stage_3_verse">Za dveřmi zůstává všechno, co počká do zítřka.</string>
    <string name="path_stage_3_desc">Deset dní. Učíš se zamknout práci tam, kam patří.</string>
    <string name="path_stage_4_title">Schody dolů</string>
    <string name="path_stage_4_verse">S každým schodem je hlava o kousek lehčí.</string>
    <string name="path_stage_4_desc">Dvacet dní. Myšlenky na práci se ještě ozvou, ale s každým krokem slábnou.</string>
    <string name="path_stage_5_title">Ulice za soumraku</string>
    <string name="path_stage_5_verse">Rozsvěcují se lampy a nikdo po tobě nic nechce.</string>
    <string name="path_stage_5_desc">Třicet pět dní. Uzavírání dne přestává být úsilím a stává se zvykem.</string>
    <string name="path_stage_6_title">Parkem</string>
    <string name="path_stage_6_verse">Zpomalíš. Cesta už nikam nespěchá.</string>
    <string name="path_stage_6_desc">Padesát dní. Mezi prací a domovem máš vlastní kus klidu.</string>
    <string name="path_stage_7_title">Přes most</string>
    <string name="path_stage_7_verse">Pod mostem teče řeka a pracovní den zůstal na druhém břehu.</string>
    <string name="path_stage_7_desc">Sedmdesát pět dní. Hranici mezi prací a vlastním časem už překračuješ bez přemýšlení.</string>
    <string name="path_stage_8_title">Branka</string>
    <string name="path_stage_8_verse">Vrznutí branky zní jako domov.</string>
    <string name="path_stage_8_desc">Sto dní. Večery zase patří tobě a lidem kolem tebe.</string>
    <string name="path_stage_9_title">Práh</string>
    <string name="path_stage_9_verse">Boty zůstanou venku a s nimi i celý den.</string>
    <string name="path_stage_9_desc">Sto padesát dní. Tu chvíli přechodu znáš tak dobře, že ji nic nenaruší.</string>
    <string name="path_stage_10_title">Doma</string>
    <string name="path_stage_10_verse">V okně svítí a uvnitř je klid.</string>
    <string name="path_stage_10_desc">Dvě stě padesát dní. Cestu domů máš v sobě, ať pracuješ odkudkoli.</string>

    <string name="path_card_title">Cesta</string>
    <string name="path_card_beginning">Cesta začíná prvním uzavřeným dnem</string>
    <string name="path_card_final">Jsi doma. Cestu teď znáš nazpaměť.</string>
    <string name="path_next_stage">Další: %s</string>
    <plurals name="path_days_to_next">
        <item quantity="one">Ještě %d den do dalšího stupně</item>
        <item quantity="few">Ještě %d dny do dalšího stupně</item>
        <item quantity="many">Ještě %d dne do dalšího stupně</item>
        <item quantity="other">Ještě %d dní do dalšího stupně</item>
    </plurals>
    <plurals name="path_rhythm_current">
        <item quantity="one">Rytmus: %d týden</item>
        <item quantity="few">Rytmus: %d týdny</item>
        <item quantity="many">Rytmus: %d týdne</item>
        <item quantity="other">Rytmus: %d týdnů</item>
    </plurals>
    <string name="path_rhythm_longest">nejdelší %d</string>
    <plurals name="path_closed_days">
        <item quantity="one">%d uzavřený den</item>
        <item quantity="few">%d uzavřené dny</item>
        <item quantity="many">%d uzavřeného dne</item>
        <item quantity="other">%d uzavřených dní</item>
    </plurals>
    <plurals name="path_stage_threshold">
        <item quantity="one">od %d dne</item>
        <item quantity="few">od %d dní</item>
        <item quantity="many">od %d dne</item>
        <item quantity="other">od %d dní</item>
    </plurals>
    <string name="path_open_description">Otevřít cestu</string>
    <string name="path_screen_title">Cesta domů</string>
    <string name="path_current_badge">Tvůj stupeň</string>
    <string name="path_origin_title">O čem je cesta</string>
    <string name="path_origin_text">Dřív den uzavírala cesta z práce sama: zaklapnout počítač, obléct kabát, zamknout, sejít schody a jít domů. Při práci z domova tahle cesta chybí a práce pak snadno přeteče do večera.\n\nKaždý uzavřený den tě posune o kus dál po cestě, kterou si tak den po dni stavíš znovu. Na konci jsi doma, a právě tam má večer patřit.</string>
    <string name="path_stages_title">Deset stupňů</string>
    <string name="path_rhythm_title">Jak funguje rytmus</string>
    <string name="path_rhythm_text">Týden je v rytmu, když uzavřeš všechny dny, kdy se rituál nabízí, až na jeden: čtyři z pěti, se zapnutými víkendy šest ze sedmi. Jeden slabší týden rytmus nepřeruší, jen se nezapočítá; přeruší ho až dva po sobě. Aktuální týden se započítá, jakmile dosáhne prahu.\n\nStupeň i rytmus zůstanou, i když smažeš historii. Pamatují si jen data uzavřených dní, nic z toho, co jsi napsal(a).</string>
    <string name="settings_about_path_button">Cesta domů</string>
    <string name="ritual_path_stage_reached">Nový stupeň cesty</string>
    <string name="ritual_path_rhythm_week">Tento týden jsi v rytmu</string>
```

- [ ] **Step 4: Znaky stupňů (11 vektorů)**

Každý soubor má tuto kostru; liší se jen `<path>` prvky uvedené níže. Bílá barva se přetónuje přes `Icon(tint = …)`.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- paths -->
</vector>
```

Tah (`S`) = `android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round"`; výplň (`F`) = `android:fillColor="#FFFFFFFF"`.

| Soubor | Motiv | `<path>` |
|---|---|---|
| `ic_path_stage_0.xml` | stůl s monitorem | S `M3,10 L21,10 M5,10 L5,20 M19,10 L19,20`; S `M9,4 L15,4 L15,8 L9,8 Z M12,8 L12,10` |
| `ic_path_stage_1.xml` | zavřený notebook | S `M4,13 L20,13 L21,16 L3,16 Z`; S `M10,14.5 L14,14.5` |
| `ic_path_stage_2.xml` | kabát na ramínku | S `M10.5,5 a1.5,1.5 0 1,1 1.5,1.5 L12,8 M12,8 L4,13 L20,13 Z`; S `M6,13 L6,20 L18,20 L18,13 M12,13 L12,20` |
| `ic_path_stage_3.xml` | klíč | S `M4.5,12 a3.5,3.5 0 1,0 7,0 a3.5,3.5 0 1,0 -7,0 Z`; S `M11.5,12 L21,12 M18,12 L18,15 M21,12 L21,14` |
| `ic_path_stage_4.xml` | schody | S `M3,20 L8,20 L8,15 L13,15 L13,10 L18,10 L18,5 L21,5` |
| `ic_path_stage_5.xml` | pouliční lampa | S `M9,21 L15,21 M12,21 L12,8 M12,8 C12,5 16,4 18,6`; F `M16,6 L20,6 L19,8.5 L17,8.5 Z` |
| `ic_path_stage_6.xml` | strom | S `M12,21 L12,13 M6,21 L18,21`; S `M12,3 C16,3 18,7 17,10 C16,13 8,13 7,10 C6,7 8,3 12,3 Z` |
| `ic_path_stage_7.xml` | most nad řekou | S `M2,14 L22,14 M2,14 C6,8 18,8 22,14 M7,10.5 L7,14 M12,9.5 L12,14 M17,10.5 L17,14`; S `M3,18 C6,16.5 8,19.5 11,18 C14,16.5 16,19.5 21,18` |
| `ic_path_stage_8.xml` | branka | S `M4,20 L4,7 M20,20 L20,7 M4,10 L20,10 M4,17 L20,17 M8,10 L8,17 M12,10 L12,17 M16,10 L16,17` |
| `ic_path_stage_9.xml` | dveře na prahu | S `M6,21 L6,3 L18,3 L18,21 M3,21 L21,21`; F `M14,12 a1,1 0 1,0 2,0 a1,1 0 1,0 -2,0 Z` |
| `ic_path_stage_10.xml` | dům s rozsvíceným oknem | S `M3,11 L12,4 L21,11 M5,9.5 L5,20 L19,20 L19,9.5`; F `M9,12 L15,12 L15,17 L9,17 Z` |

Po vytvoření zkontroluj v Android Studiu náhled každého vektoru (Resource Manager); nesmí být prázdný ani oříznutý.

- [ ] **Step 5: Mapování v `Localization.kt`**

Za `BreathingStyle.descriptionRes`:

```kotlin
@get:StringRes
val PathStage.titleRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_title
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_title
        PathStage.COAT_ON -> R.string.path_stage_2_title
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_title
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_title
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_title
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_title
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_title
        PathStage.GARDEN_GATE -> R.string.path_stage_8_title
        PathStage.THRESHOLD -> R.string.path_stage_9_title
        PathStage.HOME -> R.string.path_stage_10_title
    }

@get:StringRes
val PathStage.verseRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_verse
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_verse
        PathStage.COAT_ON -> R.string.path_stage_2_verse
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_verse
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_verse
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_verse
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_verse
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_verse
        PathStage.GARDEN_GATE -> R.string.path_stage_8_verse
        PathStage.THRESHOLD -> R.string.path_stage_9_verse
        PathStage.HOME -> R.string.path_stage_10_verse
    }

@get:StringRes
val PathStage.descriptionRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.string.path_stage_0_desc
        PathStage.LAPTOP_CLOSED -> R.string.path_stage_1_desc
        PathStage.COAT_ON -> R.string.path_stage_2_desc
        PathStage.KEY_IN_LOCK -> R.string.path_stage_3_desc
        PathStage.DOWN_THE_STAIRS -> R.string.path_stage_4_desc
        PathStage.STREET_AT_DUSK -> R.string.path_stage_5_desc
        PathStage.THROUGH_THE_PARK -> R.string.path_stage_6_desc
        PathStage.OVER_THE_BRIDGE -> R.string.path_stage_7_desc
        PathStage.GARDEN_GATE -> R.string.path_stage_8_desc
        PathStage.THRESHOLD -> R.string.path_stage_9_desc
        PathStage.HOME -> R.string.path_stage_10_desc
    }

@get:DrawableRes
val PathStage.iconRes: Int
    get() = when (this) {
        PathStage.AT_DESK -> R.drawable.ic_path_stage_0
        PathStage.LAPTOP_CLOSED -> R.drawable.ic_path_stage_1
        PathStage.COAT_ON -> R.drawable.ic_path_stage_2
        PathStage.KEY_IN_LOCK -> R.drawable.ic_path_stage_3
        PathStage.DOWN_THE_STAIRS -> R.drawable.ic_path_stage_4
        PathStage.STREET_AT_DUSK -> R.drawable.ic_path_stage_5
        PathStage.THROUGH_THE_PARK -> R.drawable.ic_path_stage_6
        PathStage.OVER_THE_BRIDGE -> R.drawable.ic_path_stage_7
        PathStage.GARDEN_GATE -> R.drawable.ic_path_stage_8
        PathStage.THRESHOLD -> R.drawable.ic_path_stage_9
        PathStage.HOME -> R.drawable.ic_path_stage_10
    }
```

Importy: `androidx.annotation.DrawableRes`, `org.clockout.domain.model.PathStage`.

- [ ] **Step 6: Komponenta znaku**

`app/src/main/java/org/clockout/presentation/components/PathStageEmblem.kt`:

```kotlin
package org.clockout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.clockout.domain.model.PathStage
import org.clockout.presentation.util.iconRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextMuted

/** A stage's emblem in the indigo disc the home screen uses for its own icon. Decorative. */
@Composable
fun PathStageEmblem(
    stage: PathStage,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isDimmed: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isDimmed) Color.White.copy(alpha = 0.04f) else Color(0x336366F1))
            .border(1.dp, if (isDimmed) Color.White.copy(alpha = 0.08f) else Color(0x666366F1), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(stage.iconRes),
            contentDescription = null,
            tint = if (isDimmed) TextMuted else IndigoAccent,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
```

- [ ] **Step 7: Ověřit test a lint**

Run: `./gradlew :app:testDebugUnitTest --tests "*DomainLocalizationTest*" && ./gradlew :app:lintDebug`
Expected: PASS a lint bez chyb. Lint může varovat `UnusedResources` — texty se použijí v Úkolech 5–8; varování nejsou chyby. Pokud by lint hlásil chybu u `path_rhythm_longest` (`PluralsCandidate`), převeď ho na `<plurals>` se všemi čtyřmi českými tvary.

- [ ] **Step 8: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/res app/src/main/java/org/clockout/presentation/util/Localization.kt app/src/main/java/org/clockout/presentation/components/PathStageEmblem.kt app/src/test/java/org/clockout/DomainLocalizationTest.kt
git commit -m "feat: texty a znaky stupňů cesty v obou jazycích"
```

---

### Task 5: Home — karta „Cesta"

**Files:**
- Create: `app/src/main/java/org/clockout/presentation/home/PathCard.kt`
- Modify: `app/src/main/java/org/clockout/presentation/home/HomeState.kt`
- Modify: `app/src/main/java/org/clockout/presentation/home/HomeViewModel.kt`
- Modify: `app/src/main/java/org/clockout/presentation/home/HomeScreen.kt:72-85,193-227`
- Test: `app/src/test/java/org/clockout/HomePathCardTest.kt`, `app/src/test/java/org/clockout/PathCardTitleFitsTest.kt`

**Interfaces:**
- Consumes: `PathProgress`, `PathStage` (Task 1); `RitualRepository.getPathProgress()` (Task 3); `titleRes`, `PathStageEmblem`, string klíče `path_card_*`, `path_days_to_next`, `path_next_stage`, `path_rhythm_current`, `path_rhythm_longest`, `path_open_description` (Task 4)
- Produces:
  - `HomeState.pathProgress: PathProgress? = null`
  - `HomeScreen(…, onOpenPath: () -> Unit = {}, …)`
  - test tagy `path_card`, `path_card_stage_title`

- [ ] **Step 1: Napsat failing testy**

`app/src/test/java/org/clockout/HomePathCardTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomePathCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    private fun show(progress: PathProgress, onOpenPath: () -> Unit = {}) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(todayQuestion = question, pathProgress = progress),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {},
                        onOpenPath = onOpenPath
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("path_card").performScrollTo()
    }

    @Test
    fun `a newcomer is told where the path begins`() {
        show(PathProgress.EMPTY)

        composeTestRule.onNodeWithText("Ještě u stolu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cesta začíná prvním uzavřeným dnem").assertIsDisplayed()
    }

    @Test
    fun `a stage in progress shows the days left and the rhythm`() {
        show(
            PathProgress(
                closedDays = 12, stage = PathStage.KEY_IN_LOCK, nextStage = PathStage.DOWN_THE_STAIRS,
                daysToNextStage = 8, rhythmWeeks = 2, longestRhythmWeeks = 5, isCurrentWeekInRhythm = false
            )
        )

        composeTestRule.onNodeWithText("Klíč v zámku").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ještě 8 dní do dalšího stupně").assertIsDisplayed()
        composeTestRule.onNodeWithText("Další: Schody dolů").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rytmus: 2 týdny · nejdelší 5").assertIsDisplayed()
    }

    @Test
    fun `the last stage says the path goes on`() {
        show(
            PathProgress(
                closedDays = 300, stage = PathStage.HOME, nextStage = null,
                daysToNextStage = null, rhythmWeeks = 0, longestRhythmWeeks = 12, isCurrentWeekInRhythm = false
            )
        )

        composeTestRule.onNodeWithText("Jsi doma. Cestu teď znáš nazpaměť.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nejdelší 12").assertIsDisplayed()
    }

    @Test
    fun `tapping the card opens the path`() {
        var opened = false
        show(PathProgress.EMPTY, onOpenPath = { opened = true })

        composeTestRule.onNodeWithTag("path_card").performClick()

        assertTrue(opened)
    }
}
```

`app/src/test/java/org/clockout/PathCardTitleFitsTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Without NATIVE graphics Robolectric lays no text out, and any width assertion passes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PathCardTitleFitsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `the longest Czech stage name fits in two lines`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            todayQuestion = RitualQuestion(
                                id = "q", textEn = "Q", textCs = "Q",
                                suggestedAnswersEn = emptyList(), suggestedAnswersCs = emptyList()
                            ),
                            pathProgress = PathProgress(1, PathStage.LAPTOP_CLOSED, PathStage.COAT_ON, 4, 0, 0, false)
                        ),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {}
                    )
                }
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("path_card_stage_title", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertTrue("lineCount = ${layouts.single().lineCount}", layouts.single().lineCount <= 2)
    }
}
```

- [ ] **Step 2: Ověřit, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "*HomePathCardTest*" --tests "*PathCardTitleFitsTest*"`
Expected: FAIL při kompilaci — `No parameter with name 'pathProgress'` / `'onOpenPath'`.

- [ ] **Step 3: Stav a ViewModel**

`HomeState.kt` — nové pole za `streakInfo`:

```kotlin
    val pathProgress: PathProgress? = null,
```

`HomeViewModel.kt` — v `observeData()` druhý, samostatný `launch` (combine už má pět toků):

```kotlin
        viewModelScope.launch {
            ritualRepository.getPathProgress().collect { progress ->
                _state.update { it.copy(pathProgress = progress) }
            }
        }
```

- [ ] **Step 4: Karta**

`app/src/main/java/org/clockout/presentation/home/PathCard.kt`:

```kotlin
package org.clockout.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.PathProgress
import org.clockout.presentation.components.PathStageEmblem
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.util.titleRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@Composable
internal fun PathCard(progress: PathProgress, onOpenPath: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                onClickLabel = stringResource(R.string.path_open_description),
                role = Role.Button,
                onClick = onOpenPath
            )
            .padding(20.dp)
            .testTag("path_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.path_card_title),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PathStageEmblem(stage = progress.stage, size = 52.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(progress.stage.titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = TextLight,
                        modifier = Modifier.testTag("path_card_stage_title")
                    )
                    Text(
                        text = progressLine(progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSlate
                    )
                }
            }
            val next = progress.nextStage
            if (next != null && progress.closedDays > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                val span = next.minDays - progress.stage.minDays
                LinearProgressIndicator(
                    progress = { (progress.closedDays - progress.stage.minDays).toFloat() / span },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = IndigoAccent,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.path_next_stage, stringResource(next.titleRes)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            rhythmLine(progress)?.let { line ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = line, style = MaterialTheme.typography.bodySmall, color = TextSlate)
            }
        }
    }
}

@Composable
private fun progressLine(progress: PathProgress): String {
    val daysToNext = progress.daysToNextStage
    return when {
        progress.closedDays == 0 -> stringResource(R.string.path_card_beginning)
        daysToNext == null -> stringResource(R.string.path_card_final)
        else -> pluralStringResource(R.plurals.path_days_to_next, daysToNext, daysToNext)
    }
}

/** Null until a first week has been in rhythm, so a newcomer is not shown a row of zeros. */
@Composable
private fun rhythmLine(progress: PathProgress): String? {
    if (progress.longestRhythmWeeks == 0) return null
    val longest = stringResource(R.string.path_rhythm_longest, progress.longestRhythmWeeks)
    if (progress.rhythmWeeks == 0) return longest.replaceFirstChar { it.titlecase() }
    val current = pluralStringResource(R.plurals.path_rhythm_current, progress.rhythmWeeks, progress.rhythmWeeks)
    return "$current · $longest"
}
```

- [ ] **Step 5: Zapojit do `HomeScreen`**

Do signatury `HomeScreen` za `onNavigateToSettings`:

```kotlin
    onOpenPath: () -> Unit = {},
```

`HomeContent` dostane parametr `onOpenPath: () -> Unit` a volání z `HomeScreen` mu ho předá. V `HomeContent` za každé `WeeklyStreakCard(state)`:

- dvousloupcová větev (uvnitř `Column` se `spacedBy(18.dp)`):

```kotlin
                WeeklyStreakCard(state)
                state.pathProgress?.let { PathCard(it, onOpenPath) }
                WhyRitualCard()
```

- jednosloupcová větev:

```kotlin
        WeeklyStreakCard(state)
        state.pathProgress?.let {
            Spacer(modifier = Modifier.height(18.dp))
            PathCard(it, onOpenPath)
        }
        Spacer(modifier = Modifier.height(18.dp))
        WhyRitualCard()
```

- [ ] **Step 6: Ověřit testy**

Run: `./gradlew :app:testDebugUnitTest --tests "*HomePathCardTest*" --tests "*PathCardTitleFitsTest*" --tests "*HomeScreen*" --tests "*HomeTwoPaneTest*"`
Expected: PASS.

- [ ] **Step 7: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/java/org/clockout/presentation/home app/src/test/java/org/clockout/HomePathCardTest.kt app/src/test/java/org/clockout/PathCardTitleFitsTest.kt
git commit -m "feat: karta Cesta na domovské obrazovce"
```

---

### Task 6: Obrazovka Cesty — `PathActivity`, vstupy z Home a Nastavení

**Files:**
- Create: `app/src/main/java/org/clockout/presentation/path/PathScreen.kt`
- Create: `app/src/main/java/org/clockout/PathActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml` (za blok `BreathingStyleActivity`)
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsScreen.kt:134-160,1288-1312`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt:219-230,288-310`
- Modify: `app/src/test/java/org/clockout/screenshot/FormFactorScreenshotTest.kt`, `app/src/test/java/org/clockout/screenshot/FormFactorFixtures.kt`
- Test: `app/src/test/java/org/clockout/PathScreenTest.kt`

**Interfaces:**
- Consumes: `PathProgress`, `PathStage` (Task 1); `getPathProgress()` (Task 3); `titleRes`/`verseRes`/`descriptionRes`, `PathStageEmblem`, string klíče `path_screen_title`, `path_current_badge`, `path_origin_*`, `path_stages_title`, `path_stage_threshold`, `path_closed_days`, `path_rhythm_*`, `settings_about_path_button`, `common_back` (Task 4); `HomeScreen(onOpenPath)` (Task 5)
- Produces: `@Composable fun PathScreen(progress: PathProgress, onBack: () -> Unit, modifier: Modifier = Modifier)`; `class PathActivity : ComponentActivity`; `SettingsScreen(…, onOpenPath: () -> Unit = {}, …)`; test tagy `path_back_button`, `path_stage_row_<ID>`, `about_path_button`; `Fixtures.pathProgress`

- [ ] **Step 1: Napsat failing testy**

`app/src/test/java/org/clockout/PathScreenTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.presentation.path.PathScreen
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PathScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val progress = PathProgress(
        closedDays = 22, stage = PathStage.DOWN_THE_STAIRS, nextStage = PathStage.STREET_AT_DUSK,
        daysToNextStage = 13, rhythmWeeks = 3, longestRhythmWeeks = 4, isCurrentWeekInRhythm = true
    )

    private fun show(onBack: () -> Unit = {}) {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { PathScreen(progress = progress, onBack = onBack) }
            }
        }
    }

    @Test
    fun `the screen explains the origin and lists every stage in Czech`() {
        show()

        composeTestRule.onNodeWithText("Cesta domů").assertIsDisplayed()
        composeTestRule.onNodeWithText("22 uzavřených dní", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("O čem je cesta").performScrollTo().assertIsDisplayed()
        PathStage.entries.drop(1).forEach { stage ->
            composeTestRule.onNodeWithTag("path_stage_row_${stage.id}").performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("od 250 dní").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Jak funguje rytmus").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the back button closes the screen`() {
        var closed = false
        show(onBack = { closed = true })

        composeTestRule.onNodeWithTag("path_back_button").performClick()

        assertTrue(closed)
    }

    @Test
    fun `the about card opens the path`() {
        var opened = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {},
                        onOpenPath = { opened = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("about_path_button").performScrollTo().performClick()

        assertTrue(opened)
    }
}
```

- [ ] **Step 2: Ověřit, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "*PathScreenTest*"`
Expected: FAIL při kompilaci — `Unresolved reference: PathScreen`, `No parameter with name 'onOpenPath'`.

- [ ] **Step 3: `PathScreen`**

`app/src/main/java/org/clockout/presentation/path/PathScreen.kt`:

```kotlin
package org.clockout.presentation.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.PathProgress
import org.clockout.domain.model.PathStage
import org.clockout.presentation.components.ContentMaxWidth
import org.clockout.presentation.components.FrostedBackgroundContainer
import org.clockout.presentation.components.PathStageEmblem
import org.clockout.presentation.components.frostedGlass
import org.clockout.presentation.components.maxContentWidth
import org.clockout.presentation.util.descriptionRes
import org.clockout.presentation.util.titleRes
import org.clockout.presentation.util.verseRes
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextMuted
import org.clockout.ui.theme.TextSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathScreen(progress: PathProgress, onBack: () -> Unit, modifier: Modifier = Modifier) {
    FrostedBackgroundContainer(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .maxContentWidth(ContentMaxWidth)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.path_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextLight
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("path_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CurrentStageHeader(progress)
                TextSection(R.string.path_origin_title, R.string.path_origin_text)
                Text(
                    text = stringResource(R.string.path_stages_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextLight
                )
                PathStage.entries.drop(1).forEach { stage ->
                    StageRow(
                        stage = stage,
                        isReached = progress.closedDays >= stage.minDays,
                        isCurrent = stage == progress.stage
                    )
                }
                TextSection(R.string.path_rhythm_title, R.string.path_rhythm_text)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CurrentStageHeader(progress: PathProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(isActive = true, cornerRadius = 24.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PathStageEmblem(stage = progress.stage, size = 80.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.path_current_badge),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = TextMuted
        )
        Text(
            text = stringResource(progress.stage.titleRes),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        val days = pluralStringResource(R.plurals.path_closed_days, progress.closedDays, progress.closedDays)
        val rhythm = if (progress.longestRhythmWeeks == 0) null else
            pluralStringResource(R.plurals.path_rhythm_current, progress.rhythmWeeks, progress.rhythmWeeks) +
                " · " + stringResource(R.string.path_rhythm_longest, progress.longestRhythmWeeks)
        Text(
            text = listOfNotNull(days, rhythm).joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSlate,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StageRow(stage: PathStage, isReached: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(isActive = isCurrent, cornerRadius = 20.dp)
            .padding(16.dp)
            .testTag("path_stage_row_${stage.id}"),
        verticalAlignment = Alignment.Top
    ) {
        PathStageEmblem(stage = stage, size = 44.dp, isDimmed = !isReached)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f).alpha(if (isReached) 1f else 0.7f)) {
            Text(
                text = pluralStringResource(R.plurals.path_stage_threshold, stage.minDays, stage.minDays),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) IndigoAccent else TextMuted
            )
            Text(
                text = stringResource(stage.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(stage.verseRes),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(stage.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = TextSlate,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun TextSection(titleRes: Int, textRes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(cornerRadius = 22.dp)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = TextLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = TextSlate,
            lineHeight = 20.sp
        )
    }
}
```

- [ ] **Step 4: `PathActivity` a manifest**

`app/src/main/java/org/clockout/PathActivity.kt`:

```kotlin
package org.clockout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.presentation.path.PathScreen
import org.clockout.presentation.rememberRitualRepository
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.util.applyFrostedWindowChrome

/**
 * The path explained, reached from the home card and from the About card in Settings. An
 * Activity of its own for the same reasons as [BreathingStyleActivity]: it is a peer of neither
 * Home nor History, and has no place on the navigation rail.
 */
class PathActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFrostedWindowChrome()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FrostedCanvas) {
                    val repository = rememberRitualRepository()
                    val language by repository.getLanguage()
                        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
                    val progress by repository.getPathProgress()
                        .collectAsStateWithLifecycle(initialValue = PathProgress.EMPTY)

                    AppLocalization(language) {
                        PathScreen(progress = progress, onBack = { finish() })
                    }
                }
            }
        }
    }
}
```

`AndroidManifest.xml` — za element `BreathingStyleActivity`:

```xml
        <!-- The path explained. Its own Activity for the same reasons as the breathing picker. -->
        <activity
            android:name=".PathActivity"
            android:exported="false"
            android:label="@string/path_screen_title"
            android:parentActivityName=".MainActivity"
            android:theme="@style/Theme.MyApplication" />
```

- [ ] **Step 5: Tlačítko v kartě „O aplikaci"**

`SettingsScreen` — parametr za `onOpenBreathingStyle`:

```kotlin
    onOpenPath: () -> Unit = {},
```

V About Card za `Text(settings_about_desc)`:

```kotlin
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onOpenPath,
                            modifier = Modifier.testTag("about_path_button")
                        ) {
                            Text(stringResource(R.string.settings_about_path_button))
                        }
```

- [ ] **Step 6: Wiring v `PadlaApp`**

Import `org.clockout.PathActivity`. V `HomeScreen(...)` volání:

```kotlin
                                onOpenPath = {
                                    context.startActivity(Intent(context, PathActivity::class.java))
                                },
```

a totéž jako `onOpenPath` do `SettingsScreen(...)` za `onOpenBreathingStyle`.

- [ ] **Step 7: Screenshot**

`FormFactorFixtures.kt` — do `object Fixtures`:

```kotlin
    val pathProgress = PathProgress(
        closedDays = 22, stage = PathStage.DOWN_THE_STAIRS, nextStage = PathStage.STREET_AT_DUSK,
        daysToNextStage = 13, rhythmWeeks = 3, longestRhythmWeeks = 4, isCurrentWeekInRhythm = true
    )
```

a do `homeOpen` i `homeClosed` přidat `pathProgress = pathProgress`. `FormFactorScreenshotTest.kt` — nový test:

```kotlin
    @Test fun path() = capture("path") {
        PathScreen(progress = Fixtures.pathProgress, onBack = {})
    }
```

- [ ] **Step 8: Ověřit testy a sestavení**

Run: `./gradlew :app:testDebugUnitTest --tests "*PathScreenTest*" --tests "*Settings*" && ./gradlew :app:assembleDebug`
Expected: PASS, APK sestaven.

- [ ] **Step 9: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/org/clockout/PathActivity.kt app/src/main/java/org/clockout/presentation app/src/test/java/org/clockout/PathScreenTest.kt app/src/test/java/org/clockout/screenshot
git commit -m "feat: obrazovka Cesty s vysvětlením stupňů a vstupy z Home a Nastavení"
```

---

### Task 7: Oslava na dokončovací kartě rituálu

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualState.kt`
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt:230-260`
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualScreen.kt:271-278,863-944`
- Test: `app/src/test/java/org/clockout/RitualPathCelebrationTest.kt`, `app/src/test/java/org/clockout/RitualScreenLocalizationTest.kt`

**Interfaces:**
- Consumes: `getPathProgress()` (Task 3); `PathStage`, `PathStageEmblem`, `titleRes`, `verseRes`, `ritual_path_stage_reached`, `ritual_path_rhythm_week` (Tasks 1, 4)
- Produces: `RitualState.reachedStage: PathStage? = null`, `RitualState.rhythmWeekReached: Boolean = false`; test tagy `ritual_path_stage_reached`, `ritual_path_rhythm_week`

- [ ] **Step 1: Napsat failing testy ViewModelu**

`app/src/test/java/org/clockout/RitualPathCelebrationTest.kt`:

```kotlin
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
        seedThisWeek(otherDays = 5)

        assertTrue(finishRitual().state.value.rhythmWeekReached)
    }

    @Test
    fun `a ritual that leaves the week short marks no rhythm`() = runTest(dispatcher) {
        seedThisWeek(otherDays = 4)

        assertFalse(finishRitual().state.value.rhythmWeekReached)
    }
}
```

- [ ] **Step 2: Napsat failing UI test**

Do `RitualScreenLocalizationTest.kt` (použij jeho existující `question` fixture a vzor `setContent`):

```kotlin
    @Test
    fun `the completion card names a newly reached stage`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            question = question,
                            currentStep = RitualStep.Log,
                            selectedMood = MoodTag.CALM,
                            isCompletedAndSaved = true,
                            reachedStage = PathStage.DOWN_THE_STAIRS
                        ),
                        onIntent = {},
                        onFinish = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Nový stupeň cesty").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Schody dolů").assertIsDisplayed()
    }
```

- [ ] **Step 3: Ověřit, že testy padají**

Run: `./gradlew :app:testDebugUnitTest --tests "*RitualPathCelebrationTest*" --tests "*RitualScreenLocalizationTest*"`
Expected: FAIL při kompilaci — `Unresolved reference: reachedStage`.

- [ ] **Step 4: Stav**

`RitualState.kt` — za `shouldRequestReview`:

```kotlin
    /** The stage this ritual has just reached, or null when it reached none. */
    val reachedStage: PathStage? = null,
    /** Whether this ritual has just brought the current week up to the rhythm threshold. */
    val rhythmWeekReached: Boolean = false
```

- [ ] **Step 5: ViewModel**

V `handleMoodSelected` uvnitř `viewModelScope.launch`, **před** `logMoodUseCase(...)`:

```kotlin
            val pathBefore = ritualRepository.getPathProgress().first()
```

a nahradit stávající `_state.update { it.copy(isCompletedAndSaved = true, shouldRequestReview = …) }`:

```kotlin
            val pathAfter = ritualRepository.getPathProgress().first()
            _state.update {
                it.copy(
                    isCompletedAndSaved = true,
                    shouldRequestReview = moodWasChosen && shouldRequestReviewUseCase(mood),
                    reachedStage = pathAfter.stage.takeIf { stage -> stage != pathBefore.stage },
                    rhythmWeekReached = pathAfter.isCurrentWeekInRhythm && !pathBefore.isCurrentWeekInRhythm
                )
            }
```

- [ ] **Step 6: UI**

`RitualScreen.kt` — volání `StepLogContent(...)` dostane `reachedStage = state.reachedStage, rhythmWeekReached = state.rhythmWeekReached`; signatura `StepLogContent` dva nové parametry `reachedStage: PathStage?`, `rhythmWeekReached: Boolean`. V dokončovací kartě za `Text(home_workday_done_desc)`:

```kotlin
                    if (reachedStage != null) {
                        Spacer(modifier = Modifier.height(22.dp))
                        PathStageEmblem(stage = reachedStage, size = 56.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.ritual_path_stage_reached),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = TextMuted,
                            modifier = Modifier.testTag("ritual_path_stage_reached")
                        )
                        Text(
                            text = stringResource(reachedStage.titleRes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(reachedStage.verseRes),
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = TextSlate,
                            textAlign = TextAlign.Center
                        )
                    } else if (rhythmWeekReached) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.ritual_path_rhythm_week),
                            style = MaterialTheme.typography.bodySmall,
                            color = IndigoAccent,
                            modifier = Modifier.testTag("ritual_path_rhythm_week")
                        )
                    }
```

Importy: `org.clockout.domain.model.PathStage`, `org.clockout.presentation.components.PathStageEmblem`, `org.clockout.presentation.util.titleRes`, `org.clockout.presentation.util.verseRes`, `androidx.compose.ui.text.font.FontStyle` (a `IndigoAccent`/`TextMuted`, pokud v souboru chybí).

- [ ] **Step 7: Ověřit testy**

Run: `./gradlew :app:testDebugUnitTest --tests "*Ritual*"`
Expected: PASS (včetně stávajících `RitualReviewRequestTest`, `RitualTelemetryTest`).

- [ ] **Step 8: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/java/org/clockout/presentation/ritual app/src/test/java/org/clockout/RitualPathCelebrationTest.kt app/src/test/java/org/clockout/RitualScreenLocalizationTest.kt
git commit -m "feat: oslava nového stupně a týdne v rytmu po dokončení rituálu"
```

---

### Task 8: Texty dialogů, screenshoty, dokumentace, celková kontrola

**Files:**
- Modify: `app/src/main/res/values/strings.xml:52,395`, `app/src/main/res/values-cs/strings.xml:54,399`
- Modify: `CLAUDE.md`, `AGENTS.md`

- [ ] **Step 1: Dialogy smazání**

Na konec textu (uvnitř `<string>`):

| Klíč | EN přidat | CS přidat |
|---|---|---|
| `history_clear_dialog_text` | ` Your stage on the path and your rhythm stay.` | ` Stupeň cesty a rytmus zůstanou zachovány.` |
| `settings_clear_data_dialog_text` | ` Your stage on the path and your rhythm stay.` | ` Stupeň cesty a rytmus zůstanou zachovány.` |

Ověř: `grep -rn "dialog_text" app/src/test` — pokud test porovnává celý text dialogu, uprav očekávaný řetězec.

- [ ] **Step 2: Dokumentace (`CLAUDE.md` i `AGENTS.md` shodně)**

- *Architecture*: nový odstavec „Cesta" — `PathStage`/`PathRhythm`/`PathPolicy` v `domain/model`, prahy, pravidlo rytmu (práh `nabízené − 1`, dva slabé týdny po sobě, rozpracovaný týden neláme, první týden jen od prvního dne), `getPathProgress()`, `PathCard` na Home, oslava v `RitualViewModel` porovnáním před/po zápisu, `PathActivity` po vzoru `BreathingStyleActivity` a vstupy z Home a „O aplikaci".
- *Module layout*: `presentation/path`.
- *Persistence and backup*: `practice_days` (jen datum, DB v2, `MIGRATION_1_2` seeduje z `ritual_logs`, legacy migrátor plní obě tabulky), přežije „Clear data and history" stejně jako čítač a příznak recenze; jede v záloze s `clockout.db`.
- *Testing*: nové testy; Home baseline obsahuje kartu Cesta; `MigrationTestHelper` čte schémata z `schemas/` přes test assets.
- *Key stack versions* (Project overview): `room-testing` 2.8.5.
- *What NOT to do*: nemazat `practice_days` v `clearLogs`; neukládat do `practice_days` nic než datum; nepočítat stupně ani rytmus z `ritual_logs`; nezapomenout na `practiceDayDao.insertAll` v legacy migrátoru.

- [ ] **Step 3: Celková kontrola**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"
./gradlew :app:recordRoborazziDebug --tests "*PhoneNavigationTest*"
./gradlew :app:recordRoborazziDebug --tests "*TabletNavigationTest*"
./gradlew :app:verifyRoborazziDebug
./gradlew :app:assembleDebug
```

Expected: vše zelené. Záznam baseline je tu oprávněný — Home i nová obrazovka se mění záměrně. Prohlédni nové snímky `home_open`, `home_closed` a `path` (`app/src/test/screenshots/formfactor/**`) na všech čtyřech tvarech okna: karta nesmí přetékat, znaky musí být viditelné. Navigační testy o víkendu padají známým způsobem (CLAUDE.md) — spusť je ve všední den nebo samostatně.

- [ ] **Step 4: Ruční ověření na zařízení**

Nainstaluj debug APK přes existující instalaci (ne čistou), aby proběhla migrace 1 → 2: stupeň na Home musí odpovídat počtu dní v Historii. Dokonči rituál → dokončovací karta. Smaž data v Nastavení → Historie prázdná, karta Cesta beze změny. Otevři Cestu z Home i z „O aplikaci", přepni jazyk.

- [ ] **Step 5: Commit (po souhlasu uživatele)**

```bash
git add app/src/main/res CLAUDE.md AGENTS.md
git commit -m "docs: dokumentace Cesty a upozornění v dialozích smazání"
```
