# Cesta — stupně a týdenní rytmus (MVP gamifikace)

Datum: 2026-10-09 · Stav: návrh ke schválení

## Záměr

Dát Padla pocit postupu a osobní tváře bez výkonového tlaku, který by šel proti smyslu aplikace (uzavřít práci, ne
plnit další metriku). Uživatel prochází deseti stupni zenové **Cesty** podle *Deseti obrazů pasení býka* (十牛図)
a sleduje svůj **týdenní rytmus**.

Tři zásady, kterými se řídí každé rozhodnutí níže:

1. **Nic se nedá ztratit.** Stupeň jen roste; nejdelší rytmus se nikdy nesníží.
2. **Počítají se jen nabízené dny.** Víkend s vypnutými víkendy, dovolená ani nemoc nejsou selhání.
3. **Odměňuje se pravidelnost, ne nálada.** Nálada do stupňů ani do rytmu nevstupuje.

### Mimo rozsah

- Účty, přihlášení, cloud. Aplikace zůstává bez účtů — tak jak slibuje popis.
- Noční obloha (varianta A) — samostatná specifikace jako další krok; tento návrh pro ni připravuje datový základ.
- Odznaky, telemetrie stupňů, možnost postup ručně vynulovat.

## Data

### Nová tabulka `practice_days`

```kotlin
@Entity(tableName = "practice_days")
data class PracticeDayEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: String // "YYYY-MM-DD", stejný formát jako ritual_logs
)
```

- Drží **jen datum** uzavřeného dne. Žádná nálada, otázka ani odpověď — „Smazat data a historii" má odstranit vše
  osobní a tahle tabulka po něm zůstává.
- `PracticeDayDao`: `observeAll(): Flow<List<String>>`, `insert(entity)` s `OnConflictStrategy.IGNORE`,
  `insertAll(list)` se stejnou strategií.
- `ClockoutDatabase` → `version = 2`, `entities` rozšířeny. Ruční `Migration(1, 2)`:
  ```sql
  CREATE TABLE IF NOT EXISTS practice_days (date TEXT NOT NULL, PRIMARY KEY(date));
  INSERT OR IGNORE INTO practice_days(date) SELECT date FROM ritual_logs;
  ```
  Stávající uživatelé tak dostanou svůj stupeň zpětně. Vyexportované schéma `2.json` jde do `app/schemas/`.
- `RitualLogMigrator` (legacy JSON blob → Room) běží až po migraci databáze, takže musí legacy data vložit
  **i do `practice_days`**, jinak by uživatel z nejstarších verzí o tyto dny přišel.

### Zápis a mazání

- `RitualRepositoryImpl.logRitual` zapíše den do `ritual_logs` (upsert, jako dnes) **i** do `practice_days`
  (insert-or-ignore). Dva rituály v jeden den = jeden den cesty.
- `clearLogs()` se **nemění** — maže jen `ritual_logs`. `practice_days` přežije „Smazat data a historii" v Nastavení
  i „Smazat historii" v Historii.
- Potvrzovací texty obou dialogů (`history_clear_dialog_text`, `settings_clear_data_dialog_text`) doplníme o větu,
  že dosažený stupeň a rytmus zůstanou zachovány — aby to uživatele nepřekvapilo.

### Zálohy

`practice_days` je v `clockout.db`, takže jede v cloudové záloze i při přenosu zařízení stejně jako logy.
Pravidla záloh se nemění; `BackupRulesTest` zůstává beze změny.

## Doména (`domain/model`, čistý Kotlin, `java.time`)

### `PathStage`

```kotlin
enum class PathStage(val id: String, val minDays: Int) {
    BEGINNING("BEGINNING", 0),            // nováček, výchozí znak
    SEEKING("SEEKING", 1),
    TRACKS("TRACKS", 5),
    GLIMPSING("GLIMPSING", 10),
    CATCHING("CATCHING", 20),
    TAMING("TAMING", 35),
    RIDING_HOME("RIDING_HOME", 50),
    OX_FORGOTTEN("OX_FORGOTTEN", 75),
    EMPTY_CIRCLE("EMPTY_CIRCLE", 100),
    SOURCE("SOURCE", 150),
    MARKETPLACE("MARKETPLACE", 250);
}
```

Bez řetězců a bez `R`. `PathStage.forDays(days)` vrací nejvyšší stupeň s `minDays <= days`; `next` vrací následující
stupeň nebo `null` za posledním.

| # | Dní | Česky | English | Znak |
|---|---|---|---|---|
| 0 | 0 | Na začátku cesty | At the Start of the Path | prázdný štětcový tah |
| 1 | 1 | Hledání býka | Seeking the Ox | cesta |
| 2 | 5 | Nalezení stop | Finding the Tracks | stopy kopyt |
| 3 | 10 | Spatření býka | Glimpsing the Ox | ocas za keřem |
| 4 | 20 | Chycení býka | Catching the Ox | provaz |
| 5 | 35 | Krocení býka | Taming the Ox | býk s provazem |
| 6 | 50 | Návrat domů na hřbetě býka | Riding the Ox Home | flétna |
| 7 | 75 | Býk zapomenut | The Ox Forgotten | měsíc |
| 8 | 100 | Prázdný kruh | The Empty Circle | ensō |
| 9 | 150 | Návrat ke zdroji | Returning to the Source | strom u řeky |
| 10 | 250 | S otevřenýma rukama do světa | Entering the Market with Open Hands | otevřené dlaně |

### Týdenní rytmus — `PathRhythm`

Vstup: množina dnů z `practice_days` (`Set<LocalDate>`), `includeWeekends`, `today: LocalDate`.

- Týden začíná pondělím (stejně jako `calculateStreakInfo`).
- **Nabízené dny týdne**: Po–Pá, nebo Po–Ne se zapnutými víkendy. Uzavřené dny mimo nabízené (víkend při
  vypnutých víkendech) se do rytmu nepočítají.
- **Týden prvního uzavřeného dne** nabízí jen dny od tohoto dne dál — kdo začne ve středu, není potrestán za
  pondělí a úterý, kdy aplikaci ještě neměl.
- **Práh týdne** = `max(1, nabízené − 1)` → 4 z 5, 6 ze 7.
- **Aktuální týden** se započítá, jakmile dosáhne prahu; dokud ne, rytmus neláme ani neprodlužuje.
- **Ukončené týdny** se procházejí od nejnovějšího: splněný týden přičte 1 a vynuluje počítadlo chyb, nesplněný
  přičte 1 k chybám; **dva nesplněné týdny po sobě rytmus ukončí** („nikdy nevynechej dvakrát"). Procházení končí
  nejpozději týdnem prvního uzavřeného dne.
- **Nejdelší rytmus**: stejné pravidlo aplikované na celou historii, maximum ze všech sérií.
- Mění-li se nastavení víkendů, rytmus se přepočítá podle aktuálního nastavení (záměrně jednoduché).

Výstup:

```kotlin
data class PathProgress(
    val closedDays: Int,
    val stage: PathStage,
    val nextStage: PathStage?,
    val daysToNextStage: Int?,          // null na posledním stupni
    val rhythmWeeks: Int,
    val longestRhythmWeeks: Int,
    val isCurrentWeekInRhythm: Boolean
)
```

`PathPolicy.progress(days, includeWeekends, today)` je čistá funkce; `RitualRepository.getPathProgress(): Flow<PathProgress>`
ji kombinuje z `practiceDayDao.observeAll()` a `preferences.includeWeekendsFlow`. Dnešní datum se čte při každé
emisi.

## UI

### Home — karta „Cesta"

Nová `PathCard` pod `WeeklyStreakCard` (v jednosloupcovém i dvousloupcovém rozložení — ve dvou sloupcích ve
stejném sloupci jako týdenní karta, nad `WhyRitualCard`). Frosted glass jako ostatní karty.

- Znak aktuálního stupně, název stupně a postup: „Ještě 3 dny do *Spatření býka*" (plurál) + tenký progress bar
  mezi prahy. Na posledním stupni místo toho „Cesta je u konce — a pokračuje dál".
- Řádek rytmu: „Rytmus: 3 týdny · nejdelší 7" (plurály). Při nulovém rytmu jen „nejdelší N", případně nic, pokud
  rytmus ještě nevznikl.
- Nováček (`BEGINNING`): výchozí znak a „Cesta začíná prvním uzavřeným dnem".
- Klepnutí otevře `PathActivity`.
- `HomeState` dostane `pathProgress: PathProgress? = null`; `HomeViewModel` ho plní z repozitáře.

### Dokončovací karta rituálu

`RitualViewModel.handleMoodSelected` přečte `PathProgress` **před** zápisem a **po** zápisu:

- `RitualState.reachedStage: PathStage?` — nastaví se, když se stupeň po zápisu liší.
- `RitualState.rhythmWeekReached: Boolean` — aktuální týden po zápisu nově splnil práh.

`StepLogContent` po uložení ukáže znak, název a krátký verš nového stupně; při splněném týdnu jemnou větu
„Tento týden jsi v rytmu". Jinak nic navíc. Stávající uživatelé po aktualizaci uvidí zpětně spočtený stupeň na
Home bez oslavy — to je záměr.

### `PathActivity` — obrazovka Cesty

Samostatná Activity podle vzoru `BreathingStyleActivity`: `applyFrostedWindowChrome()`, `rememberRitualRepository()`
volané nad `AppLocalization`, záznam v manifestu s `parentActivityName=".MainActivity"`. Vlastní `PathViewModel`
(nebo stav přímo z `getPathProgress()`).

Obsah, shora:

1. Hlavička se znakem a názvem aktuálního stupně, počtem uzavřených dní a rytmem.
2. **Odkud cesta pochází**: Deset obrazů pasení býka, čínský mistr Kuo-an Š'-jüan (jap. Kakuan), 12. století;
   býk jako obraz vlastní mysli; poslední obraz se vrací do běžného světa — proto se hodí k uzavírání pracovního dne.
3. **Deset stupňů**: řádek se znakem, prahem dní, názvem, veršem a krátkým výkladem. Dosažené stupně plnou barvou,
   aktuální zvýrazněný, nedosažené ztlumené, ale čitelné.
4. **Jak funguje rytmus**: práh týdne, tolerance jednoho slabšího týdne, víkendy, a že stupně i rytmus přežijí
   smazání historie.

Na širokém okně je obsah omezen maximální šířkou a vycentrován (`currentWindowAdaptiveInfo()`), seznam stupňů
může přejít do dvou sloupců.

Vstupy: karta na Home a nové tlačítko „Cesta k býkovi" v kartě „O aplikaci" v Nastavení.

### Znaky stupňů

11 vektorových drawables `ic_path_stage_0.xml` … `ic_path_stage_10.xml`, jednobarevné v „tušovém" stylu, kreslené
pro tónování přes `Icon(tint = …)`, takže fungují ve světlém i tmavém motivu. Mapování `PathStage → @DrawableRes`
a `PathStage → @StringRes` (název, verš, výklad) žije v `presentation/util/Localization.kt` vedle ostatních enumů —
statické `R.*` reference kvůli `isShrinkResources`. Soubory jsou připravené na pozdější výměnu za ilustrace.

### Texty

Všechny texty v `values/strings.xml` i `values-cs/strings.xml` se stejnou sadou klíčů:
`path_stage_<n>_title`, `path_stage_<n>_verse`, `path_stage_<n>_desc`, texty karty, obrazovky a dokončovací karty.
Počty dní a týdnů jako `plurals` (česky `one`/`few`/`many`/`other`). Verše a výklady jsou **vlastní texty** —
originál z 12. století je volné dílo, známé překlady (např. Reps & Senzaki) ne.

## Testy

- `PathStageTest` — hranice prahů (0, 1, 4, 5, 249, 250, 1000), `next`, `daysToNextStage`.
- `PathRhythmTest` — plné týdny; jeden nesplněný týden tolerován; dva po sobě ukončí; rozpracovaný aktuální týden
  neláme; částečný první týden; víkendy zapnuté/vypnuté; víkendové dny ignorovány při vypnutých víkendech;
  nejdelší rytmus; prázdná množina.
- `ClockoutDatabaseMigrationTest` — `MigrationTestHelper` 1 → 2 naplní `practice_days` z `ritual_logs`. Vyžaduje
  `androidx.room:room-testing` v katalogu verzí a `schemas/` jako assets i pro `test` source set (dnes jen
  `androidTest`).
- Repozitář — `logRitual` dvakrát v jeden den = 1 den; `clearLogs` nechá `practice_days` beze změny; legacy migrátor
  plní i `practice_days`.
- `RitualViewModel` — `reachedStage` při překročení prahu, `null` při opakovaném rituálu téhož dne;
  `rhythmWeekReached`.
- Home — `PathCard` pro nováčka, střední stupeň a poslední stupeň, v obou jazycích; nejdelší český název
  („S otevřenýma rukama do světa") se vejde — `@GraphicsMode(NATIVE)`, aserce na `lineCount`.
- `PathScreen` — lokalizace přes `AppLocalization`, screenshot ve stylu `FormFactorScreenshotTest` (telefon a tablet).
  Baseline jsou git-ignored, nahrají se lokálně.
- `PhoneNavigationTest` / `TabletNavigationTest` — baseline Home se změní (nová karta), re-record jako záměrná
  změna UI.
- `./gradlew :app:lintDebug` zelený (chybějící `many` u plurálů).

## Dokumentace

`CLAUDE.md` a `AGENTS.md` společně: Architecture (Cesta, rytmus, `PathActivity`), Persistence and backup
(`practice_days` a to, že přežije smazání), Testing (nové testy, změna Home baseline), What NOT to do:

- Nemazat `practice_days` v `clearLogs`.
- Neukládat do `practice_days` nic osobního (náladu, otázku, odpověď).
- Nepočítat rytmus z `ritual_logs` — ten smazání nepřežije.
