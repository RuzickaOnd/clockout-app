# Migrace lokalizace do strings.xml — implementační plán

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Přesunout všech 98 živých překladů z `AppStrings.kt` a z doménových modelů do `values/strings.xml` a `values-cs/strings.xml`, aby měla aplikace jediný zdroj pravdy pro texty.

**Architecture:** Jazyk vybraný v aplikaci se do Compose stromu vnese lokalizovaným `Context`em (`createConfigurationContext` se `setLocale`) přes `CompositionLocalProvider`. Obrazovky pak volají `stringResource()`. Doménové enumy přijdou o texty; mapování enum → resource id žije v presentation vrstvě. Mimo Compose (notifikace) se použije stejný `Context` helper.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose (BOM 2026.08.00, `ui-android` 1.11.4), Robolectric + Roborazzi, minSdk 26 / targetSdk 37.

**Spec:** `docs/superpowers/specs/2026-09-09-localization-strings-xml-design.md`

## Global Constraints

- **Texty se kopírují doslova** z uvedeného řádku `AppStrings.kt` (resp. doménového modelu). Plán uvádí u každého klíče přesný zdrojový řádek; formulace se při migraci nepřepisují ani nevylepšují.
- **Rozdíl „2–3 min" vs „3 minuty" je záměrný.** `start_ritual` říká „(2–3 min)", zatímco `home_why_ritual_desc` a `history_empty_desc` mluví o třech minutách. Nesjednocovat.
- **Každý klíč se přidává do obou souborů** `app/src/main/res/values/strings.xml` (en) i `app/src/main/res/values-cs/strings.xml` (cs), ve stejném pořadí a se stejnými komentářovými sekcemi.
- **XML escaping:** `&` → `&amp;`, `<` → `&lt;`, apostrof → `\'`, uvozovka `"` → `\"`. Znaky `„ “ ” → –` se nepíšou jako entity, jdou do XML přímo.
- **Žádná nová gradle závislost.** Řešení stojí na `createConfigurationContext`, který je v SDK od API 17.
- **Po každém tasku musí být zelené** `./gradlew :app:testDebugUnitTest`.
- `AppStrings.kt` se během migrace nemaže — jen se z něj ubírá. Smaže se až v Tasku 8.
- Commit messages česky, prefixy `refactor:` / `fix:` jako v dosavadní historii.

## Přehled souborů

| Soubor | Odpovědnost |
|---|---|
| `app/src/main/java/org/clockout/util/LocalizedContext.kt` (nový) | `Context.localizedFor(AppLanguage)` — jediné místo, kde se vyrábí lokalizovaný Context |
| `app/src/main/java/org/clockout/presentation/util/Localization.kt` (nový) | `AppLocalization` provider + mapování doménových enumů na resource id |
| `app/src/main/res/values/strings.xml` | anglické texty |
| `app/src/main/res/values-cs/strings.xml` | české texty |
| `app/src/main/java/org/clockout/presentation/util/AppStrings.kt` | postupně se vyprazdňuje, v Tasku 8 mizí |

## Mrtvý kód ke smazání (Task 8)

Těchto šest členů `AppStrings` nikdo nevolá — do XML **nepřecházejí**, jen se smažou:
`closeRitualDialogTitle` (122), `closeRitualDialogText` (126), `closeRitualContinue` (134), `buttonCompleteRitual` (234), `languageSectionDesc` (291, hodnota `""`), `soundStyleDesc` (348).

---

### Task 1: Lokalizovaný Context a provider

**Files:**
- Create: `app/src/main/java/org/clockout/util/LocalizedContext.kt`
- Create: `app/src/main/java/org/clockout/presentation/util/Localization.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt:88-115` (provider + `overlay` slot)
- Modify: `app/src/main/java/org/clockout/MainActivity.kt:64-82` (banner do slotu)
- Modify: `app/src/main/java/org/clockout/data/local/QuestionsData.kt:120-125` (na sdílený helper)
- Test: `app/src/test/java/org/clockout/LocalizationProviderTest.kt`

**Interfaces:**
- Produces: `fun Context.localizedFor(language: AppLanguage): Context` v balíčku `org.clockout.util`
- Produces: `@Composable fun AppLocalization(language: AppLanguage, content: @Composable () -> Unit)` v balíčku `org.clockout.presentation.util`
- Produces: `PadlaApp(modifier: Modifier = Modifier, overlay: @Composable BoxScope.() -> Unit = {})`
- Consumes: nic

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/LocalizationProviderTest.kt`:

```kotlin
package org.clockout

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.util.AppLocalization
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Jazyk vybraný v aplikaci se může lišit od systémového locale, takže resources musí
 * jít o zvolený jazyk požádat i na zařízení nastaveném jinak.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LocalizationProviderTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `czech is served under a czech provider`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.app_subtitle))
            }
        }

        composeTestRule.onNodeWithText("Přechod práce → volno").assertIsDisplayed()
    }

    @Test
    fun `english is served under an english provider`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.app_subtitle))
            }
        }

        composeTestRule.onNodeWithText("Work → Leisure transition").assertIsDisplayed()
    }

    @Test
    fun `a nested provider wins over the one above it`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                AppLocalization(AppLanguage.CZECH) {
                    Text(stringResource(R.string.settings_title))
                }
            }
        }

        composeTestRule.onNodeWithText("Nastavení").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*LocalizationProviderTest*"`
Expected: FAIL — kompilace neprojde, `AppLocalization` neexistuje (`Unresolved reference: AppLocalization`).

- [ ] **Step 3: Vytvoř `LocalizedContext.kt`**

```kotlin
package org.clockout.util

import android.content.Context
import android.content.res.Configuration
import org.clockout.domain.model.AppLanguage
import java.util.Locale

/**
 * Resources pro jazyk vybraný v aplikaci, který se může lišit od systémového locale.
 * Jediné místo, kde se locale override vyrábí - používá ho jak Compose strom, tak
 * katalog otázek a notifikace.
 */
fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    return createConfigurationContext(configuration)
}
```

- [ ] **Step 4: Vytvoř `Localization.kt` s providerem**

```kotlin
package org.clockout.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.clockout.domain.model.AppLanguage
import org.clockout.util.localizedFor

/**
 * Podřídí resources ve stromu jazyku vybranému v aplikaci. stringResource() čte z
 * LocalResources, který je odvozený od LocalContext.resources, takže override Contextu
 * stačí; LocalConfiguration se poskytuje kvůli kódu, který si locale čte z konfigurace.
 */
@Composable
fun AppLocalization(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val localized = remember(context, language) { context.localizedFor(language) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content
    )
}
```

- [ ] **Step 5: Spusť test a ověř, že prochází**

Run: `./gradlew :app:testDebugUnitTest --tests "*LocalizationProviderTest*"`
Expected: PASS, 3 testy.

- [ ] **Step 6: Zapoj provider do `PadlaApp`**

V `PadlaApp.kt` za řádek 115 obal celé tělo obrazovky. `RitualPreferences` a databáze (řádky 89-96) **zůstávají nad providerem** — DataStore a Room mají dostat nedotčený Context.

Signatura se rozšíří o slot pro překryv:

```kotlin
@Composable
fun PadlaApp(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
```

a obsah se obalí:

```kotlin
    AppLocalization(language) {
        Box(modifier = Modifier.fillMaxSize()) {
            // dosavadní obsah PadlaApp
            overlay()
        }
    }
```

- [ ] **Step 7: Přesuň `AppUpdateBanner` do slotu**

V `MainActivity.kt` nahraď `Box` s `PadlaApp` a bannerem voláním:

```kotlin
val updateState by appUpdateController.state.collectAsStateWithLifecycle()

PadlaApp(
    overlay = {
        AppUpdateBanner(
            state = updateState,
            onInstall = { appUpdateController.completeUpdate() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
)
```

Banner se tím dostane pod provider a jeho dva `stringResource()` začnou respektovat jazyk aplikace.

- [ ] **Step 8: Přepni `QuestionsData` na sdílený helper**

V `QuestionsData.kt` smaž privátní `Context.resourcesFor` (řádky 120-125) a nahraď jeho volání:

```kotlin
val resourcesByLanguage =
    AppLanguage.entries.associateWith { context.localizedFor(it).resources }
```

Přidej `import org.clockout.util.localizedFor`, odeber nepoužité importy `Configuration`, `Locale` a `Resources`, pokud po změně zbydou bez užití.

- [ ] **Step 9: Spusť celou testovací sadu**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. `QuestionsCatalogTest` hlídá, že katalog otázek se po refaktoru načítá stejně.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/org/clockout/util/LocalizedContext.kt \
        app/src/main/java/org/clockout/presentation/util/Localization.kt \
        app/src/main/java/org/clockout/presentation/PadlaApp.kt \
        app/src/main/java/org/clockout/MainActivity.kt \
        app/src/main/java/org/clockout/data/local/QuestionsData.kt \
        app/src/test/java/org/clockout/LocalizationProviderTest.kt
git commit -m "refactor: resources následují jazyk vybraný v aplikaci"
```

---

### Task 2: Doménové modely

Jde před obrazovkami, protože `MoodTag.label()` volají Home, History i Ritual — po tomto tasku mají všechny tři jednotný způsob, jak náladu pojmenovat.

**Files:**
- Modify: `app/src/main/java/org/clockout/domain/model/MoodTag.kt`
- Modify: `app/src/main/java/org/clockout/domain/model/MeditationSoundStyle.kt`
- Modify: `app/src/main/java/org/clockout/domain/model/SoundChoice.kt`
- Modify: `app/src/main/java/org/clockout/domain/model/AppLanguage.kt:31-38` (`LanguageOption.label`)
- Modify: `app/src/main/java/org/clockout/presentation/util/Localization.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Modify volajících: `home/HomeScreen.kt:300`, `history/HistoryScreen.kt:261,324`, `ritual/RitualScreen.kt:966,1000,1008`, `settings/SettingsScreen.kt:590,632,633,673,674,743,780,935,936,970`
- Modify: `app/src/test/java/org/clockout/CustomSoundTest.kt:60-65`
- Test: `app/src/test/java/org/clockout/DomainLocalizationTest.kt`

**Interfaces:**
- Consumes: `AppLocalization` z Tasku 1
- Produces v `presentation/util/Localization.kt`:
  - `@get:StringRes val MoodTag.labelRes: Int`
  - `@get:StringRes val MoodTag.descriptionRes: Int`
  - `@get:StringRes val MeditationSoundStyle.labelRes: Int`
  - `@get:StringRes val MeditationSoundStyle.descriptionRes: Int`
  - `@Composable fun SoundChoice.localizedLabel(): String`
  - `@Composable fun SoundChoice.localizedDescription(): String`
  - `@Composable fun LanguageOption.localizedLabel(): String`
- `MoodTag` po tasku: `enum class MoodTag(val id: String, val emoji: String)`
- `MeditationSoundStyle` po tasku: `enum class MeditationSoundStyle(val id: String)` bez `label`/`description`
- `SoundChoice` po tasku: `sealed interface SoundChoice { val id: String }` bez `label`/`description`

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/DomainLocalizationTest.kt`:

```kotlin
package org.clockout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.CustomSound
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import org.clockout.presentation.util.AppLocalization
import org.clockout.presentation.util.labelRes
import org.clockout.presentation.util.localizedDescription
import org.clockout.presentation.util.localizedLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DomainLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(language: AppLanguage, content: @Composable () -> Unit) {
        composeTestRule.setContent { AppLocalization(language) { content() } }
    }

    @Test
    fun `a mood is named in the app language`() {
        show(AppLanguage.CZECH) { Text(stringResource(MoodTag.CALM.labelRes)) }

        composeTestRule.onNodeWithText("Klidný").assertIsDisplayed()
    }

    @Test
    fun `a built-in sound is named in the app language`() {
        show(AppLanguage.ENGLISH) {
            Text(SoundChoice.BuiltIn(MeditationSoundStyle.TIBETAN_BOWL).localizedLabel())
        }

        composeTestRule.onNodeWithText("Tibetan Singing Bowl & Gong").assertIsDisplayed()
    }

    @Test
    fun `a custom sound keeps the file name and reports its duration`() {
        val sound = CustomSound(
            id = "custom:1",
            displayName = "Ocean night",
            fileName = "ocean.mp3",
            durationMs = 154_000L,
            addedAt = 0L
        )

        show(AppLanguage.CZECH) {
            Text(SoundChoice.Custom(sound).localizedLabel())
            Text(SoundChoice.Custom(sound).localizedDescription())
        }

        composeTestRule.onNodeWithText("Ocean night").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Vlastní nahrávka (2:34) přehrávaná ve smyčce s jemným náběhem hlasitosti")
            .assertIsDisplayed()
    }

    @Test
    fun `the system language option is translated but endonyms are not`() {
        show(AppLanguage.CZECH) {
            Text(LanguageOption.SYSTEM.localizedLabel())
            Text(LanguageOption.ENGLISH.localizedLabel())
        }

        composeTestRule.onNodeWithText("Dle systému").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*DomainLocalizationTest*"`
Expected: FAIL — `Unresolved reference: labelRes`.

- [ ] **Step 3: Doplň klíče do obou strings.xml**

Sekce `<!-- Mood tags -->`, `<!-- Sound styles -->`, `<!-- Language options -->`. Hodnoty se kopírují doslova z uvedených řádků.

| Klíč | Zdroj (en i cs) |
|---|---|
| `mood_calm_label`, `mood_calm_desc` | `MoodTag.kt:11-17` (CALM) |
| `mood_tired_label`, `mood_tired_desc` | `MoodTag.kt:19-25` (TIRED) |
| `mood_satisfied_label`, `mood_satisfied_desc` | `MoodTag.kt:27-33` (SATISFIED) |
| `mood_tense_label`, `mood_tense_desc` | `MoodTag.kt:35-41` (TENSE) |
| `sound_tibetan_bowl_label`, `sound_tibetan_bowl_desc` | `MeditationSoundStyle.kt:10-13`, `29-32` |
| `sound_rain_pink_noise_label`, `sound_rain_pink_noise_desc` | `MeditationSoundStyle.kt:14-17`, `33-36` |
| `sound_breathing_waves_label`, `sound_breathing_waves_desc` | `MeditationSoundStyle.kt:18-21`, `37-40` |
| `sound_silent_haptic_label`, `sound_silent_haptic_desc` | `MeditationSoundStyle.kt:22-25`, `41-44` |
| `language_option_system` | `AppLanguage.kt:32-35` |

Pozor na `&` v `Tibetan Singing Bowl & Gong` a `Silent & Haptics Only` → `&amp;`.

Popis vlastní nahrávky se z interpolace stane formátovacím řetězcem (`SoundChoice.kt:25-29`):

```xml
<!-- values/strings.xml -->
<string name="sound_custom_desc">Your own recording (%1$s) played in a gentle looping fade</string>
<!-- values-cs/strings.xml -->
<string name="sound_custom_desc">Vlastní nahrávka (%1$s) přehrávaná ve smyčce s jemným náběhem hlasitosti</string>
```

- [ ] **Step 4: Zbav enumy textů**

`MoodTag.kt` — konstruktor jen `id` a `emoji`, metody `label()` a `description()` pryč:

```kotlin
enum class MoodTag(val id: String, val emoji: String) {
    CALM(id = "CALM", emoji = "😌"),
    TIRED(id = "TIRED", emoji = "🥱"),
    SATISFIED(id = "SATISFIED", emoji = "😊"),
    TENSE(id = "TENSE", emoji = "🤯");

    companion object {
        fun fromId(id: String?): MoodTag = entries.find { it.id == id } ?: CALM
    }
}
```

`MeditationSoundStyle.kt` — smaž `label()` i `description()`, zůstane `id` a `fromId`.

`SoundChoice.kt` — z rozhraní zmizí `label` a `description`, zůstane `id` a `fromId`:

```kotlin
sealed interface SoundChoice {
    val id: String

    data class BuiltIn(val style: MeditationSoundStyle) : SoundChoice {
        override val id: String get() = style.id
    }

    data class Custom(val sound: CustomSound) : SoundChoice {
        override val id: String get() = sound.id
    }

    companion object { /* Default a fromId beze změny */ }
}
```

`AppLanguage.kt` — z `LanguageOption` smaž metodu `label(language)` (řádky 31-38). `resolveEffectiveLanguage`, `code`, `fromCode` a `DEFAULT` zůstávají.

- [ ] **Step 5: Přidej mapování do `Localization.kt`**

```kotlin
import androidx.annotation.StringRes
import org.clockout.R
import org.clockout.domain.model.LanguageOption
import org.clockout.domain.model.MeditationSoundStyle
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.SoundChoice
import androidx.compose.ui.res.stringResource

@get:StringRes
val MoodTag.labelRes: Int
    get() = when (this) {
        MoodTag.CALM -> R.string.mood_calm_label
        MoodTag.TIRED -> R.string.mood_tired_label
        MoodTag.SATISFIED -> R.string.mood_satisfied_label
        MoodTag.TENSE -> R.string.mood_tense_label
    }

@get:StringRes
val MoodTag.descriptionRes: Int
    get() = when (this) {
        MoodTag.CALM -> R.string.mood_calm_desc
        MoodTag.TIRED -> R.string.mood_tired_desc
        MoodTag.SATISFIED -> R.string.mood_satisfied_desc
        MoodTag.TENSE -> R.string.mood_tense_desc
    }

@get:StringRes
val MeditationSoundStyle.labelRes: Int
    get() = when (this) {
        MeditationSoundStyle.TIBETAN_BOWL -> R.string.sound_tibetan_bowl_label
        MeditationSoundStyle.RAIN_PINK_NOISE -> R.string.sound_rain_pink_noise_label
        MeditationSoundStyle.BREATHING_WAVES -> R.string.sound_breathing_waves_label
        MeditationSoundStyle.SILENT_HAPTIC -> R.string.sound_silent_haptic_label
    }

@get:StringRes
val MeditationSoundStyle.descriptionRes: Int
    get() = when (this) {
        MeditationSoundStyle.TIBETAN_BOWL -> R.string.sound_tibetan_bowl_desc
        MeditationSoundStyle.RAIN_PINK_NOISE -> R.string.sound_rain_pink_noise_desc
        MeditationSoundStyle.BREATHING_WAVES -> R.string.sound_breathing_waves_desc
        MeditationSoundStyle.SILENT_HAPTIC -> R.string.sound_silent_haptic_desc
    }

/** Vlastní zvuk nese jméno souboru, které uživatel zvolil, a to se nepřekládá. */
@Composable
fun SoundChoice.localizedLabel(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.labelRes)
    is SoundChoice.Custom -> sound.displayName
}

@Composable
fun SoundChoice.localizedDescription(): String = when (this) {
    is SoundChoice.BuiltIn -> stringResource(style.descriptionRes)
    is SoundChoice.Custom -> stringResource(R.string.sound_custom_desc, sound.formattedDuration)
}

/** Endonyma zůstávají v původním jazyce, překládá se jen volba "podle systému". */
@Composable
fun LanguageOption.localizedLabel(): String = when (this) {
    LanguageOption.SYSTEM -> stringResource(R.string.language_option_system)
    LanguageOption.ENGLISH -> AppLanguage.ENGLISH.displayName
    LanguageOption.CZECH -> AppLanguage.CZECH.displayName
}
```

- [ ] **Step 6: Uprav volající místa**

Nálady — `HomeScreen.kt:300`, `HistoryScreen.kt:261,324`, `RitualScreen.kt:966,1000,1008`:
`mood.label(state.language)` → `stringResource(mood.labelRes)`, `mood.description(language)` → `stringResource(mood.descriptionRes)`.
Na řádcích `HomeScreen.kt:300` a `RitualScreen.kt:966` je výsledek argumentem `strings.workdayDoneDesc(...)`; ten zatím zůstává, jen dovnitř jde `stringResource(...)`.

Zvuky — `SettingsScreen.kt:590,632,633,673,674,743,780`:
`choice.label(state.language)` → `choice.localizedLabel()`, `choice.description(state.language)` → `choice.localizedDescription()`.

Jazyky — `SettingsScreen.kt:935,936,970`:
`option.label(state.language)` → `option.localizedLabel()`.

- [ ] **Step 7: Uprav `CustomSoundTest`**

Test na řádcích 60-65 volá zrušené metody. Nahraď ho testem, který nechá popis vykreslit pod providerem — vzor viz `DomainLocalizationTest` výše. Pokud po úpravě zbyde v `CustomSoundTest` jen tento případ, přesuň ho do `DomainLocalizationTest` a v `CustomSoundTest` ho smaž.

- [ ] **Step 8: Spusť testy**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. Screenshot testy musí zůstat beze změny — fixtures jedou v angličtině (`FormFactorFixtures.kt:56,63,89,96`), `ClosureStepScreenshotTest` nálady nevykresluje.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/org/clockout/domain/model/ \
        app/src/main/java/org/clockout/presentation/util/Localization.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/main/java/org/clockout/presentation/ \
        app/src/test/java/org/clockout/
git commit -m "refactor: nálady, zvuky a volby jazyka berou texty z resources"
```

---

### Task 3: HomeScreen

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/home/HomeScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/java/org/clockout/presentation/util/AppStrings.kt` (odebrání přenesených členů)
- Test: `app/src/test/java/org/clockout/HomeScreenLocalizationTest.kt`

**Interfaces:**
- Consumes: `AppLocalization` (Task 1), `MoodTag.labelRes` (Task 2)
- Produces: klíče `home_*`, `nav_*`, `common_quoted` v obou strings.xml

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/HomeScreenLocalizationTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.presentation.home.HomeScreen
import org.clockout.presentation.home.HomeState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Texty obrazovky teď určuje provider, ne pole ve stavu. Test drží stav v angličtině a
 * provider v češtině, aby bylo vidět, že rozhoduje provider.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    @Test
    fun `the home screen follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HomeScreen(
                        state = HomeState(
                            language = AppLanguage.ENGLISH,
                            todayQuestion = question
                        ),
                        onStartRitual = {},
                        onNavigateToHistory = {},
                        onNavigateToSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Uzavři vědomě dnešní den").assertIsDisplayed()
        composeTestRule.onNodeWithText("DNEŠNÍ OTÁZKA").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*HomeScreenLocalizationTest*"`
Expected: FAIL — vykreslí se anglické „Close today's workday mindfully", protože obrazovka zatím čte `state.language`.

- [ ] **Step 3: Doplň klíče do obou strings.xml**

Sekce `<!-- Home -->`. Klíče `app_name`, `app_subtitle`, `start_ritual` a `repeat_ritual` **už v XML jsou a mají shodné hodnoty** — nové duplikáty se nevytvářejí.

| Člen `AppStrings` | Řádek | Klíč |
|---|---|---|
| `appName` | 8 | `app_name` (existuje) |
| `quoted` | 26 | `common_quoted` |
| `appSubtitle` | 32 | `app_subtitle` (existuje) |
| `navHome` | 36 | `nav_home` |
| `navHistory` | 40 | `nav_history` |
| `navSettings` | 44 | `nav_settings` |
| `slotWorkdayEnd` | 48 | `home_slot_workday_end` |
| `weekendSlotBadge` | 52 | `home_weekend_slot_badge` |
| `weekendRestTitle` | 56 | `home_weekend_rest_title` |
| `weekendRestDesc` | 60 | `home_weekend_rest_desc` |
| `todayClosedBadge` | 64 | `home_today_closed_badge` |
| `workdayDoneTitle` | 68 | `home_workday_done_title` |
| `workdayDoneDesc` | 72 | `home_workday_done_desc` |
| `repeatRitual` | 76 | `repeat_ritual` (existuje) |
| `closeDayTitle` | 80 | `home_close_day_title` |
| `closeDayDesc` | 84 | `home_close_day_desc` |
| `todayQuestionLabel` | 88 | `home_today_question_label` |
| `startRitualButton` | 92 | `start_ritual` (existuje) |
| `thisWeekTitle` | 96 | `home_this_week_title` |
| `daysClosedSummary` | 100 | `home_days_closed_summary` (plurals) |
| `whyRitualTitle` | 104 | `home_why_ritual_title` |
| `whyRitualDesc` | 108 | `home_why_ritual_desc` |

Formátovací klíče:

```xml
<!-- values/strings.xml -->
<string name="common_quoted">“%1$s”</string>
<string name="home_workday_done_desc">Mood: %1$s. Enjoy your free evening for yourself and your mind.</string>
<plurals name="home_days_closed_summary">
    <item quantity="one">%1$d of %2$d day closed</item>
    <item quantity="other">%1$d of %2$d days closed</item>
</plurals>

<!-- values-cs/strings.xml -->
<string name="common_quoted">„%1$s“</string>
<string name="home_workday_done_desc">Nálada: %1$s. Užij si volný večer pro sebe a své tělo.</string>
<plurals name="home_days_closed_summary">
    <item quantity="one">%1$d z %2$d dne uzavřen</item>
    <item quantity="few">%1$d z %2$d dnů uzavřeno</item>
    <item quantity="other">%1$d z %2$d dní uzavřeno</item>
</plurals>
```

Množné číslo se v obou jazycích řídí cílem (`target`), protože skloňované je slovo „dní".

- [ ] **Step 4: Přepiš `HomeScreen` na `stringResource`**

21 volání `strings.*` nahraď. Vzory:

```kotlin
// prosté
Text(text = stringResource(R.string.home_close_day_title))

// s argumentem
Text(text = stringResource(R.string.home_workday_done_desc, moodLabel))

// uvozovky kolem odpovědi
Text(text = stringResource(R.string.common_quoted, answer))

// plurál - pozor na pořadí: count pro výběr tvaru je target
Text(
    text = pluralStringResource(
        R.plurals.home_days_closed_summary,
        target,
        count,
        target
    )
)
```

Přidej importy `androidx.compose.ui.res.stringResource`, `androidx.compose.ui.res.pluralStringResource` a `org.clockout.R`. Odeber `val strings = ...` a nepoužité importy `AppStrings`.

Pokud si nějaká pomocná funkce v souboru bere `strings: AppStrings` jako parametr, parametr zruš a `stringResource` volej uvnitř — funkce označ `@Composable`.

- [ ] **Step 5: Ubere přenesené členy z `AppStrings`**

Smaž z `AppStrings.kt` členy z tabulky ve Step 3 (řádky 8, 26, 32-108). `navHome`, `navHistory` a `navSettings` **nech zatím být** — používá je `RailDestination` v `PadlaApp`, který se řeší v Tasku 6.

- [ ] **Step 6: Spusť testy**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS včetně `HomeScreenLocalizationTest`, `HomeScreenWeekendTest`, `HomeScreenClosedDayAnswerTest`, `HomeTwoPaneTest` a screenshot testů.

- [ ] **Step 7: Ověř screenshoty**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: PASS bez rozdílů. Pokud se objeví rozdíl, nezaznamenávej nové screenshoty — jde o regresi v textu, oprav ji.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/org/clockout/presentation/home/HomeScreen.kt \
        app/src/main/java/org/clockout/presentation/util/AppStrings.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/test/java/org/clockout/HomeScreenLocalizationTest.kt
git commit -m "refactor: HomeScreen bere texty z resources"
```

---

### Task 4: HistoryScreen

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/history/HistoryScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/java/org/clockout/presentation/util/AppStrings.kt`
- Modify: `app/src/test/java/org/clockout/HistoryScreenLanguageTest.kt`
- Test: tamtéž

**Interfaces:**
- Consumes: `AppLocalization`, `common_quoted` (Task 3), `MoodTag.labelRes` (Task 2)
- Produces: klíče `history_*` a `common_back`, `common_cancel`, `common_delete`

- [ ] **Step 1: Uprav existující `HistoryScreenLanguageTest`**

Test vykresluje `HistoryScreen` přímo, takže po migraci by mu češtinu neurčoval `state.language`, ale locale testu. Obal obsah providerem — `setContent` v `HistoryScreenLanguageTest.kt:37-46` přepiš:

```kotlin
    private fun setContent(language: AppLanguage, log: RitualLogEntry) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    HistoryScreen(
                        state = HistoryState(language = language, logs = listOf(log), totalCount = 1),
                        onBack = {},
                        onClearHistory = {}
                    )
                }
            }
        }
    }
```

Přidej `import org.clockout.presentation.util.AppLocalization`. `state.language` zůstává — řídí překlad uložených frází přes `RitualQuestion`, ne texty rozhraní.

- [ ] **Step 2: Přidej padající test na plurály**

Do `HistoryScreenLanguageTest.kt`:

```kotlin
    @Test
    fun `czech declines the day count`() {
        val logs = listOf(czechLog)

        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    HistoryScreen(
                        state = HistoryState(
                            language = AppLanguage.CZECH,
                            logs = logs,
                            totalCount = 1
                        ),
                        onBack = {},
                        onClearHistory = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Celkem uzavřeno: 1 den").assertIsDisplayed()
    }
```

- [ ] **Step 3: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*HistoryScreenLanguageTest*"`
Expected: FAIL — vykreslí se „Celkem uzavřeno: 1 dnů".

- [ ] **Step 4: Doplň klíče do obou strings.xml**

Sekce `<!-- History -->`. Klíč `history_title` už v XML je a hodnoty sedí — použij ho.

| Člen `AppStrings` | Řádek | Klíč |
|---|---|---|
| `back` | 12 | `common_back` |
| `cancel` | 16 | `common_cancel` |
| `delete` | 20 | `common_delete` |
| `historyTitle` | 252 | `history_title` (existuje) |
| `clearHistoryTooltip` | 256 | `history_clear_tooltip` |
| `clearHistoryDialogTitle` | 260 | `history_clear_dialog_title` |
| `clearHistoryDialogText` | 264 | `history_clear_dialog_text` |
| `emptyHistoryTitle` | 268 | `history_empty_title` |
| `emptyHistoryDesc` | 272 | `history_empty_desc` |
| `totalClosedDays` | 276 | `history_total_closed_days` (plurals) |
| `padlaBadge` | 280 | `history_padla_badge` |

```xml
<!-- values/strings.xml -->
<plurals name="history_total_closed_days">
    <item quantity="one">Total closed: %1$d day</item>
    <item quantity="other">Total closed: %1$d days</item>
</plurals>

<!-- values-cs/strings.xml -->
<plurals name="history_total_closed_days">
    <item quantity="one">Celkem uzavřeno: %1$d den</item>
    <item quantity="few">Celkem uzavřeno: %1$d dny</item>
    <item quantity="other">Celkem uzavřeno: %1$d dní</item>
</plurals>
```

- [ ] **Step 5: Přepiš `HistoryScreen`**

11 volání `strings.*` nahraď `stringResource(...)`; počet uzavřených dnů přes:

```kotlin
Text(text = pluralStringResource(R.plurals.history_total_closed_days, count, count))
```

Odeber `val strings = ...` a import `AppStrings`.

- [ ] **Step 6: Ubere přenesené členy z `AppStrings`**

Smaž řádky 12-24 (`back`, `cancel`, `delete`) a 252-283.

- [ ] **Step 7: Spusť testy a screenshoty**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:verifyRoborazziDebug`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/org/clockout/presentation/history/HistoryScreen.kt \
        app/src/main/java/org/clockout/presentation/util/AppStrings.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/test/java/org/clockout/HistoryScreenLanguageTest.kt
git commit -m "refactor: HistoryScreen bere texty z resources a skloňuje počet dnů"
```

---

### Task 5: RitualScreen

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/java/org/clockout/presentation/util/AppStrings.kt`
- Modify: `app/src/test/java/org/clockout/ClosureStepScreenshotTest.kt`
- Test: `app/src/test/java/org/clockout/RitualScreenLocalizationTest.kt`

**Interfaces:**
- Consumes: `AppLocalization`, `MoodTag.labelRes` a `descriptionRes` (Task 2), `home_workday_done_desc` (Task 3)
- Produces: klíče `ritual_*`

- [ ] **Step 1: Obal `ClosureStepScreenshotTest` providerem**

Test staví `RitualState(language = AppLanguage.CZECH, ...)` (`ClosureStepScreenshotTest.kt:39-44`) a jeho uložené screenshoty jsou české. Po migraci by se vykreslily anglicky a screenshot by nesouhlasil. Do `setContent` proto přidej `AppLocalization(AppLanguage.CZECH) { ... }` kolem `MyApplicationTheme`, plus `import org.clockout.presentation.util.AppLocalization`.

- [ ] **Step 2: Napiš padající test**

`app/src/test/java/org/clockout/RitualScreenLocalizationTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.RitualQuestion
import org.clockout.domain.model.RitualStep
import org.clockout.presentation.ritual.RitualScreen
import org.clockout.presentation.ritual.RitualState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RitualScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val question = RitualQuestion(
        id = "q_success",
        textEn = "What went well today?",
        textCs = "Co se dnes povedlo?",
        suggestedAnswersEn = listOf("Finished an important task"),
        suggestedAnswersCs = listOf("Dokončil(a) jsem důležitý úkol")
    )

    @Test
    fun `the breathing step follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                MyApplicationTheme {
                    RitualScreen(
                        state = RitualState(
                            language = AppLanguage.ENGLISH,
                            currentStep = RitualStep.Breathing,
                            question = question
                        ),
                        onIntent = {},
                        onFinish = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Vědomý dech 4s / 4s").assertIsDisplayed()
        composeTestRule.onNodeWithText("Krok 2 ze 4").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*RitualScreenLocalizationTest*"`
Expected: FAIL — vykreslí se „Mindful breath 4s / 4s".

- [ ] **Step 4: Doplň klíče do obou strings.xml**

Sekce `<!-- Ritual -->`.

| Člen `AppStrings` | Řádek | Klíč |
|---|---|---|
| `ritualHeaderTitle` | 114 | `ritual_header_title` |
| `stepIndicator` | 118 | `ritual_step_indicator` |
| `closeRitualConfirm` | 130 | `ritual_close_confirm` |
| `questionCategory` | 140 | `ritual_question_category` |
| `questionInstruction` | 144 | `ritual_question_instruction` |
| `customAnswerPlaceholder` | 148 | `ritual_custom_answer_placeholder` |
| `buttonContinueToBreathing` | 152 | `ritual_button_continue_to_breathing` |
| `breathingCategory` | 158 | `ritual_breathing_category` |
| `breathingTitle` | 162 | `ritual_breathing_title` |
| `breathingInhale` | 166 | `ritual_breathing_inhale` |
| `breathingExhale` | 170 | `ritual_breathing_exhale` |
| `breathingInhaleDesc` | 174 | `ritual_breathing_inhale_desc` |
| `breathingExhaleDesc` | 178 | `ritual_breathing_exhale_desc` |
| `buttonFinishBreathing` | 182 | `ritual_button_finish_breathing` |
| `buttonSkipBreathing` | 186 | `ritual_button_skip_breathing` |
| `closureCategory` | 192 | `ritual_closure_category` |
| `closureTapPrompt` | 196 | `ritual_closure_tap_prompt` |
| `closureDoneTitle` | 200 | `ritual_closure_done_title` |
| `closureLampTurnOffPrompt` | 204 | `ritual_closure_lamp_turn_off_prompt` |
| `closureFinishedPrompt` | 208 | `ritual_closure_finished_prompt` |
| `closureDoneMessage` | 212 | `ritual_closure_done_message` |
| `buttonRecordMood` | 216 | `ritual_button_record_mood` |
| `moodCategory` | 222 | `ritual_mood_category` |
| `moodTitle` | 226 | `ritual_mood_title` |
| `moodSubtitle` | 230 | `ritual_mood_subtitle` |
| `buttonSkipMood` | 238 | `ritual_button_skip_mood` |
| `ritualCompletedNotice` | 242 | `ritual_completed_notice` |
| `returnHome` | 246 | `ritual_return_home` |

Ukazatel kroku má dva argumenty:

```xml
<!-- values/strings.xml -->
<string name="ritual_step_indicator">Step %1$d of %2$d</string>
<!-- values-cs/strings.xml -->
<string name="ritual_step_indicator">Krok %1$d ze %2$d</string>
```

Výchozí hodnota `total = 4` z `AppStrings.stepIndicator` zaniká — volající předává obě čísla.

- [ ] **Step 5: Přepiš `RitualScreen`**

26 volání `strings.*` nahraď. Ukazatel kroku:

```kotlin
Text(text = stringResource(R.string.ritual_step_indicator, step, 4))
```

Popis nálady na řádku 966 používá `strings.workdayDoneDesc(...)` — klíč `home_workday_done_desc` už existuje z Tasku 3:

```kotlin
Text(
    text = stringResource(
        R.string.home_workday_done_desc,
        selectedMood?.let { stringResource(it.labelRes) } ?: ""
    )
)
```

Odeber `val strings = ...` a import `AppStrings`.

- [ ] **Step 6: Ubere přenesené členy z `AppStrings`**

Smaž řádky 114-120 a 130-249 s výjimkou mrtvých členů, které se řeší až v Tasku 8 (`closeRitualDialogTitle`, `closeRitualDialogText`, `closeRitualContinue`, `buttonCompleteRitual` klidně smaž rovnou zde, pokud je diff přehlednější).

- [ ] **Step 7: Spusť testy a screenshoty**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:verifyRoborazziDebug`
Expected: PASS. `ClosureStepScreenshotTest` musí sedět proti uloženým českým screenshotům.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/org/clockout/presentation/ritual/RitualScreen.kt \
        app/src/main/java/org/clockout/presentation/util/AppStrings.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/test/java/org/clockout/RitualScreenLocalizationTest.kt \
        app/src/test/java/org/clockout/ClosureStepScreenshotTest.kt
git commit -m "refactor: RitualScreen bere texty z resources"
```

---

### Task 6: SettingsScreen a navigační rail

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt:155,255-263` (`RailDestination`)
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/java/org/clockout/presentation/util/AppStrings.kt`
- Modify: `app/src/test/java/org/clockout/SettingsWeekendToggleTest.kt` (pokud asertuje na text)
- Test: `app/src/test/java/org/clockout/SettingsScreenLocalizationTest.kt`

**Interfaces:**
- Consumes: `AppLocalization`, `SoundChoice.localizedLabel()`, `LanguageOption.localizedLabel()` (Task 2), `nav_*` klíče (Task 3)
- Produces: klíče `settings_*`; `RailDestination.label` se mění na `@StringRes val labelRes: Int`

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/SettingsScreenLocalizationTest.kt`:

```kotlin
package org.clockout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.settings.SettingsScreen
import org.clockout.presentation.settings.SettingsState
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SettingsScreenLocalizationTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(language: AppLanguage) {
        composeTestRule.setContent {
            AppLocalization(language) {
                MyApplicationTheme {
                    SettingsScreen(
                        state = SettingsState(language = AppLanguage.ENGLISH),
                        onBack = {},
                        onTimeSelected = { _, _ -> },
                        onTestModeToggled = {},
                        onSoundToggled = {},
                        onClearData = {}
                    )
                }
            }
        }
    }

    @Test
    fun `settings follow the provider language`() {
        show(AppLanguage.CZECH)

        composeTestRule.onNodeWithText("Nastavení").assertIsDisplayed()
        composeTestRule.onNodeWithText("Čas konce pracovního dne").assertIsDisplayed()
    }

    @Test
    fun `the language section header is translated`() {
        show(AppLanguage.CZECH)

        composeTestRule.onNodeWithText("Jazyk aplikace").assertIsDisplayed()
    }
}
```

Druhý test pokrývá opravu: `AppStrings.languageSectionTitle` je natvrdo `"App Language"` bez české varianty (`AppStrings.kt:290`).

- [ ] **Step 2: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsScreenLocalizationTest*"`
Expected: FAIL — vykreslí se „Settings" a „App Language".

- [ ] **Step 3: Doplň klíče do obou strings.xml**

Sekce `<!-- Settings -->`. Klíče `settings_title` a `language_title` už v XML jsou; `language_title` má v `values-cs` hodnotu „Jazyk aplikace", což je právě chybějící překlad.

| Člen `AppStrings` | Řádek | Klíč |
|---|---|---|
| `settingsTitle` | 286 | `settings_title` (existuje) |
| `languageSectionTitle` | 290 | `language_title` (existuje, opraví chybějící češtinu) |
| `workdayEndTimeTitle` | 292 | `settings_workday_end_time_title` |
| `notificationsDisabled` | 296 | `settings_notifications_disabled` |
| `notificationsEnabled` | 300 | `settings_notifications_enabled` |
| `permissionDialogTitle` | 304 | `settings_permission_dialog_title` |
| `permissionDeniedMessage` | 308 | `settings_permission_denied_message` |
| `currentlySetTo` | 312 | `settings_currently_set_to` |
| `quickPresetsLabel` | 316 | `settings_quick_presets_label` |
| `includeWeekendsTitle` | 320 | `settings_include_weekends_title` |
| `includeWeekendsDesc` | 324 | `settings_include_weekends_desc` |
| `testModeTitle` | 328 | `settings_test_mode_title` |
| `testModeDesc` | 332 | `settings_test_mode_desc` |
| `ambientSoundTitle` | 336 | `settings_ambient_sound_title` |
| `ambientSoundDesc` | 340 | `settings_ambient_sound_desc` |
| `soundStyleTitle` | 344 | `settings_sound_style_title` |
| `soundPreviewButton` | 352 | `settings_sound_preview_button` |
| `soundPreviewPlaying` | 356 | `settings_sound_preview_playing` |
| `customSoundSectionTitle` | 360 | `settings_custom_sound_section_title` |
| `customSoundGroupLabel` | 364 | `settings_custom_sound_group_label` |
| `builtInSoundGroupLabel` | 368 | `settings_built_in_sound_group_label` |
| `uploadCustomSoundButton` | 372 | `settings_upload_custom_sound_button` |
| `uploadingCustomSound` | 376 | `settings_uploading_custom_sound` |
| `customSoundHint` | 380 | `settings_custom_sound_hint` |
| `deleteCustomSoundTitle` | 384 | `settings_delete_custom_sound_title` |
| `deleteCustomSoundText` | 388 | `settings_delete_custom_sound_text` |
| `customSoundErrorUnsupported` | 392 | `settings_custom_sound_error_unsupported` |
| `customSoundErrorTooLarge` | 396 | `settings_custom_sound_error_too_large` |
| `customSoundErrorLimit` | 400 | `settings_custom_sound_error_limit` |
| `customSoundErrorRead` | 404 | `settings_custom_sound_error_read` |
| `aboutTitle` | 408 | `settings_about_title` |
| `aboutDesc` | 412 | `settings_about_desc` |
| `appVersionLabel` | 416 | `settings_app_version_label` |
| `clearDataButton` | 420 | `settings_clear_data_button` |
| `clearDataDialogTitle` | 424 | `settings_clear_data_dialog_title` |
| `clearDataDialogText` | 428 | `settings_clear_data_dialog_text` |
| `clearDataConfirm` | 432 | `settings_clear_data_confirm` |

Formátovací klíče:

```xml
<!-- values/strings.xml -->
<string name="settings_currently_set_to">Currently set to: %1$s</string>
<string name="settings_delete_custom_sound_text">The file \"%1$s\" will be removed from the app.</string>

<!-- values-cs/strings.xml -->
<string name="settings_currently_set_to">Aktuálně nastaveno: %1$s</string>
<string name="settings_delete_custom_sound_text">Soubor „%1$s“ bude z aplikace odstraněn.</string>
```

`settings_about_desc` obsahuje odstavce oddělené `\n\n` — v XML je zapiš jako `\n\n`, ne jako skutečné nové řádky.

- [ ] **Step 4: Přepiš `SettingsScreen`**

39 volání `strings.*` nahraď `stringResource(...)`. Odeber `val strings = ...` a import `AppStrings`.

Řádek s verzí aplikace (`SettingsScreen.kt:1036`) přejde na:

```kotlin
text = "${stringResource(R.string.settings_app_version_label)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
```

- [ ] **Step 5: Přepiš `RailDestination`**

`PadlaApp.kt:255-263`:

```kotlin
private enum class RailDestination(
    val screen: Screen,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Home(Screen.Home, Icons.Default.Nightlight, R.string.nav_home),
    History(Screen.History, Icons.Default.History, R.string.nav_history),
    Settings(Screen.Settings, Icons.Default.Settings, R.string.nav_settings)
}
```

a volání na řádku 155:

```kotlin
label = { Text(stringResource(destination.labelRes)) },
```

- [ ] **Step 6: Ubere přenesené členy z `AppStrings`**

Smaž řádky 36-46 (`navHome`, `navHistory`, `navSettings`) a 286-435. Po tomto kroku by měl v `AppStrings` zbýt jen mrtvý kód.

- [ ] **Step 7: Spusť testy a screenshoty**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:verifyRoborazziDebug`
Expected: PASS včetně `SettingsWeekendToggleTest` a `AdaptiveNavigationTest`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/org/clockout/presentation/settings/SettingsScreen.kt \
        app/src/main/java/org/clockout/presentation/PadlaApp.kt \
        app/src/main/java/org/clockout/presentation/util/AppStrings.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/test/java/org/clockout/SettingsScreenLocalizationTest.kt
git commit -m "refactor: SettingsScreen bere texty z resources a doplňuje český název sekce jazyka"
```

---

### Task 7: Notifikace

**Files:**
- Modify: `app/src/main/java/org/clockout/receiver/NotificationReceiver.kt:87-95`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-cs/strings.xml`
- Test: `app/src/test/java/org/clockout/NotificationTextTest.kt`

**Interfaces:**
- Consumes: `Context.localizedFor` (Task 1)
- Produces: klíče `notification_workday_end_title`, `notification_workday_end_text`

- [ ] **Step 1: Napiš padající test**

`app/src/test/java/org/clockout/NotificationTextTest.kt`:

```kotlin
package org.clockout

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.clockout.domain.model.AppLanguage
import org.clockout.util.localizedFor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Notifikace vzniká mimo Compose, takže si jazyk musí vyžádat sama.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the reminder speaks czech when the app is czech`() {
        val resources = context.localizedFor(AppLanguage.CZECH).resources

        assertEquals(
            "Padla – čas ukončit práci",
            resources.getString(R.string.notification_workday_end_title)
        )
    }

    @Test
    fun `the reminder speaks english when the app is english`() {
        val resources = context.localizedFor(AppLanguage.ENGLISH).resources

        assertEquals(
            "ClockOut – time to wrap up work",
            resources.getString(R.string.notification_workday_end_title)
        )
    }
}
```

- [ ] **Step 2: Spusť test a ověř, že padá**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationTextTest*"`
Expected: FAIL — `Unresolved reference: notification_workday_end_title`.

- [ ] **Step 3: Doplň klíče do obou strings.xml**

Sekce `<!-- Notifications -->`, hodnoty doslova z `NotificationReceiver.kt:87-95`:

```xml
<!-- values/strings.xml -->
<string name="notification_workday_end_title">ClockOut – time to wrap up work</string>
<string name="notification_workday_end_text">Take 2 minutes to pause and close your workday.</string>

<!-- values-cs/strings.xml -->
<string name="notification_workday_end_title">Padla – čas ukončit práci</string>
<string name="notification_workday_end_text">Zastav se na 2 minuty a vědomě uzavři dnešní pracovní den.</string>
```

- [ ] **Step 4: Přepiš `NotificationReceiver`**

Nahraď oba `when (language)` bloky (řádky 87-95):

```kotlin
val resources = context.localizedFor(language).resources
val title = resources.getString(R.string.notification_workday_end_title)
val text = resources.getString(R.string.notification_workday_end_text)
```

Přidej `import org.clockout.util.localizedFor`.

- [ ] **Step 5: Spusť testy**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS včetně `ReminderPolicyTest`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/org/clockout/receiver/NotificationReceiver.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml \
        app/src/test/java/org/clockout/NotificationTextTest.kt
git commit -m "refactor: text notifikace pochází z resources"
```

---

### Task 8: Smazání AppStrings

**Files:**
- Delete: `app/src/main/java/org/clockout/presentation/util/AppStrings.kt`
- Modify: `app/src/test/java/org/clockout/ExampleUnitTest.kt:7,45-52`

**Interfaces:**
- Consumes: vše z Tasků 1-7
- Produces: nic

- [ ] **Step 1: Ověř, že už `AppStrings` nikdo nevolá**

Run: `grep -rn "AppStrings\|strings\." app/src/main --include=*.kt`
Expected: žádný výsledek kromě případných shod v komentářích. Pokud něco zbylo, doplň to podle vzoru příslušného tasku dřív, než budeš pokračovat.

- [ ] **Step 2: Přepiš `ExampleUnitTest.testAppStringsLocalization`**

Test na řádcích 45-52 instancuje `AppStrings`. Nahraď ho čtením resources:

```kotlin
    @Test
    fun testStringsAreLocalized() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val en = context.localizedFor(AppLanguage.ENGLISH).resources
        val cs = context.localizedFor(AppLanguage.CZECH).resources

        assertEquals("ClockOut", en.getString(R.string.app_name))
        assertEquals("Padla", cs.getString(R.string.app_name))

        assertEquals("About ClockOut", en.getString(R.string.settings_about_title))
        assertEquals("O aplikaci Padla", cs.getString(R.string.settings_about_title))
    }
```

Uprav importy: pryč `org.clockout.presentation.util.AppStrings`, přibude `org.clockout.util.localizedFor`, `android.content.Context` a `androidx.test.core.app.ApplicationProvider`.

- [ ] **Step 3: Smaž `AppStrings.kt`**

```bash
git rm app/src/main/java/org/clockout/presentation/util/AppStrings.kt
```

Tím zmizí i šest mrtvých členů (`closeRitualDialogTitle`, `closeRitualDialogText`, `closeRitualContinue`, `buttonCompleteRitual`, `languageSectionDesc`, `soundStyleDesc`), které do XML nepřešly.

- [ ] **Step 4: Ověř úplnost obou překladů**

Run:
```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-cs/strings.xml | sort)
```
Expected: žádný rozdíl — každý klíč existuje v obou jazycích.

- [ ] **Step 5: Postav a otestuj vše**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL, všechny testy zelené, screenshoty beze změny.

- [ ] **Step 6: Ruční kontrola v aplikaci**

Spusť aplikaci, v nastavení přepni jazyk mezi „Dle systému", „English" a „Čeština" a zkontroluj, že se texty mění okamžitě na všech čtyřech obrazovkách včetně názvu sekce jazyka a záhlaví navigace na tabletu.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: AppStrings mizí, překlady žijí ve strings.xml"
```
