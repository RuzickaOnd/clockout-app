# Noční obloha — design

Datum: 2026-10-09 · Stav: návrh ke schválení · Navazuje na: `2026-10-09-cesta-gamifikace-design.md`

## Záměr

Dát uzavřeným dnům podobu, ke které se dá vracet. Cesta domů říká, *kam* uživatel došel; Noční obloha ukazuje,
*jak* tam šel. Příběhově navazuje: jsi doma, je večer a díváš se z okna na nebe, které sis den po dni poskládal(a).

Platí tytéž zásady jako u Cesty:

1. **Nic se nedá ztratit.** Hvězdy ani rozsvícená souhvězdí nezmizí; smazání historie vezme jen barvy nálad.
2. **Počítají se jen nabízené dny** — pravidlo rytmu je to z Cesty, žádná druhá logika.
3. **Odměňuje se pravidelnost, ne nálada.** Nálada jen barví hvězdu, nic nerozhoduje.

### Rozhodnutí z návrhu

| Otázka | Rozhodnutí |
|---|---|
| Kde obloha žije | Samostatná obrazovka `SkyActivity`, náhled v Historii |
| Práce s časem | Obloha po měsících, listuje se mezi nimi |
| Jména souhvězdí | Vlastní katalog, jedno souhvězdí na ISO týden v roce |
| Tvar souhvězdí | Pevná figura ze 7 hvězd (Po–Ne), hvězda = uzavřený den |
| Sbírka | Atlas všech souhvězdí na obrazovce Oblohy |
| Počet souhvězdí | 53 — 53. týden mají jen některé roky („Modrý měsíc") |

### Mimo rozsah

- Sdílení oblohy (obrázek, odkaz) a jakákoli síťová funkce.
- Ruční pojmenování souhvězdí uživatelem.
- Skutečná fáze Měsíce, skutečné hvězdné mapy.
- Změny na Home — obloha se tam neobjevuje.

## Pravidla

### Hvězda

- Každý den v `practice_days` je jedna hvězda. Neuzavřené dny se nekreslí — na obloze nejsou „díry".
- Hvězda stojí na bodu figury svého týdne podle dne v týdnu (Po = bod 0 … Ne = bod 6).
- **Barva** podle nálady ze `ritual_logs` pro stejné datum: `CALM` modrá, `SATISFIED` zlatá, `TIRED` levandulová,
  `TENSE` růžová. Bez záznamu (smazaná historie) bílá. Barvy žijí v `presentation`, doména nese jen `MoodTag?`.

### Souhvězdí týdne

- Týden je ISO týden (pondělí–neděle, `IsoFields.WEEK_OF_WEEK_BASED_YEAR`), souhvězdí = položka katalogu s tímto
  číslem (1–53).
- **Týden patří měsíci, ve kterém leží jeho čtvrtek** (stejné pravidlo jako ISO pro roky). Týden přes přelom měsíce
  se tedy nikdy nerozdělí a každý měsíc má 4 nebo 5 týdnů.
- **Týden v rytmu** (výsledek `PathRhythm` pro tento týden, včetně víkendového bonusu) propojí čarami ty hrany figury,
  jejichž **obě** hvězdy svítí, a ukáže jméno souhvězdí — ale jen pokud taková hrana existuje aspoň jedna. Týden
  v rytmu s jedinou hvězdou (první, částečný týden) souhvězdí nerozsvítí. Rozpracovaný aktuální týden se propojí,
  jakmile dosáhne prahu. Dokončovací karta hlásí souhvězdí u rituálu, který ho poprvé rozsvítí.
- Týden mimo rytmus ukáže jen své hvězdy, bez čar a bez jména.

### Měsíc na obloze

- Obloha má poměr stran 3 : 4. Pro 4 i 5 týdnů jsou předem dané sloty (střed a velikost); figura týdne se do slotu
  vepíše s mírným pootočením (±12°), odvozeným deterministicky z `(rok, týden)`, aby obloha nepůsobila jako
  mřížka. Posun by rozbil zaručené nepřekrývání slotů, proto se figury jen otáčejí.
- Doplňková hvězdná pole: 60 drobných hvězd pevně odvozených ze `(rok, měsíc)`. Vše je funkce dat — žádná
  náhodnost za běhu, takže screenshot test vidí vždy totéž.
- Jemné blikání hvězd je funkcí času (fáze odvozená z data hvězdy), stejně jako dech u `BreathingAurora`;
  se systémově vypnutými animacemi stojí.
- Rozsah listování: od měsíce prvního dne v `practice_days` po aktuální měsíc. Obrazovka se otevírá na aktuálním
  měsíci. Měsíc bez hvězd ukáže jen hvězdné pole a větu „Tento měsíc zatím bez hvězd".

### Souhrn a sbírka

- **Hvězdy celkem** = počet dnů v `practice_days`.
- **Rozsvícená souhvězdí celkem** = počet týdnů v rytmu za celou historii.
- **Letos** = počet *různých* čísel souhvězdí rozsvícených v aktuálním ISO roce / počet týdnů tohoto roku (52 nebo 53).
- **Atlas**: všech 53 souhvězdí. Rozsvícené aspoň jednou mají plnou figuru a počet rozsvícení („3×");
  ostatní tlumenou figuru a „vychází v týdnu 41". Souhvězdí 53 má popisek „jen v letech s 53. týdnem".

## UI

### `SkyActivity` — obrazovka Oblohy

Samostatná Activity podle vzoru `PathActivity` (`applyFrostedWindowChrome()`, `rememberRitualRepository()` nad
`AppLocalization`, záznam v manifestu s `parentActivityName=".MainActivity"`). Shora:

1. Horní lišta „Noční obloha" / „The Night Sky" s tlačítkem zpět.
2. Souhrn: „412 hvězd · 61 souhvězdí · letos 23 z 52" (plurály).
3. Obloha měsíce v `HorizontalPager`, nad ní název měsíce a roku se šipkami vlevo/vpravo.
4. **Karta dne** po klepnutí na hvězdu (zásah v okruhu 24 dp od nejbližší hvězdy): datum, emoji a název nálady,
   otázka a odpověď přes `RitualLogLocalization`. Bez záznamu: datum a „Záznam byl smazán, hvězda zůstává."
   Opětovné klepnutí nebo jiný měsíc kartu zavře.
5. Atlas souhvězdí: mřížka miniatur, na úzkém okně 3 sloupce, na širokém 5.

Přístupnost: plátno oblohy má popis, např. „Říjen 2026: 12 hvězd, souhvězdí Dýně a Padající list". Podrobnosti
jednotlivých dní nabízí Historie.

Bez jediného dne v `practice_days` ukáže obrazovka prázdný stav: „První hvězda se rozsvítí po prvním uzavřeném dni."

### Náhled v Historii — `SkyPreviewCard`

- Karta přes celou šířku mřížky, pod souhrnem nálad: neinteraktivní obloha aktuálního měsíce (bez blikání)
  a řádek „412 hvězd · 61 souhvězdí". Klepnutí otevře `SkyActivity`.
- **Ukáže se i v prázdné Historii**, pokud `practice_days` není prázdná — po smazání historie obloha dál existuje.
  Prázdný stav Historie (🌙) pak stojí pod kartou.
- Bez dnů v `practice_days` se karta neukáže vůbec.

### Dokončovací karta rituálu

Mění se jen věta pro `rhythmWeekReached`: „Tento týden jsi v rytmu. Na obloze se rozsvítilo souhvězdí: Konvice."
(jméno v 1. pádě, aby se nemuselo skloňovat). Žádná denní odměna navíc.

## Doména (`domain/model`, čistý Kotlin, `java.time`)

- `PathRhythm.Result` dostane `weeks: Map<LocalDate, Boolean>` — pondělí týdne → splněno. Týdny bez nabízeného dne
  v mapě nejsou. `current`/`longest` se počítají z téže mapy, takže Cesta a Obloha nemohou mít jiný názor na rytmus.
- `Constellation(number: Int, stars: List<SkyPoint>, edges: List<Pair<Int, Int>>)`, `SkyPoint(x: Float, y: Float)`
  v rozsahu 0..1; přesně 7 bodů v pořadí Po–Ne.
- `ConstellationCatalog.all: List<Constellation>` (53 položek) a `forWeek(number)`.
- `SkyPolicy.month(days, moods, rhythmWeeks, yearMonth): SkyMonth` a `SkyPolicy.summary(...): SkySummary`.
  - `SkyMonth(yearMonth, weeks: List<SkyWeek>, fieldSeed: Long)`
  - `SkyWeek(monday, constellationNumber, isInRhythm, stars: List<SkyStar>)`
  - `SkyStar(date, pointIndex, mood: MoodTag?)`
  - `SkySummary(stars, constellationsLit, litThisYear, weeksThisYear, timesLitByNumber: Map<Int, Int>)`
- Rozložení slotů a pootočení figur je čistá funkce v doméně (`SkyLayout`), aby byla testovatelná bez Compose.

## Data

- **Žádná nová tabulka ani migrace.** `RitualRepository.getSky(): Flow<SkyData>` kombinuje `practice_days`
  (přes stávající `parsePracticeDay`), nálady z `ritual_logs` (klíč = datum) a `includeWeekendsFlow`.
- `SkyData` nese množinu dnů, mapu nálad, výsledek rytmu a dnešní datum; `SkyPolicy` z něj skládá měsíce na žádost
  pageru, ne všechny najednou.
- Smazání historie: `ritual_logs` zmizí → hvězdy zbělají; `practice_days` zůstane → obloha i souhvězdí zůstanou.

## Katalog souhvězdí

### Pravidla tvaru

1. Přesně 7 bodů v rozsahu 0..1, pořadí Po–Ne; 5–8 hran.
2. **Body Po–Pá tvoří rozpoznatelný obrys**, So a Ne jsou ozdoba — figura musí fungovat i u uživatele bez víkendů.
3. **Figura Po–Pá je mezi všemi 53 jedinečná** (ověřuje test: normalizovaný tvar dvou figur se nesmí shodovat).
4. Figura připomíná předmět **jedním rysem** (výlevka, rukojeť, ocas), ne celou siluetou — jako skutečná souhvězdí.

Všech 53 figur se nakreslí během implementace a před dokončením se předloží ke kontrole jako jeden přehledový obrázek.

### Jména

Jména sledují roční období, protože číslo týdne je každý rok ve stejné části roku. Texty v
`res/values*/strings.xml` jako `string-array name="constellation_names"` (index = číslo týdne − 1).

| Týden | Česky | English |
|---|---|---|
| 1 | Hrnek čaje | Cup of Tea |
| 2 | Vlněné ponožky | Wool Socks |
| 3 | Krb | Fireplace |
| 4 | Teplá deka | Warm Blanket |
| 5 | Sáňky | Sled |
| 6 | Rukavice na topení | Mittens on the Radiator |
| 7 | Kniha na nočním stolku | Bedside Book |
| 8 | Hrnec polévky | Pot of Soup |
| 9 | Svíčka | Candle |
| 10 | Konvice | Kettle |
| 11 | Deštník u dveří | Umbrella by the Door |
| 12 | Sněženka | Snowdrop |
| 13 | Holínky | Rain Boots |
| 14 | Kolo | Bicycle |
| 15 | Konev | Watering Can |
| 16 | Otevřené okno | Open Window |
| 17 | Ptačí budka | Birdhouse |
| 18 | Houpačka | Swing |
| 19 | Pampeliška | Dandelion |
| 20 | Kočka na parapetu | Cat on the Sill |
| 21 | Zahradní lavička | Garden Bench |
| 22 | Košík jahod | Basket of Strawberries |
| 23 | Lampion | Paper Lantern |
| 24 | Síťka na motýly | Butterfly Net |
| 25 | Houpací síť | Hammock |
| 26 | Kornout zmrzliny | Ice Cream Cone |
| 27 | Slamák | Straw Hat |
| 28 | Táborák | Campfire |
| 29 | Stan | Tent |
| 30 | Loďka | Rowing Boat |
| 31 | Rybářský prut | Fishing Rod |
| 32 | Meloun | Watermelon |
| 33 | Padající hvězda | Shooting Star |
| 34 | Kytara | Guitar |
| 35 | Sklenice marmelády | Jar of Jam |
| 36 | Jablko | Apple |
| 37 | Drak | Kite |
| 38 | Košík hub | Basket of Mushrooms |
| 39 | Kaštany | Chestnuts |
| 40 | Dýně | Pumpkin |
| 41 | Pletení | Knitting |
| 42 | Padající list | Falling Leaf |
| 43 | Petrolejka | Oil Lamp |
| 44 | Šála | Scarf |
| 45 | Deskovka | Board Game |
| 46 | Papuče | Slippers |
| 47 | Křeslo | Armchair |
| 48 | Čerstvý chleba | Fresh Loaf |
| 49 | Adventní věnec | Advent Wreath |
| 50 | Perníček | Gingerbread |
| 51 | Sněhulák | Snowman |
| 52 | Jedlička | Little Fir Tree |
| 53 | Modrý měsíc | Blue Moon |

(Padající hvězda v 33. týdnu odpovídá Perseidám v polovině srpna.)

## Testy

- `ConstellationCatalogTest` — 53 položek s čísly 1–53; každá 7 bodů v 0..1; hrany odkazují na platné body,
  5–8 hran, bez smyček a duplicit; figury Po–Pá navzájem různé (po normalizaci posunu a měřítka);
  `constellation_names` má 53 položek v obou jazycích.
- `PathRhythmTest` — `weeks` souhlasí s `current`/`longest`; týdny bez nabízeného dne v mapě nejsou.
- `SkyPolicyTest` — týden patří měsíci svého čtvrtku (přelom měsíce i roku); 53. týden; hvězda na bodu podle dne
  v týdnu; nálada z logu, bez logu `null`; souhvězdí jen pro týden v rytmu; souhrn včetně „letos X z 52/53"
  a `timesLitByNumber` přes více let; měsíc bez hvězd.
- `SkyLayoutTest` — sloty pro 4 i 5 týdnů leží uvnitř plátna a nepřekrývají se; pootočení v ±12°; deterministické.
- Repozitář — `getSky()` po `clearLogs()` drží dny i souhvězdí, nálady jsou `null`.
- UI — `SkyPreviewCard` v prázdné Historii (po smazání) i v plné; bez dnů se neukáže; karta dne pro smazaný záznam;
  šipky přepínají měsíce; dokončovací karta jmenuje souhvězdí; čeština přes `AppLocalization`.
- Screenshot — `SkyScreen` a Historie s náhledem ve `FormFactorScreenshotTest` (4 tvary okna), fixní fixture
  a ručně posunutý čas kvůli blikání.
- `./gradlew :app:lintDebug` zelený.

## Dokumentace

`CLAUDE.md` a `AGENTS.md` společně: Architecture (Obloha, katalog, `SkyPolicy`, `SkyActivity`, náhled v Historii),
Module layout (`presentation/sky`), Testing (nové screenshoty), What NOT to do:

- Nepočítat rytmus pro oblohu jinde než v `PathRhythm`.
- Neměnit pořadí ani tvar existujících souhvězdí po vydání — uživatelům by se změnila jejich obloha i sbírka.
- Nepřidávat do oblohy náhodnost za běhu — rozbila by screenshot testy i stálost oblohy.
