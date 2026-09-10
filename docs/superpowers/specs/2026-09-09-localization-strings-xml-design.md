# Migrace lokalizace z AppStrings do strings.xml

Datum: 2026-09-09

## Proč

`AppStrings.kt` drží 102 řetězců a 7 formátovacích funkcí jako Kotlin
`when (language)` výrazy. Třída existuje od prvního commitu repozitáře (ještě
pod balíčkem `com.example`) — je to pozůstatek generovaného scaffoldu, ne
architektonické rozhodnutí. V historii souboru není commit, který by ji
zaváděl s odůvodněním.

Jediný důvod, který by vlastní řešení obhájil, je přepínač jazyka v aplikaci
nezávislý na systémovém locale (`LanguageOption.SYSTEM` / `ENGLISH` /
`CZECH`). Ten problém ale repozitář už řeší přes resources: `QuestionsData`
si přes `createConfigurationContext(setLocale(...))` vytáhne `Resources` pro
konkrétní jazyk a takto načítá celý katalog rituálních otázek z
`strings.xml`. Precedens je zavedený a funkční.

Souběžná existence obou systémů už teď škodí. `values/strings.xml` a
`values-cs/strings.xml` obsahují šest klíčů, které mají protějšek v
`AppStrings` a které nikdo nečte: `app_name`, `app_subtitle`, `start_ritual`,
`history_title`, `settings_title`, `language_title`. Rozejít se mohou
kdykoli.

## Cíl

Jediný zdroj pravdy pro překlady: `values/strings.xml` a
`values-cs/strings.xml`. `AppStrings.kt` zaniká. Chování aplikace zůstává
stejné, s dvěma výjimkami uvedenými v části "Opravy chyb".

## Rozsah

| Místo | Překladových párů |
|---|---|
| `presentation/util/AppStrings.kt` | 102 hodnot + 7 funkcí |
| `domain/model/MeditationSoundStyle.kt` | 8 |
| `domain/model/MoodTag.kt` | 8 |
| `domain/model/AppLanguage.kt` (`LanguageOption.label`) | 1 |
| `domain/model/SoundChoice.kt` | delegace na styl |
| `receiver/NotificationReceiver.kt` | 2 |

Volání `strings.*`: 101 (Home 21, History 11, Ritual 26, Settings 42, plus
`RailDestination` v `PadlaApp`).

Mimo rozsah: `RitualQuestion` a `QuestionsData` — už čtou z XML.
`RitualPreferences` — `when (language)` je tam mapování enumů, ne překlad.

## Architektura

### Lokalizovaný Context

Nový soubor `org/clockout/util/LocalizedContext.kt`:

```kotlin
fun Context.localizedFor(language: AppLanguage): Context =
    createConfigurationContext(
        Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.code))
        }
    )
```

`QuestionsData.resourcesFor` se na tento helper přepíše, aby logika žila na
jednom místě.

Poskytne se v `PadlaApp`, hned za čtením jazyka (`PadlaApp.kt:114`), přes
`CompositionLocalProvider`:

```kotlin
val localized = LocalContext.current.localizedFor(state.language)
CompositionLocalProvider(
    LocalContext provides localized,
    LocalConfiguration provides localized.resources.configuration,
) { /* obsah aplikace */ }
```

Ověřeno proti `androidx.compose.ui:ui-android:1.11.4` (Compose BOM
2026.08.00): `stringResource()` čte z `LocalResources`, který je odvozený od
`LocalContext.current.resources` a invalidovaný změnou `LocalConfiguration`.
Override `LocalContext` tedy stačí; `LocalConfiguration` se poskytuje kvůli
kódu, který si locale čte z konfigurace přímo.

Přepnutí jazyka zůstává okamžité, bez `Activity.recreate()`. Jazyk už v
`PadlaApp` k dispozici je, nic nového se neprotahuje.

`RitualPreferences` a databáze se v `PadlaApp` vytvářejí nad původním
`context` (`PadlaApp.kt:89-96`), tedy nad providerem — DataStore i Room
zůstávají na nedotčeném Contextu, provider ovlivní jen resources ve stromu
pod sebou.

### AppUpdateBanner

`AppUpdateBanner` dnes visí v `MainActivity` v `Box` nad `PadlaApp`
(`MainActivity.kt:70-80`) a jako jediné místo v aplikaci už `stringResource()`
používá. Zůstal by proto mimo provider a držel by se systémového locale, což
je sice dnešní chování, ale po migraci by to byla viditelná nesrovnalost.

Řešení: `Box` s bannerem se přesune dovnitř `PadlaApp` jako slot.
`PadlaApp` dostane parametr `overlay: @Composable BoxScope.() -> Unit = {}`,
obalí svůj obsah do `Box` a slot zavolá uvnitř provideru. `MainActivity` pak
předá banner jako `overlay`. Stav updatu si dál drží `MainActivity`.

### Klíče v XML

Konvence `obrazovka_prvek`: `home_why_ritual_title`, `settings_about_desc`,
`ritual_step_indicator`, `history_empty_desc`. Sekce oddělené XML komentáři
podle obrazovek, ve stejném členění, jaké má dnes `AppStrings`. Šest výše
zmíněných existujících klíčů se použije, nevytvářejí se duplikáty.

### Formátování a plurály

Textové argumenty přes poziční placeholdery: `stringResource(R.string.x, arg)`.

`quoted()` se stane klíčem `quoted_answer` s hodnotou `„%1$s“` (cs) a
`“%1$s”` (en).

Dvě funkce počítají kusy a půjdou do `<plurals>`:

- `totalClosedDays(count)`
- `daysClosedSummary(count, target)`

### Doménové modely

`MoodTag`, `MeditationSoundStyle`, `SoundChoice` a `LanguageOption` přijdou o
textová pole i o metody `label(language)` / `description(language)`.
Mapování enum → resource id žije v `presentation/util/Localization.kt`:

```kotlin
@get:StringRes
val MoodTag.labelRes: Int get() = when (this) {
    MoodTag.CALM -> R.string.mood_calm_label
    ...
}
```

Doménová vrstva tak zůstane bez závislosti na `R` a androidx anotacích a
její testy nepotřebují Robolectric.

Endonyma se nepřekládají a zůstávají v kódu: `AppLanguage.displayName`
(`English`, `Čeština`) a stejnojmenné položky `LanguageOption`. Do XML jde
jen `LanguageOption.SYSTEM.label` („Dle systému" / „System default").

`RitualQuestion` si ponechá `textEn`/`textCs` i `suggestedAnswersEn`/`Cs`.
Obojí najednou potřebuje historie, aby uloženou frázi uměla zobrazit v
aktuálním jazyce; plní se z XML už dnes.

### Mimo Compose

`NotificationReceiver.showNotification` použije
`context.localizedFor(language).getString(R.string.notification_*)`.

## Opravy chyb, které migrace přináší

1. `AppStrings.kt:290` obsahuje `val languageSectionTitle: String = "App
   Language"` — jediná hodnota bez české varianty. Migrace ji doplní.
2. Čeština dnes u počtů nerozlišuje tvary, takže vzniká „Celkem uzavřeno: 1
   dnů". `<plurals>` to opraví (`one` / `few` / `many` / `other`).

## Testy

- `ExampleUnitTest.testAppStringsLocalization` se přepíše na čtení resources
  ve dvou locale přes `localizedFor`.
- Nový test na `<plurals>` v češtině pro počty 1, 3 a 5.
- Screenshot testy (`FormFactorScreenshotTest`, `ClosureStepScreenshotTest`,
  `GreetingScreenshotTest`, `AdaptiveNavigationTest`) se pouští před i po
  každém commitu. Očekává se shoda — výchozí locale se nemění.
- `HistoryScreenLanguageTest` a `RitualLogLocalizationTest` hlídají, že
  přepnutí jazyka dál přepíná i uložené fráze.

## Postup

Jeden commit na obrazovku, build a testy zelené v každém kroku. `AppStrings`
během migrace zůstává a zmenšuje se; smaže se až posledním commitem.

1. `LocalizedContext.kt`, provider v `PadlaApp`, `overlay` slot pro
   `AppUpdateBanner`, `QuestionsData` na sdílený helper
2. Doménové modely + `presentation/util/Localization.kt` — jde před
   obrazovkami, protože `MoodTag.label()` volají Home, History i Ritual
3. HomeScreen
4. HistoryScreen
5. RitualScreen
6. SettingsScreen (včetně `RailDestination` v `PadlaApp`)
7. NotificationReceiver
8. Smazání `AppStrings.kt`, přepsání testů
