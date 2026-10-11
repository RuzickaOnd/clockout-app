# Noční obloha — implementační plán

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Přidat obrazovku Noční obloha (hvězda za uzavřený den, souhvězdí za týden v rytmu, obloha po měsících, atlas 53 souhvězdí) s náhledem v Historii.

**Architecture:** Čistá doména (`Constellation`/`ConstellationCatalog`, `SkyLayout`, `SkyPolicy`, `SkyData`) nad stávajícími `practice_days`, `ritual_logs` a `PathRhythm`, který nově vrací výsledek po týdnech. `RitualRepository.getSky()` skládá `SkyData`; UI tvoří `SkyCanvas`, `SkyScreen` v nové `SkyActivity`, `SkyPreviewCard` v Historii a jméno souhvězdí na dokončovací kartě rituálu.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00, `HorizontalPager`), Room 2.8.5, `java.time` (`IsoFields`, `YearMonth`), Robolectric 4.17, Roborazzi 1.76.0.

**Spec:** `docs/superpowers/specs/2026-10-09-nocni-obloha-design.md`

## Global Constraints

- **Git:** nic nestageovat ani necommitovat bez výslovného souhlasu uživatele v témže tahu (CLAUDE.md). Krok „Commit" znamená: na konci práce se zeptat a commitnout po „ano". Commit message česky s prefixem a `Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>`.
- Práce pokračuje na větvi `feature/CL_cesta` (Obloha staví na Cestě, která ještě není v `develop`).
- `domain/` bez `R`, androidx importů a textů; texty jen v `values/strings.xml` + `values-cs/strings.xml` se stejnými klíči, české plurály `one`/`few`/`many`/`other`.
- Jen statické `R.*` reference (`isShrinkResources`); jména souhvězdí jako `string-array name="constellation_names"`, index = číslo týdne − 1.
- Obloha je čistá funkce dat: žádná náhodnost za běhu. Pseudonáhoda jen přes `java.util.Random(seed)` se semínkem z dat (algoritmus je specifikovaný, tedy stabilní).
- Rytmus jen z `PathRhythm` — žádná druhá logika.
- Testy obrazovek v `AppLocalization`; měření textu s `@GraphicsMode(NATIVE)`; Robolectric `@Config(sdk = [36])`.
- `CLAUDE.md` a `AGENTS.md` se upravují společně (Úkol 9).

## Review Focus

1. **Týden přes přelom měsíce i roku** (např. Po 29. 12. 2025 – Ne 4. 1. 2026) — patří měsíci svého čtvrtku a má souhvězdí 1, ne 53. Test: Úkol 4 (`a week across new year belongs to January as constellation 1`).
2. **Rok s 53 týdny** (2026 jím je: pondělí 28. 12. 2026 začíná 53. týden) — souhvězdí 53 se objeví a souhrn „letos X z 53". Test: Úkol 4 (`2026 has 53 weeks and the blue moon`).
3. **Smazaná historie** — hvězdy zůstanou, bez nálad (bílé), karta dne ukáže „Záznam byl smazán". Testy: Úkol 5 (`clearLogs keeps the sky but drops the moods`), Úkol 6 (`a star without an entry says the entry was deleted`).
4. **Prázdná Historie po smazání** — náhled oblohy se přesto ukáže. Test: Úkol 7 (`the sky preview shows above an empty history`).
5. **Uživatel bez víkendů** — figury Po–Pá musí být rozlišitelné. Test: Úkol 2 (`weekday outlines are distinct`).

---

### Task 1: `PathRhythm` vrací výsledek po týdnech

**Files:**
- Modify: `app/src/main/java/org/clockout/domain/model/PathRhythm.kt`
- Test: `app/src/test/java/org/clockout/PathRhythmTest.kt`

**Interfaces:**
- Produces: `PathRhythm.Result.weeks: Map<LocalDate, Boolean>` — pondělí každého týdne od týdne prvního dne po aktuální týden včetně, který nabídl aspoň jeden den → splněn práh. Aktuální týden je v mapě se skutečnou hodnotou (i `false`).

- [ ] **Step 1: Failing testy** — do `PathRhythmTest.kt` přidat:

```kotlin
    @Test
    fun `weeks lists every week from the first one with its result`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(mapOf(w1 to true, w2 to false, w3 to true, w4 to false), result.weeks)
    }

    @Test
    fun `the week in progress appears in weeks once it is met`() {
        val result = eval(workweek(w3) + days(w4, 0, 1, 2, 3), today = w4.plusDays(3))
        assertEquals(true, result.weeks[w4])
    }

    @Test
    fun `a week before the first ritual is not in weeks`() {
        assertEquals(setOf(w3, w4), eval(days(w3, 4)).weeks.keys)
    }

    @Test
    fun `current and longest agree with weeks`() {
        val result = eval(workweek(w1) + days(w2, 0, 1, 2) + workweek(w3))
        assertEquals(2, result.weeks.values.count { it })
        assertEquals(2, result.current)
    }
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*PathRhythmTest*"` → FAIL při kompilaci, `Unresolved reference 'weeks'`.

- [ ] **Step 3: Implementace** — `Result` a `evaluate` (KDoc objektu zároveň přelomit na řádky do 100 znaků; v aktuálním textu je jeden řádek přetažený):

```kotlin
    data class Result(
        val current: Int,
        val longest: Int,
        val isCurrentWeekInRhythm: Boolean,
        /**
         * Monday of every week since the first closed day that offered a day at all, mapped to
         * whether it was in rhythm. The night sky draws its constellations from this map, so the
         * path and the sky can never disagree about a week.
         */
        val weeks: Map<LocalDate, Boolean> = emptyMap()
    )

    fun evaluate(days: Set<LocalDate>, includeWeekends: Boolean, today: LocalDate): Result {
        if (days.isEmpty()) return Result(0, 0, false)
        val firstDay = days.min()
        val currentWeek = today.mondayOfWeek()

        val weeks = generateSequence(firstDay.mondayOfWeek()) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(currentWeek) }
            .mapNotNull { monday -> isWeekMet(monday, days, includeWeekends, firstDay)?.let { monday to it } }
            .toMap()
        val isCurrentWeekInRhythm = weeks[currentWeek] == true

        var run = 0
        var longest = 0
        var weakInARow = 0
        // The week in progress only ever adds: until it is met it is left out rather than weak.
        for ((monday, met) in weeks) {
            if (monday == currentWeek && !met) continue
            if (met) {
                run++
                weakInARow = 0
                longest = maxOf(longest, run)
            } else {
                weakInARow++
                if (weakInARow >= WEAK_WEEKS_THAT_END_A_RHYTHM) run = 0
            }
        }
        return Result(run, longest, isCurrentWeekInRhythm, weeks)
    }
```

(`toMap()` vrací `LinkedHashMap`, pořadí týdnů zůstává chronologické.)

- [ ] **Step 4: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*PathRhythmTest*" --tests "*Path*"` → PASS (všechny stávající testy Cesty beze změny).

- [ ] **Step 5: Commit (po souhlasu)** — `feat: rytmus cesty vrací výsledek po jednotlivých týdnech`

---

### Task 2: Katalog 53 souhvězdí a jejich jména

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/Constellation.kt`
- Create: `app/src/main/java/org/clockout/domain/model/ConstellationCatalog.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Test: `app/src/test/java/org/clockout/ConstellationCatalogTest.kt`

**Interfaces:**
- Produces: `data class SkyPoint(val x: Float, val y: Float)`; `data class Constellation(val number: Int, val stars: List<SkyPoint>, val edges: List<Pair<Int, Int>>)`; `object ConstellationCatalog { const val SIZE = 53; val all: List<Constellation>; fun forWeek(number: Int): Constellation }`; `R.array.constellation_names` (53 položek).

- [ ] **Step 1: Failing test** — `ConstellationCatalogTest.kt`:

```kotlin
package org.clockout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.util.localizedFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConstellationCatalogTest {

    private val all = ConstellationCatalog.all

    @Test
    fun `the catalog holds one constellation per ISO week`() {
        assertEquals((1..53).toList(), all.map { it.number })
    }

    @Test
    fun `every figure has seven stars inside the unit square`() {
        all.forEach { c ->
            assertEquals("figure ${c.number}", 7, c.stars.size)
            assertTrue("figure ${c.number}", c.stars.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test
    fun `every figure has five to eight valid distinct edges`() {
        all.forEach { c ->
            assertTrue("figure ${c.number}: ${c.edges.size}", c.edges.size in 5..8)
            assertTrue("figure ${c.number}", c.edges.all { (a, b) -> a != b && a in 0..6 && b in 0..6 })
            val undirected = c.edges.map { (a, b) -> minOf(a, b) to maxOf(a, b) }
            assertEquals("figure ${c.number}", undirected.size, undirected.toSet().size)
        }
    }

    @Test
    fun `the weekdays alone draw at least three lines`() {
        all.forEach { c ->
            assertTrue("figure ${c.number}", c.edges.count { (a, b) -> a < 5 && b < 5 } >= 3)
        }
    }

    @Test
    fun `weekday outlines are distinct`() {
        val outlines = all.map { it.number to it.weekdayOutline() }
        for (i in outlines.indices) for (j in i + 1 until outlines.size) {
            val (na, a) = outlines[i]
            val (nb, b) = outlines[j]
            val sameEdges = a.second == b.second
            val maxGap = a.first.zip(b.first).maxOf { (p, q) -> hypot(p.first - q.first, p.second - q.second) }
            assertTrue("figures $na and $nb look the same without the weekend", !sameEdges || maxGap > 0.15f)
        }
    }

    @Test
    fun `there is a name for every constellation in both languages`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppLanguage.entries.forEach { language ->
            val names = context.localizedFor(language).resources.getStringArray(R.array.constellation_names)
            assertEquals(language.name, 53, names.size)
            assertTrue(language.name, names.all { it.isNotBlank() })
        }
    }

    /** Weekday points centred on their centroid and scaled to unit size, plus the weekday edges. */
    private fun org.clockout.domain.model.Constellation.weekdayOutline():
        Pair<List<Pair<Float, Float>>, Set<Pair<Int, Int>>> {
        val pts = stars.take(5)
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()
        val scale = pts.maxOf { hypot(it.x - cx, it.y - cy) }.coerceAtLeast(1e-3f)
        val norm = pts.map { (it.x - cx) / scale to (it.y - cy) / scale }
        val e = edges.filter { (a, b) -> a < 5 && b < 5 }.map { (a, b) -> minOf(a, b) to maxOf(a, b) }.toSet()
        return norm to e
    }
}
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*ConstellationCatalogTest*"` → FAIL, `Unresolved reference 'ConstellationCatalog'`.

- [ ] **Step 3: Model** — `Constellation.kt`:

```kotlin
package org.clockout.domain.model

/** A point on the unit square, y growing downwards like on screen. */
data class SkyPoint(val x: Float, val y: Float)

/**
 * One constellation of the night sky, owned by an ISO week number. [stars] holds exactly seven
 * points, Monday first: the five weekdays draw the recognisable outline, Saturday and Sunday only
 * ornament it, so the figure still reads for someone who never closes a weekend.
 */
data class Constellation(
    val number: Int,
    val stars: List<SkyPoint>,
    val edges: List<Pair<Int, Int>>
)
```

- [ ] **Step 4: Katalog** — `ConstellationCatalog.kt` s kompaktním zápisem; prvních šest figur je navrženo, zbytek navrhnout v tomto kroku:

```kotlin
package org.clockout.domain.model

/**
 * The 53 constellations, one per ISO week. Shipped figures must never change shape or order:
 * every user's sky and atlas are drawn from them.
 */
object ConstellationCatalog {

    const val SIZE = 53

    fun forWeek(number: Int): Constellation = all[number.coerceIn(1, SIZE) - 1]

    val all: List<Constellation> = listOf(
        // number, seven "x,y" points Mon..Sun, edges
        c(10, "0.30,0.78 0.62,0.78 0.72,0.50 0.92,0.30 0.30,0.45 0.50,0.30 0.15,0.60", "0-1 1-2 2-3 2-4 4-0 4-5 4-6"),
        // …
    ).sortedBy { it.number }

    private fun c(number: Int, points: String, edges: String) = Constellation(
        number = number,
        stars = points.split(' ').map { p -> p.split(',').let { (x, y) -> SkyPoint(x.toFloat(), y.toFloat()) } },
        edges = edges.split(' ').map { e -> e.split('-').let { (a, b) -> a.toInt() to b.toInt() } }
    )
}
```

Ukázky (už ověřené náhledem; Konvice je proti náhledu ze spec fáze přepracovaná, protože pětiúhelník kolidoval s Dýní):

| # | Jméno | body Po..Ne | hrany |
|---|---|---|---|
| 10 | Konvice | `0.30,0.78 0.62,0.78 0.72,0.50 0.92,0.30 0.30,0.45 0.50,0.30 0.15,0.60` | `0-1 1-2 2-3 2-4 4-0 4-5 4-6` |
| 11 | Deštník u dveří | `0.10,0.45 0.30,0.25 0.50,0.20 0.70,0.25 0.90,0.45 0.50,0.80 0.38,0.90` | `0-1 1-2 2-3 3-4 4-0 2-5 5-6` |
| 20 | Kočka na parapetu | `0.33,0.18 0.55,0.18 0.44,0.42 0.72,0.78 0.32,0.80 0.92,0.55 0.10,0.92` | `0-1 1-2 2-0 2-4 4-3 3-1 3-5 4-6` |
| 33 | Padající hvězda | `0.75,0.25 0.60,0.40 0.45,0.52 0.30,0.64 0.15,0.78 0.86,0.12 0.90,0.36` | `0-1 1-2 2-3 3-4 0-5 0-6` |
| 40 | Dýně | `0.20,0.58 0.35,0.82 0.65,0.82 0.80,0.58 0.50,0.38 0.50,0.18 0.70,0.20` | `0-1 1-2 2-3 3-4 4-0 4-5 5-6` |
| 53 | Modrý měsíc | `0.55,0.10 0.30,0.25 0.22,0.50 0.30,0.75 0.55,0.90 0.44,0.50 0.82,0.30` | `0-1 1-2 2-3 3-4 4-5 5-0` |

Zbývajících 47 figur navrhni podle pravidel ze specifikace (Po–Pá nese obrys, So/Ne ozdoba, jeden rozpoznávací rys, 5–8 hran). Hlídá je test z kroku 1. Po návrhu vykresli přehled (krok 6) a zkontroluj jej očima — figura, která nepřipomíná nic, se překreslí.

- [ ] **Step 5: Jména** — do obou `strings.xml` `string-array name="constellation_names"` s 53 položkami přesně podle tabulky ve specifikaci (sekce Katalog souhvězdí → Jména), EN do `values/`, CS do `values-cs/`. Apostrofy escapovat (`\'`).

- [ ] **Step 6: Přehled figur** — skript do scratchpadu (ne do repa), který z `ConstellationCatalog.kt` vyparsuje volání `c(...)` a vykreslí SVG mřížku 53 figur dvakrát (celý týden / jen Po–Pá) s čísly a českými jmény, převod `qlmanage -t -s 1600`. Obrázek prohlédnout a uložit cestu pro závěrečnou zprávu uživateli (figury jsou obsah ke schválení).

- [ ] **Step 7: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*ConstellationCatalogTest*"` → PASS (6 testů).

- [ ] **Step 8: Commit (po souhlasu)** — `feat: katalog 53 souhvězdí noční oblohy`

---

### Task 3: `SkyLayout` — sloty měsíce, pootočení, promítání a zásah

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/SkyLayout.kt`
- Modify: `docs/superpowers/specs/2026-10-09-nocni-obloha-design.md` (věta o posunu figur, viz krok 3)
- Test: `app/src/test/java/org/clockout/SkyLayoutTest.kt`

**Interfaces:**
- Consumes: `SkyPoint` (Task 2)
- Produces: `data class SkySlot(val centerX: Float, val centerY: Float, val size: Float, val rotationDegrees: Float)`; `object SkyLayout { const val ASPECT = 0.75f; fun slots(year: Int, weekNumbers: List<Int>): List<SkySlot>; fun project(slot: SkySlot, point: SkyPoint): SkyPoint; fun fieldStars(year: Int, month: Int): List<FieldStar>; fun <T> nearest(candidates: List<Pair<T, SkyPoint>>, tap: SkyPoint, maxDistance: Float): T? }`; `data class FieldStar(val x: Float, val y: Float, val radius: Float, val phase: Float)`.

Souřadnice: `x` je zlomek šířky plátna, `y` zlomek výšky; plátno má šířku 1 a výšku `1 / ASPECT`. `size` je zlomek šířky.

- [ ] **Step 1: Failing test** — `SkyLayoutTest.kt`:

```kotlin
package org.clockout

import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkyPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class SkyLayoutTest {

    private val height = 1f / SkyLayout.ASPECT

    @Test
    fun `slots for four and five weeks stay inside the canvas and never overlap`() {
        listOf(listOf(40, 41, 42, 43), listOf(40, 41, 42, 43, 44)).forEach { weeks ->
            val slots = SkyLayout.slots(2026, weeks)
            assertEquals(weeks.size, slots.size)
            // A figure rotated any way fits a circle of radius size * √2 / 2.
            val circles = slots.map { Triple(it.centerX, it.centerY * height, it.size * 0.7072f) }
            circles.forEach { (x, y, r) ->
                assertTrue("x $x r $r", x - r >= 0f && x + r <= 1f)
                assertTrue("y $y r $r", y - r >= 0f && y + r <= height)
            }
            for (i in circles.indices) for (j in i + 1 until circles.size) {
                val (x1, y1, r1) = circles[i]
                val (x2, y2, r2) = circles[j]
                assertTrue("slots $i and $j overlap", hypot(x1 - x2, y1 - y2) >= r1 + r2)
            }
        }
    }

    @Test
    fun `rotation stays within twelve degrees and is the same every time`() {
        val a = SkyLayout.slots(2026, listOf(1, 2, 3, 4, 5))
        assertEquals(a, SkyLayout.slots(2026, listOf(1, 2, 3, 4, 5)))
        assertTrue(a.all { it.rotationDegrees in -12f..12f })
    }

    @Test
    fun `the centre of a figure lands on the centre of its slot`() {
        val slot = SkyLayout.slots(2026, listOf(40, 41, 42, 43)).first()
        val p = SkyLayout.project(slot, SkyPoint(0.5f, 0.5f))
        assertEquals(slot.centerX, p.x, 1e-4f)
        assertEquals(slot.centerY, p.y, 1e-4f)
    }

    @Test
    fun `the star field is fixed by the month`() {
        assertEquals(60, SkyLayout.fieldStars(2026, 10).size)
        assertEquals(SkyLayout.fieldStars(2026, 10), SkyLayout.fieldStars(2026, 10))
        assertTrue(SkyLayout.fieldStars(2026, 10) != SkyLayout.fieldStars(2026, 11))
    }

    @Test
    fun `a tap picks the nearest star within reach and nothing beyond it`() {
        val stars = listOf("a" to SkyPoint(0.2f, 0.2f), "b" to SkyPoint(0.25f, 0.2f))
        assertEquals("b", SkyLayout.nearest(stars, SkyPoint(0.24f, 0.2f), maxDistance = 0.05f))
        assertNull(SkyLayout.nearest(stars, SkyPoint(0.8f, 0.8f), maxDistance = 0.05f))
    }
}
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*SkyLayoutTest*"` → FAIL, `Unresolved reference 'SkyLayout'`.

- [ ] **Step 3: Implementace** — `SkyLayout.kt` (sloty spočítané tak, aby ohraničující kružnice rotované figury nepřesáhly plátno a nepřekryly se; proto **bez** posunu, jen s pootočením — do specifikace v sekci „Měsíc na obloze" změnit „s mírným pootočením (±12°) a posunem" na „s mírným pootočením (±12°); posun by rozbil zaručené nepřekrývání slotů"):

```kotlin
package org.clockout.domain.model

import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class SkySlot(val centerX: Float, val centerY: Float, val size: Float, val rotationDegrees: Float)

data class FieldStar(val x: Float, val y: Float, val radius: Float, val phase: Float)

/**
 * Where a month's constellations sit on a canvas of width 1 and height 1 / [ASPECT]. Every value is
 * derived from the dates through java.util.Random, whose algorithm is specified, so a month always
 * looks the same - for the user and for the screenshot tests.
 */
object SkyLayout {

    const val ASPECT = 0.75f
    private const val FIELD_STARS = 60
    private const val MAX_ROTATION = 12f

    // Zig-zag columns; sized so a rotated figure's bounding circle stays inside and apart.
    private val FOUR = listOf(0.26f to 0.20f, 0.74f to 0.40f, 0.26f to 0.60f, 0.74f to 0.80f)
    private const val FOUR_SIZE = 0.35f
    private val FIVE = listOf(0.26f to 0.17f, 0.74f to 0.335f, 0.26f to 0.50f, 0.74f to 0.665f, 0.26f to 0.83f)
    private const val FIVE_SIZE = 0.31f

    fun slots(year: Int, weekNumbers: List<Int>): List<SkySlot> {
        val (positions, size) = if (weekNumbers.size <= 4) FOUR to FOUR_SIZE else FIVE to FIVE_SIZE
        return weekNumbers.take(positions.size).mapIndexed { i, week ->
            val rotation = Random(year * 100L + week).nextFloat() * 2 * MAX_ROTATION - MAX_ROTATION
            SkySlot(positions[i].first, positions[i].second, size, rotation)
        }
    }

    fun project(slot: SkySlot, point: SkyPoint): SkyPoint {
        val angle = Math.toRadians(slot.rotationDegrees.toDouble())
        val dx = (point.x - 0.5f) * slot.size
        val dy = (point.y - 0.5f) * slot.size
        val rx = (dx * cos(angle) - dy * sin(angle)).toFloat()
        val ry = (dx * sin(angle) + dy * cos(angle)).toFloat()
        return SkyPoint(slot.centerX + rx, slot.centerY + ry * ASPECT)
    }

    fun fieldStars(year: Int, month: Int): List<FieldStar> {
        val random = Random(year * 1_000L + month)
        return List(FIELD_STARS) {
            FieldStar(random.nextFloat(), random.nextFloat(), 0.4f + random.nextFloat() * 0.8f, random.nextFloat())
        }
    }

    /** Distances are measured on the canvas, where a unit of y is 1 / ASPECT units of x. */
    fun <T> nearest(candidates: List<Pair<T, SkyPoint>>, tap: SkyPoint, maxDistance: Float): T? =
        candidates
            .map { (item, p) -> item to hypot(p.x - tap.x, (p.y - tap.y) / ASPECT) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
            ?.first
}
```

- [ ] **Step 4: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*SkyLayoutTest*"` → PASS (5 testů).

- [ ] **Step 5: Commit (po souhlasu)** — `feat: rozložení souhvězdí na obloze měsíce`

---

### Task 4: `SkyPolicy` a `SkyData`

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/SkyPolicy.kt`
- Test: `app/src/test/java/org/clockout/SkyPolicyTest.kt`

**Interfaces:**
- Consumes: `PathRhythm.Result.weeks` (Task 1), `ConstellationCatalog.SIZE` (Task 2), `RitualLogEntry`, `MoodTag`
- Produces:
  - `data class SkyStar(val date: LocalDate, val pointIndex: Int, val mood: MoodTag?)`
  - `data class SkyWeek(val monday: LocalDate, val constellationNumber: Int, val isInRhythm: Boolean, val stars: List<SkyStar>)`
  - `data class SkyMonth(val yearMonth: YearMonth, val weeks: List<SkyWeek>)` s `val starCount: Int`
  - `data class SkySummary(val stars: Int, val constellationsLit: Int, val litThisYear: Int, val weeksThisYear: Int, val timesLitByNumber: Map<Int, Int>)`
  - `data class SkyData(val days: Set<LocalDate>, val logs: Map<LocalDate, RitualLogEntry>, val rhythmWeeks: Map<LocalDate, Boolean>, val today: LocalDate)` s `val months: List<YearMonth>`, `val summary: SkySummary`, `fun month(yearMonth: YearMonth): SkyMonth`; `companion val EMPTY`
  - `object SkyPolicy { fun constellationNumber(monday: LocalDate): Int; fun monthOf(monday: LocalDate): YearMonth; fun month(...); fun months(...); fun summary(...) }`

- [ ] **Step 1: Failing test** — `SkyPolicyTest.kt`:

```kotlin
package org.clockout

import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.domain.model.SkyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SkyPolicyTest {

    private fun log(date: LocalDate, mood: MoodTag) = RitualLogEntry(date.toString(), "Q", null, mood, 0L)

    private fun data(
        days: List<LocalDate>,
        rhythm: Map<LocalDate, Boolean> = emptyMap(),
        logs: List<RitualLogEntry> = emptyList(),
        today: LocalDate = LocalDate.of(2026, 10, 9)
    ) = SkyData(days.toSet(), logs.associateBy { LocalDate.parse(it.date) }, rhythm, today)

    @Test
    fun `a week belongs to the month of its thursday`() {
        // Mon 28 Sep 2026 - Sun 4 Oct 2026; Thursday is 1 October.
        assertEquals(YearMonth.of(2026, 10), SkyPolicy.monthOf(LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun `a week across new year belongs to January as constellation 1`() {
        val monday = LocalDate.of(2025, 12, 29)
        assertEquals(YearMonth.of(2026, 1), SkyPolicy.monthOf(monday))
        assertEquals(1, SkyPolicy.constellationNumber(monday))
    }

    @Test
    fun `2026 has 53 weeks and the blue moon`() {
        val monday = LocalDate.of(2026, 12, 28)
        assertEquals(53, SkyPolicy.constellationNumber(monday))
        val summary = data(listOf(monday), today = monday.plusDays(1)).summary
        assertEquals(53, summary.weeksThisYear)
    }

    @Test
    fun `a month lists its four or five weeks in order`() {
        val october = data(listOf(LocalDate.of(2026, 10, 5))).month(YearMonth.of(2026, 10))
        assertEquals(
            listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)),
            october.weeks.map { it.monday }
        )
        assertEquals(listOf(40, 41, 42, 43, 44), october.weeks.map { it.constellationNumber })
    }

    @Test
    fun `a star sits on the point of its weekday and carries the mood of its entry`() {
        val friday = LocalDate.of(2026, 10, 9)
        val saturday = LocalDate.of(2026, 10, 10)
        val week = data(listOf(friday, saturday), logs = listOf(log(friday, MoodTag.TENSE)))
            .month(YearMonth.of(2026, 10)).weeks.first { it.monday == LocalDate.of(2026, 10, 5) }
        assertEquals(listOf(4, 5), week.stars.map { it.pointIndex })
        assertEquals(MoodTag.TENSE, week.stars[0].mood)
        assertNull(week.stars[1].mood)
    }

    @Test
    fun `only a week in rhythm is a lit constellation`() {
        val monday = LocalDate.of(2026, 10, 5)
        val month = data(listOf(monday), rhythm = mapOf(monday to true, LocalDate.of(2026, 10, 12) to false))
            .month(YearMonth.of(2026, 10))
        assertTrue(month.weeks.first { it.monday == monday }.isInRhythm)
        assertFalse(month.weeks.first { it.monday == LocalDate.of(2026, 10, 12) }.isInRhythm)
    }

    @Test
    fun `months run from the first star to the current week`() {
        val sky = data(listOf(LocalDate.of(2026, 8, 3)), today = LocalDate.of(2026, 10, 9))
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)), sky.months)
    }

    @Test
    fun `a sky without days has no months`() {
        assertEquals(emptyList<YearMonth>(), SkyData.EMPTY.months)
    }

    @Test
    fun `the summary counts stars, lit weeks and distinct constellations this year`() {
        val lit2025 = LocalDate.of(2025, 10, 6)   // week 41 of 2025
        val lit2026a = LocalDate.of(2026, 10, 5)  // week 41 of 2026
        val lit2026b = LocalDate.of(2026, 9, 28)  // week 40 of 2026
        val summary = data(
            days = listOf(lit2025, lit2026a, lit2026b, LocalDate.of(2026, 10, 6)),
            rhythm = mapOf(lit2025 to true, lit2026a to true, lit2026b to true, LocalDate.of(2026, 9, 21) to false)
        ).summary
        assertEquals(4, summary.stars)
        assertEquals(3, summary.constellationsLit)
        assertEquals(2, summary.litThisYear)
        assertEquals(53, summary.weeksThisYear)
        assertEquals(mapOf(41 to 2, 40 to 1), summary.timesLitByNumber)
    }

    @Test
    fun `a month without stars has its weeks but no stars`() {
        val sky = data(listOf(LocalDate.of(2026, 8, 3)))
        assertEquals(0, sky.month(YearMonth.of(2026, 9)).starCount)
    }
}
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*SkyPolicyTest*"` → FAIL, `Unresolved reference 'SkyData'`.

- [ ] **Step 3: Implementace** — `SkyPolicy.kt`:

```kotlin
package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

data class SkyStar(val date: LocalDate, val pointIndex: Int, val mood: MoodTag?)

data class SkyWeek(
    val monday: LocalDate,
    val constellationNumber: Int,
    val isInRhythm: Boolean,
    val stars: List<SkyStar>
)

data class SkyMonth(val yearMonth: YearMonth, val weeks: List<SkyWeek>) {
    val starCount: Int get() = weeks.sumOf { it.stars.size }
}

data class SkySummary(
    val stars: Int,
    val constellationsLit: Int,
    val litThisYear: Int,
    val weeksThisYear: Int,
    val timesLitByNumber: Map<Int, Int>
)

/**
 * Everything the night sky is drawn from. Months are built on demand, page by page, rather than
 * all at once - a long history would otherwise rebuild years of sky on every new day.
 */
data class SkyData(
    val days: Set<LocalDate>,
    val logs: Map<LocalDate, RitualLogEntry>,
    val rhythmWeeks: Map<LocalDate, Boolean>,
    val today: LocalDate
) {
    val months: List<YearMonth> get() = SkyPolicy.months(days, today)
    val summary: SkySummary get() = SkyPolicy.summary(days, rhythmWeeks, today)
    fun month(yearMonth: YearMonth): SkyMonth = SkyPolicy.month(yearMonth, days, logs, rhythmWeeks)

    companion object {
        val EMPTY = SkyData(emptySet(), emptyMap(), emptyMap(), LocalDate.MIN)
    }
}

/**
 * A week belongs to the month its Thursday falls in - the rule ISO uses for years - so a week is
 * never split across two skies and every month holds four or five of them.
 */
object SkyPolicy {

    fun constellationNumber(monday: LocalDate): Int = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    fun monthOf(monday: LocalDate): YearMonth = YearMonth.from(monday.plusDays(3))

    fun month(
        yearMonth: YearMonth,
        days: Set<LocalDate>,
        logs: Map<LocalDate, RitualLogEntry>,
        rhythmWeeks: Map<LocalDate, Boolean>
    ): SkyMonth {
        val firstThursday = yearMonth.atDay(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY))
        val weeks = generateSequence(firstThursday.minusDays(3)) { it.plusWeeks(1) }
            .takeWhile { monthOf(it) == yearMonth }
            .map { monday ->
                SkyWeek(
                    monday = monday,
                    constellationNumber = constellationNumber(monday),
                    isInRhythm = rhythmWeeks[monday] == true,
                    stars = (0L until 7L).map { monday.plusDays(it) }
                        .filter { it in days }
                        .map { SkyStar(it, it.dayOfWeek.value - 1, logs[it]?.moodTag) }
                )
            }
            .toList()
        return SkyMonth(yearMonth, weeks)
    }

    fun months(days: Set<LocalDate>, today: LocalDate): List<YearMonth> {
        if (days.isEmpty()) return emptyList()
        val first = monthOf(days.min().mondayOfWeek())
        val last = maxOf(monthOf(today.mondayOfWeek()), first)
        return generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(last) }.toList()
    }

    fun summary(days: Set<LocalDate>, rhythmWeeks: Map<LocalDate, Boolean>, today: LocalDate): SkySummary {
        val lit = rhythmWeeks.filterValues { it }.keys
        val year = today.get(IsoFields.WEEK_BASED_YEAR)
        return SkySummary(
            stars = days.size,
            constellationsLit = lit.size,
            litThisYear = lit.filter { it.get(IsoFields.WEEK_BASED_YEAR) == year }
                .map(::constellationNumber).toSet().size,
            weeksThisYear = today.range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).maximum.toInt(),
            timesLitByNumber = lit.groupingBy(::constellationNumber).eachCount()
        )
    }

    private fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
```

Pozn.: `today.range(WEEK_OF_WEEK_BASED_YEAR)` vrací rozsah pro *week-based* rok data `today`; pro `SkyData.EMPTY` (`LocalDate.MIN`) se summary nevolá (žádné dny → UI ho neukáže), ale ověř, že `LocalDate.MIN.range(...)` nehází — pokud ano, vrať v `summary` pro prázdné `days` `SkySummary(0, 0, 0, 52, emptyMap())` předem.

- [ ] **Step 4: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*SkyPolicyTest*"` → PASS (10 testů).

- [ ] **Step 5: Commit (po souhlasu)** — `feat: doménová pravidla noční oblohy`

---

### Task 5: Repozitář — `getSky()`

**Files:**
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepository.kt`, `RitualRepositoryImpl.kt`
- Test: `app/src/test/java/org/clockout/PracticeDayPersistenceTest.kt`

**Interfaces:**
- Consumes: `SkyData` (Task 4), `PathRhythm.Result.weeks` (Task 1), `parsePracticeDay` (stávající, private v `RitualRepositoryImpl`)
- Produces: `RitualRepository.getSky(): Flow<SkyData>`

- [ ] **Step 1: Failing testy** — do `PracticeDayPersistenceTest.kt`:

```kotlin
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
```

(Import `org.junit.Assert.assertTrue`, pokud chybí.)

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*PracticeDayPersistenceTest*"` → FAIL, `Unresolved reference 'getSky'`.

- [ ] **Step 3: Implementace** — `RitualRepository.kt` za `getPathProgress()`:

```kotlin
    /** The night sky: practice days, the entries still in the history, and the rhythm per week. */
    fun getSky(): Flow<SkyData>
```

`RitualRepositoryImpl.kt` — sdílený tok dnů a nový tok oblohy (a `getPathProgress` přepsat na sdílený tok, ať se dny nečtou dvakrát jinak):

```kotlin
    private val practiceDaysFlow: Flow<Set<LocalDate>> = flow {
        logMigrator.ensureMigrated()
        emitAll(practiceDayDao.observeAll().map { dates -> dates.mapNotNull(::parsePracticeDay).toSet() })
    }

    override fun getPathProgress(): Flow<PathProgress> =
        practiceDaysFlow.combine(preferences.includeWeekendsFlow) { days, includeWeekends ->
            PathPolicy.progress(days = days, includeWeekends = includeWeekends, today = LocalDate.now())
        }

    override fun getSky(): Flow<SkyData> =
        combine(practiceDaysFlow, logsFlow, preferences.includeWeekendsFlow) { days, logs, includeWeekends ->
            val today = LocalDate.now()
            SkyData(
                days = days,
                logs = logs.mapNotNull { entry -> parsePracticeDay(entry.date)?.let { it to entry } }.toMap(),
                rhythmWeeks = PathRhythm.evaluate(days, includeWeekends, today).weeks,
                today = today
            )
        }
```

Importy: `org.clockout.domain.model.PathRhythm`, `org.clockout.domain.model.SkyData`. `logsFlow` už `ensureMigrated()` volá — dvojí volání je bezpečné (mutex + příznak).

- [ ] **Step 4: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*PracticeDayPersistenceTest*" --tests "*Path*"` → PASS.

- [ ] **Step 5: Commit (po souhlasu)** — `feat: repozitář skládá data noční oblohy`

---

### Task 6: Obrazovka Oblohy — `SkyCanvas`, `SkyScreen`, `SkyActivity`

**Files:**
- Create: `app/src/main/java/org/clockout/presentation/sky/SkyColors.kt`
- Create: `app/src/main/java/org/clockout/presentation/sky/SkyCanvas.kt`
- Create: `app/src/main/java/org/clockout/presentation/sky/SkyScreen.kt`
- Create: `app/src/main/java/org/clockout/SkyActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `values/strings.xml`, `values-cs/strings.xml`
- Modify: `app/src/test/java/org/clockout/screenshot/FormFactorFixtures.kt`, `FormFactorScreenshotTest.kt`
- Test: `app/src/test/java/org/clockout/SkyScreenTest.kt`

**Interfaces:**
- Consumes: `SkyData`, `SkyMonth`, `SkyLayout`, `ConstellationCatalog` (Tasks 2–4), `getSky()` (Task 5), `RitualLogEntry.localizedQuestionText/localizedAnswer`, `MoodTag.labelRes`
- Produces:
  - `@Composable fun SkyCanvas(month: SkyMonth, modifier: Modifier = Modifier, twinkle: Boolean = true, onStarTap: ((LocalDate) -> Unit)? = null)`
  - `@Composable fun SkyScreen(data: SkyData, language: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable internal fun SkyDayCard(date: LocalDate, entry: RitualLogEntry?, language: AppLanguage)`
  - `@Composable internal fun ConstellationFigure(constellation: Constellation, modifier: Modifier, isLit: Boolean)` (atlas, náhled)
  - `@Composable fun skySummaryText(summary: SkySummary, withYear: Boolean): String`
  - `class SkyActivity`; test tagy `sky_canvas`, `sky_month_title`, `sky_prev_month`, `sky_next_month`, `sky_day_card`, `sky_atlas_tile_<n>`, `sky_empty`

- [ ] **Step 1: Texty** — do obou `strings.xml`:

```xml
    <!-- EN -->
    <string name="sky_screen_title">The Night Sky</string>
    <plurals name="sky_summary_stars">
        <item quantity="one">%d star</item>
        <item quantity="other">%d stars</item>
    </plurals>
    <plurals name="sky_summary_constellations">
        <item quantity="one">%d constellation</item>
        <item quantity="other">%d constellations</item>
    </plurals>
    <string name="sky_summary_this_year">this year %1$d of %2$d</string>
    <string name="sky_previous_month">Previous month</string>
    <string name="sky_next_month">Next month</string>
    <string name="sky_month_empty">No stars this month yet</string>
    <string name="sky_empty">The first star lights up after your first closed day.</string>
    <string name="sky_record_deleted">The entry was deleted; the star stays.</string>
    <string name="sky_atlas_title">Atlas of constellations</string>
    <string name="sky_atlas_times">%d×</string>
    <string name="sky_atlas_rises">rises in week %d</string>
    <string name="sky_atlas_rare">only in years with a 53rd week</string>
    <plurals name="sky_canvas_description">
        <item quantity="one">%2$s: %1$d star</item>
        <item quantity="other">%2$s: %1$d stars</item>
    </plurals>
    <string name="sky_canvas_constellations">constellations %s</string>
```

```xml
    <!-- CS -->
    <string name="sky_screen_title">Noční obloha</string>
    <plurals name="sky_summary_stars">
        <item quantity="one">%d hvězda</item>
        <item quantity="few">%d hvězdy</item>
        <item quantity="many">%d hvězdy</item>
        <item quantity="other">%d hvězd</item>
    </plurals>
    <plurals name="sky_summary_constellations">
        <item quantity="one">%d souhvězdí</item>
        <item quantity="few">%d souhvězdí</item>
        <item quantity="many">%d souhvězdí</item>
        <item quantity="other">%d souhvězdí</item>
    </plurals>
    <string name="sky_summary_this_year">letos %1$d z %2$d</string>
    <string name="sky_previous_month">Předchozí měsíc</string>
    <string name="sky_next_month">Další měsíc</string>
    <string name="sky_month_empty">Tento měsíc zatím bez hvězd</string>
    <string name="sky_empty">První hvězda se rozsvítí po prvním uzavřeném dni.</string>
    <string name="sky_record_deleted">Záznam byl smazán, hvězda zůstává.</string>
    <string name="sky_atlas_title">Atlas souhvězdí</string>
    <string name="sky_atlas_times">%d×</string>
    <string name="sky_atlas_rises">vychází v týdnu %d</string>
    <string name="sky_atlas_rare">jen v letech s 53. týdnem</string>
    <plurals name="sky_canvas_description">
        <item quantity="one">%2$s: %1$d hvězda</item>
        <item quantity="few">%2$s: %1$d hvězdy</item>
        <item quantity="many">%2$s: %1$d hvězdy</item>
        <item quantity="other">%2$s: %1$d hvězd</item>
    </plurals>
    <string name="sky_canvas_constellations">souhvězdí %s</string>
```

- [ ] **Step 2: Failing test** — `SkyScreenTest.kt`:

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
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.presentation.sky.SkyDayCard
import org.clockout.presentation.sky.SkyScreen
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SkyScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 9)
    private val sky = SkyData(
        days = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)),
        logs = emptyMap(),
        rhythmWeeks = mapOf(LocalDate.of(2026, 10, 5) to true),
        today = today
    )

    private fun show(data: SkyData = sky) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { SkyScreen(data = data, language = AppLanguage.CZECH, onBack = {}) }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun `the screen opens on the current month with the summary`() {
        show()
        composeTestRule.onNodeWithText("Noční obloha").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 hvězdy", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Říjen 2026").assertIsDisplayed()
    }

    @Test
    fun `the arrows move between months`() {
        show()
        composeTestRule.onNodeWithTag("sky_prev_month").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Září 2026").assertIsDisplayed()
    }

    @Test
    fun `the atlas names a lit constellation and when an unlit one rises`() {
        show()
        composeTestRule.onNodeWithTag("sky_atlas_tile_41").performScrollTo()
        composeTestRule.onNodeWithText("Pletení").assertIsDisplayed()
        composeTestRule.onNodeWithText("1×").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sky_atlas_tile_42").performScrollTo()
        composeTestRule.onNodeWithText("vychází v týdnu 42").assertIsDisplayed()
    }

    @Test
    fun `without any day the sky explains where the first star comes from`() {
        show(SkyData.EMPTY)
        composeTestRule.onNodeWithTag("sky_empty").assertIsDisplayed()
    }

    @Test
    fun `a star with an entry shows its mood and answer`() {
        val date = LocalDate.of(2026, 10, 5)
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    SkyDayCard(
                        date = date,
                        entry = RitualLogEntry(date.toString(), "Co se dnes povedlo?", "Dopsal(a) jsem report", MoodTag.CALM, 0L),
                        language = AppLanguage.CZECH
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Klidný", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Dopsal(a) jsem report").assertIsDisplayed()
    }

    @Test
    fun `a star without an entry says the entry was deleted`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme { SkyDayCard(date = LocalDate.of(2026, 10, 5), entry = null, language = AppLanguage.CZECH) }
            }
        }
        composeTestRule.onNodeWithText("Záznam byl smazán, hvězda zůstává.").assertIsDisplayed()
    }
}
```

(Týden 41 roku 2026 začíná 5. 10. → jméno „Pletení"; týden 42 „Padající list" je nerozsvícený.)

- [ ] **Step 3: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*SkyScreenTest*"` → FAIL, `Unresolved reference 'SkyScreen'`.

- [ ] **Step 4: Barvy** — `SkyColors.kt`:

```kotlin
package org.clockout.presentation.sky

import androidx.compose.ui.graphics.Color
import org.clockout.domain.model.MoodTag

internal val SkyBackgroundTop = Color(0xFF070818)
internal val SkyBackgroundBottom = Color(0xFF1A1740)
internal val SkyLine = Color(0x8CA5B4FC)
internal val SkyNeutralStar = Color(0xFFF8FAFC)
internal val SkyFieldStar = Color(0x99CBD5E1)

/** A star keeps the colour of its mood for as long as the history still holds the entry. */
internal fun MoodTag?.starColor(): Color = when (this) {
    MoodTag.CALM -> Color(0xFF93C5FD)
    MoodTag.SATISFIED -> Color(0xFFFDE68A)
    MoodTag.TIRED -> Color(0xFFC4B5FD)
    MoodTag.TENSE -> Color(0xFFFDA4AF)
    null -> SkyNeutralStar
}
```

- [ ] **Step 5: `SkyCanvas`** — `SkyCanvas.kt`:

```kotlin
package org.clockout.presentation.sky

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.clockout.R
import org.clockout.domain.model.ConstellationCatalog
import org.clockout.domain.model.SkyLayout
import org.clockout.domain.model.SkyMonth
import org.clockout.domain.model.SkyPoint
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

/**
 * One month of sky. Everything drawn is a function of [month] and the twinkle clock, so a capture
 * with the clock advanced by hand is the same on every machine.
 */
@Composable
fun SkyCanvas(
    month: SkyMonth,
    modifier: Modifier = Modifier,
    twinkle: Boolean = true,
    onStarTap: ((LocalDate) -> Unit)? = null
) {
    val names = stringArrayResource(R.array.constellation_names)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = SkyLine.copy(alpha = 0.9f), fontSize = 11.sp)
    val time by if (twinkle) {
        rememberInfiniteTransition(label = "twinkle").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
            label = "twinkle"
        )
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val slots = remember(month) {
        SkyLayout.slots(month.yearMonth.year, month.weeks.map { it.constellationNumber })
    }
    val placed = remember(month, slots) {
        month.weeks.zip(slots).flatMap { (week, slot) ->
            val figure = ConstellationCatalog.forWeek(week.constellationNumber)
            week.stars.map { star -> star to SkyLayout.project(slot, figure.stars[star.pointIndex]) }
        }
    }
    val field = remember(month) { SkyLayout.fieldStars(month.yearMonth.year, month.yearMonth.monthValue) }
    val tapReach = with(LocalDensity.current) { 24.dp.toPx() }
    val description = skyDescription(month, names)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SkyLayout.ASPECT)
            .testTag("sky_canvas")
            .semantics { contentDescription = description }
            .then(
                if (onStarTap == null) Modifier else Modifier.pointerInput(placed) {
                    detectTapGestures { offset ->
                        val tap = SkyPoint(offset.x / size.width, offset.y / size.height)
                        SkyLayout.nearest(
                            placed.map { (star, p) -> star.date to p }, tap, tapReach / size.width
                        )?.let(onStarTap)
                    }
                }
            )
    ) {
        drawRect(Brush.verticalGradient(listOf(SkyBackgroundTop, SkyBackgroundBottom)))
        fun at(p: SkyPoint) = Offset(p.x * size.width, p.y * size.height)
        fun twinkleAlpha(phase: Float) = 0.7f + 0.3f * sin(2 * PI * (time + phase)).toFloat()

        field.forEach { s ->
            drawCircle(SkyFieldStar.copy(alpha = SkyFieldStar.alpha * twinkleAlpha(s.phase)),
                radius = s.radius * 1.2.dp.toPx(), center = at(SkyPoint(s.x, s.y)))
        }
        month.weeks.zip(slots).forEach { (week, slot) ->
            if (!week.isInRhythm) return@forEach
            val figure = ConstellationCatalog.forWeek(week.constellationNumber)
            val lit = week.stars.map { it.pointIndex }.toSet()
            figure.edges.filter { (a, b) -> a in lit && b in lit }.forEach { (a, b) ->
                drawLine(SkyLine, at(SkyLayout.project(slot, figure.stars[a])),
                    at(SkyLayout.project(slot, figure.stars[b])), strokeWidth = 1.2.dp.toPx())
            }
            val label = measurer.measure(names[week.constellationNumber - 1], labelStyle)
            val bottom = at(SkyPoint(slot.centerX, slot.centerY + slot.size * SkyLayout.ASPECT * 0.55f))
            drawText(label, topLeft = Offset(bottom.x - label.size.width / 2f, bottom.y))
        }
        placed.forEach { (star, p) ->
            val phase = (star.date.toEpochDay() % 97) / 97f
            val color = star.mood.starColor()
            drawCircle(color.copy(alpha = 0.25f * twinkleAlpha(phase)), radius = 7.dp.toPx(), center = at(p))
            drawCircle(color, radius = 3.dp.toPx(), center = at(p))
        }
    }
}
```

`skyDescription(month, names)` (private `@Composable` ve stejném souboru): název měsíce (viz `monthTitle` v kroku 6), `pluralStringResource(R.plurals.sky_canvas_description, starCount, starCount, monthTitle)`, a pokud jsou rozsvícená souhvězdí, připojit `", " + stringResource(R.string.sky_canvas_constellations, jména.joinToString(", "))`.

Pozn. k tvaru `time`: oba branche musí vracet `State<Float>`; pokud `by` s `if` nekompiluje, rozepiš na `val timeState: State<Float> = if (...) ... else remember { mutableFloatStateOf(0f) }` a `val time = timeState.value` uvnitř kreslení.

- [ ] **Step 6: `SkyScreen`, `SkyDayCard`, atlas** — `SkyScreen.kt`. Struktura (vzor horní lišty, pozadí a odsazení převzít z `presentation/path/PathScreen.kt`):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyScreen(data: SkyData, language: AppLanguage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    FrostedBackgroundContainer(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().maxContentWidth(ContentMaxWidth).align(Alignment.TopCenter)
                .statusBarsPadding().navigationBarsPadding()
        ) {
            SkyTopBar(onBack)   // jako v PathScreen; titulek R.string.sky_screen_title, tag "sky_back_button"
            val months = data.months
            if (months.isEmpty()) {
                Text(stringResource(R.string.sky_empty), Modifier.padding(32.dp).testTag("sky_empty"),
                    color = TextSlate, textAlign = TextAlign.Center)
                return@Column
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(skySummaryText(data.summary, withYear = true), color = TextSlate,
                    style = MaterialTheme.typography.bodyMedium)
                val pager = rememberPagerState(initialPage = months.lastIndex) { months.size }
                var selected by rememberSaveable { mutableStateOf<String?>(null) }  // ISO datum
                LaunchedEffect(pager.currentPage) { selected = null }
                val scope = rememberCoroutineScope()
                MonthHeader(
                    title = monthTitle(months[pager.currentPage]),
                    canGoBack = pager.currentPage > 0,
                    canGoForward = pager.currentPage < months.lastIndex,
                    onBack = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    onForward = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                )
                HorizontalPager(state = pager, pageSpacing = 16.dp) { page ->
                    val month = remember(data, page) { data.month(months[page]) }
                    Box(Modifier.clip(RoundedCornerShape(24.dp))) {
                        SkyCanvas(month, onStarTap = { date ->
                            selected = date.toString().takeIf { it != selected }
                        })
                        if (month.starCount == 0) {
                            Text(stringResource(R.string.sky_month_empty), Modifier.align(Alignment.Center),
                                color = TextMuted)
                        }
                    }
                }
                selected?.let { iso ->
                    val date = LocalDate.parse(iso)
                    SkyDayCard(date, data.logs[date], language)
                }
                Atlas(data.summary, data.today)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
```

- `MonthHeader`: `Row` s `IconButton` (`Icons.AutoMirrored.Filled.KeyboardArrowLeft`/`Right`, `contentDescription` `sky_previous_month`/`sky_next_month`, tagy `sky_prev_month`/`sky_next_month`, `enabled` podle `canGo…`) a uprostřed `Text(title, Modifier.testTag("sky_month_title"))`.
- `monthTitle(yearMonth)`: `val locale = LocalResources.current.configuration.locales[0]`; `yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + yearMonth.year` (čeština: „Říjen 2026").
- `SkyDayCard(date, entry, language)` (`internal`, tag `sky_day_card`, frosted karta): datum `DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)`; s `entry`: `"${entry.moodTag.emoji} ${stringResource(entry.moodTag.labelRes)}"`, `entry.localizedQuestionText(language)`, `entry.localizedAnswer(language)` (pokud není null); bez `entry`: `stringResource(R.string.sky_record_deleted)`.
- `Atlas(summary, today)`: nadpis `sky_atlas_title`; `BoxWithConstraints` → sloupce `if (maxWidth >= 600.dp) 5 else 3`; `ConstellationCatalog.all.chunked(columns)` → `Row` s `weight(1f)` dlaždicemi, poslední řádek doplnit prázdnými `Spacer(Modifier.weight(1f))` (stejně jako `BreathingStyleScreen`). Dlaždice (tag `sky_atlas_tile_<n>`): `ConstellationFigure(c, Modifier.fillMaxWidth().aspectRatio(1f), isLit = times > 0)`, jméno ze `stringArrayResource(R.array.constellation_names)[n-1]`, pod ním `stringResource(R.string.sky_atlas_times, times)` pokud `times > 0`, jinak pro `n == 53` `sky_atlas_rare`, jinak `stringResource(R.string.sky_atlas_rises, n)`.
- `ConstellationFigure(constellation, modifier, isLit)` (`internal`, ve `SkyCanvas.kt`): `Canvas` — všech 7 bodů a všechny hrany, body v souřadnicích `x * width`, `y * height` s 12% okrajem; `isLit` → `SkyLine`/`SkyNeutralStar`, jinak alfa 0.25.
- `skySummaryText(summary, withYear)` (veřejná, `@Composable`): `pluralStringResource(sky_summary_stars, …) + " · " + pluralStringResource(sky_summary_constellations, constellationsLit, …)`, s `withYear` navíc `" · " + stringResource(sky_summary_this_year, litThisYear, weeksThisYear)`.

- [ ] **Step 7: `SkyActivity` a manifest** — kopie `PathActivity` se změnami: KDoc („The night sky, reached from the preview card in History."), `val sky by repository.getSky().collectAsStateWithLifecycle(initialValue = null)`; obsah `AppLocalization(language) { sky?.let { SkyScreen(data = it, language = language, onBack = { finish() }) } }` (null = ještě nenačteno, nic nekreslit — ne prázdný stav). Manifest za `PathActivity`:

```xml
        <!-- The night sky. Its own Activity for the same reasons as the path. -->
        <activity
            android:name=".SkyActivity"
            android:exported="false"
            android:label="@string/sky_screen_title"
            android:parentActivityName=".MainActivity"
            android:theme="@style/Theme.MyApplication" />
```

- [ ] **Step 8: Screenshot** — `FormFactorFixtures.kt`: `val sky = SkyData(days = (pracovní dny 2026-09-28 … 2026-10-09 mínus 2026-10-07), logs = pár záznamů s různými náladami, rhythmWeeks = mapOf(LocalDate.of(2026, 9, 28) to true, LocalDate.of(2026, 10, 5) to true), today = LocalDate.of(2026, 10, 9))` — doplň konkrétní seznam dat. `FormFactorScreenshotTest.kt`: `@Test fun sky() = capture("sky") { SkyScreen(data = Fixtures.sky, language = AppLanguage.ENGLISH, onBack = {}) }` (import `org.clockout.presentation.sky.SkyScreen`).

- [ ] **Step 9: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*SkyScreenTest*"` → PASS (6 testů); `./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"` a prohlédnout `app/src/test/screenshots/formfactor/*/sky.png` (souhvězdí čitelná, jména pod figurami, nic nepřetéká); `./gradlew :app:assembleDebug`.

- [ ] **Step 10: Commit (po souhlasu)** — `feat: obrazovka Noční oblohy s atlasem souhvězdí`

---

### Task 7: Náhled oblohy v Historii

**Files:**
- Create: `app/src/main/java/org/clockout/presentation/sky/SkyPreviewCard.kt`
- Modify: `app/src/main/java/org/clockout/presentation/history/HistoryState.kt`, `HistoryViewModel.kt`, `HistoryScreen.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt` (volání `HistoryScreen(` kolem ř. 275)
- Test: `app/src/test/java/org/clockout/HistorySkyPreviewTest.kt`

**Interfaces:**
- Consumes: `getSky()` (Task 5), `SkyCanvas`, `skySummaryText` (Task 6), `SkyActivity` (Task 6)
- Produces: `HistoryState.sky: SkyData? = null`; `HistoryScreen(…, onOpenSky: () -> Unit = {}, …)`; `@Composable fun SkyPreviewCard(data: SkyData, onOpen: () -> Unit)` s tagem `sky_preview_card`

- [ ] **Step 1: Failing test** — `HistorySkyPreviewTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.RitualLogEntry
import org.clockout.domain.model.SkyData
import org.clockout.presentation.history.HistoryScreen
import org.clockout.presentation.history.HistoryState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HistorySkyPreviewTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val day = LocalDate.of(2026, 10, 5)
    private val sky = SkyData(setOf(day), emptyMap(), mapOf(day to false), LocalDate.of(2026, 10, 9))

    private fun show(state: HistoryState, onOpenSky: () -> Unit = {}) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HistoryScreen(state = state, onBack = {}, onClearHistory = {}, onOpenSky = onOpenSky)
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun `the sky preview shows above an empty history`() {
        show(HistoryState(sky = sky))
        composeTestRule.onNodeWithTag("sky_preview_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 hvězda", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the sky preview sits under the mood summary of a full history`() {
        val entry = RitualLogEntry(day.toString(), "Q", null, MoodTag.CALM, 1L)
        show(HistoryState(logs = listOf(entry), totalCount = 1, moodCounts = mapOf(MoodTag.CALM to 1), sky = sky))
        composeTestRule.onNodeWithTag("sky_preview_card").assertIsDisplayed()
    }

    @Test
    fun `without any day there is no preview`() {
        show(HistoryState(sky = SkyData.EMPTY))
        composeTestRule.onNodeWithTag("sky_preview_card").assertDoesNotExist()
    }

    @Test
    fun `tapping the preview opens the sky`() {
        var opened = false
        show(HistoryState(sky = sky), onOpenSky = { opened = true })
        composeTestRule.onNodeWithTag("sky_preview_card").performClick()
        assertTrue(opened)
    }
}
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*HistorySkyPreviewTest*"` → FAIL, `No parameter with name 'sky'`.

- [ ] **Step 3: Stav a ViewModel** — `HistoryState`: `val sky: SkyData? = null`. `HistoryViewModel.loadHistory()`: druhý `launch` s `repository.getSky().collect { sky -> _state.update { it.copy(sky = sky) } }`.

- [ ] **Step 4: Karta** — `SkyPreviewCard.kt`:

```kotlin
@Composable
fun SkyPreviewCard(data: SkyData, onOpen: () -> Unit) {
    val current = data.months.lastOrNull() ?: return
    val month = remember(data) { data.month(current) }
    Column(
        Modifier.fillMaxWidth().frostedGlass(cornerRadius = 22.dp).clip(RoundedCornerShape(22.dp))
            .clickable(onClickLabel = stringResource(R.string.sky_screen_title), role = Role.Button, onClick = onOpen)
            .padding(16.dp).testTag("sky_preview_card")
    ) {
        Text(stringResource(R.string.sky_screen_title), style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold), color = TextLight)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
            SkyCanvas(month = month, twinkle = false)
        }
        Spacer(Modifier.height(10.dp))
        Text(skySummaryText(data.summary, withYear = false), style = MaterialTheme.typography.bodySmall, color = TextSlate)
    }
}
```

(Výška: celé plátno 3 : 4 je v Historii příliš vysoké — omez `Box` na `Modifier.heightIn(max = 220.dp)` a `SkyCanvas` vlož s `Modifier.fillMaxWidth()`; ořez horní části oblohy je u náhledu v pořádku. Pokud ořez uřízne všechna souhvězdí, použij místo ořezu `Modifier.width(165.dp)` vycentrovaný.)

- [ ] **Step 5: `HistoryScreen`** — parametr `onOpenSky: () -> Unit = {}` za `onClearHistory`.
  - Větev `state.logs.isEmpty()`: obal prázdný stav do `Column(Modifier.fillMaxSize().padding(horizontal = 20.dp))`; nahoře `state.sky?.takeIf { it.days.isNotEmpty() }?.let { SkyPreviewCard(it, onOpenSky) }`, pod ním stávající `Box(...weight(1f)...)` s 🌙.
  - Větev s mřížkou: v prvním `item(span = …)` za souhrnnou kartu nálad a před `Spacer(4.dp)`: `state.sky?.takeIf { it.days.isNotEmpty() }?.let { Spacer(Modifier.height(14.dp)); SkyPreviewCard(it, onOpenSky) }`.
- [ ] **Step 6: `PadlaApp`** — import `org.clockout.SkyActivity`; do `HistoryScreen(...)`: `onOpenSky = { context.startActivity(Intent(context, SkyActivity::class.java)) },`.
- [ ] **Step 7: Screenshot** — `Fixtures.history` doplnit `sky = sky` (z Úkolu 6), aby baseline Historie obsahovala náhled.
- [ ] **Step 8: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*HistorySkyPreviewTest*" --tests "*History*"` → PASS.
- [ ] **Step 9: Commit (po souhlasu)** — `feat: náhled noční oblohy v historii`

---

### Task 8: Jméno souhvězdí na dokončovací kartě

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualState.kt`, `RitualViewModel.kt`, `RitualScreen.kt`
- Modify: `values/strings.xml`, `values-cs/strings.xml` (nahradit `ritual_path_rhythm_week`)
- Test: `app/src/test/java/org/clockout/RitualPathCelebrationTest.kt`, `RitualScreenLocalizationTest.kt`

**Interfaces:**
- Consumes: `SkyPolicy.constellationNumber` (Task 4), `R.array.constellation_names` (Task 2)
- Produces: `RitualState.rhythmConstellationNumber: Int? = null` (nahrazuje `rhythmWeekReached: Boolean`)

- [ ] **Step 1: Failing testy** — v `RitualPathCelebrationTest.kt` přejmenovat aserce `rhythmWeekReached` na číslo:

```kotlin
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
```

(Nahrazuje dva stávající testy `…marks the rhythm` / `…marks no rhythm`; import `org.clockout.domain.model.SkyPolicy`.) Do `RitualScreenLocalizationTest.kt`:

```kotlin
    @Test
    fun `the completion card names the constellation a full week lit`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            question = question, currentStep = RitualStep.Log, selectedMood = MoodTag.CALM,
                            isCompletedAndSaved = true, rhythmConstellationNumber = 10
                        ),
                        onIntent = {}, onFinish = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Tento týden jsi v rytmu. Na obloze se rozsvítilo souhvězdí: Konvice.")
            .performScrollTo().assertIsDisplayed()
    }
```

- [ ] **Step 2: Ověřit pád** — `./gradlew :app:testDebugUnitTest --tests "*RitualPathCelebrationTest*" --tests "*RitualScreenLocalizationTest*"` → FAIL, `Unresolved reference 'rhythmConstellationNumber'`.

- [ ] **Step 3: Implementace**
  - `RitualState`: `rhythmWeekReached: Boolean` nahradit `/** This week's constellation when this ritual has just brought the week into rhythm. */ val rhythmConstellationNumber: Int? = null`.
  - `RitualViewModel.handleMoodSelected`: `rhythmConstellationNumber = SkyPolicy.constellationNumber(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).takeIf { pathAfter.isCurrentWeekInRhythm && !pathBefore.isCurrentWeekInRhythm }`.
  - `RitualScreen`: parametr `StepLogContent` `rhythmWeekReached: Boolean` → `rhythmConstellationNumber: Int?`; větev `else if (rhythmConstellationNumber != null)` s textem `stringResource(R.string.ritual_path_rhythm_constellation, stringArrayResource(R.array.constellation_names)[rhythmConstellationNumber - 1])`, tag zůstává `ritual_path_rhythm_week`.
  - Texty: smazat `ritual_path_rhythm_week` z obou souborů, přidat `ritual_path_rhythm_constellation` — EN `You\'re in rhythm this week. A constellation lit up in the sky: %s.`, CS `Tento týden jsi v rytmu. Na obloze se rozsvítilo souhvězdí: %s.`
  - `grep -rn "rhythmWeekReached\|ritual_path_rhythm_week\"" app/src` → žádný výskyt kromě test tagu.

- [ ] **Step 4: Ověřit** — `./gradlew :app:testDebugUnitTest --tests "*Ritual*"` → PASS.
- [ ] **Step 5: Commit (po souhlasu)** — `feat: dokončovací karta jmenuje rozsvícené souhvězdí`

---

### Task 9: Dokumentace, celková kontrola, emulátor

**Files:**
- Modify: `CLAUDE.md`, `AGENTS.md`

- [ ] **Step 1: Dokumentace (oba soubory shodně)**
  - *Module layout*: `presentation/{home,history,ritual,settings,path,sky}`.
  - *Architecture*: odstavec o Noční obloze — `SkyPolicy`/`SkyData`/`SkyLayout`/`ConstellationCatalog` v doméně, týden patří měsíci čtvrtku, souhvězdí = ISO týden (53 položek), rytmus výhradně z `PathRhythm.Result.weeks`, barvy nálad jen dokud existuje záznam, `SkyActivity` z náhledu v Historii, náhled i v prázdné Historii, determinismus přes `java.util.Random(seed)`; dokončovací karta jmenuje souhvězdí.
  - *Testing*: nové testy, screenshot `sky` a Historie s náhledem.
  - *What NOT to do*: nepočítat rytmus oblohy jinde než v `PathRhythm`; neměnit pořadí ani tvar vydaných souhvězdí; nepřidávat do oblohy běhovou náhodnost; nepřidávat k figurám posun ve slotu bez úpravy testu nepřekrývání.
- [ ] **Step 2: Celková kontrola**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"
./gradlew :app:verifyRoborazziDebug
./gradlew :app:assembleDebug
```

Expected: vše zelené; nové a změněné snímky (`sky`, `history`, navigační) prohlédnout.

- [ ] **Step 3: Emulátor** — je-li `adb devices` neprázdné: zazálohovat data aplikace (`adb exec-out run-as org.clockout tar cf - .`), nainstalovat debug APK přes stávající instalaci, otevřít Historii → náhled → Obloha, listovat měsíci, klepnout na hvězdu, projít atlas, přepnout do češtiny, smazat historii a ověřit, že obloha zůstala (bílé hvězdy); na konci data obnovit (`run-as org.clockout tar xf -`).
- [ ] **Step 4: Commit (po souhlasu)** — `docs: dokumentace Noční oblohy`
