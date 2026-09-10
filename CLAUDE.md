# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Padla (Czech) / ClockOut (English) is a single-module Android app: a four-step, ~3-minute ritual that closes the workday. Everything is stored locally — Room for ritual logs, DataStore for preferences, no accounts and no network sync.

## Commands

```bash
./gradlew :app:testDebugUnitTest                      # unit + Robolectric tests
./gradlew :app:testDebugUnitTest --tests "*HomeScreen*"   # one test class or pattern
./gradlew :app:compileDebugKotlin                     # fastest correctness check
./gradlew :app:assembleDebug
./gradlew :app:lintDebug                              # abortOnError is on; no lint baseline exists
```

Screenshot tests (Roborazzi):

```bash
./gradlew :app:verifyRoborazziDebug   # compares against baselines
./gradlew :app:recordRoborazziDebug   # rewrites baselines — only when a UI change is intended
```

`testDebugUnitTest` alone runs the screenshot tests but does **not** compare images; only the `verifyRoborazzi*` tasks do. A green `testDebugUnitTest` therefore says nothing about screenshots.

## Screenshot baselines: what is and isn't a real failure

- `app/src/test/screenshots/formfactor/**` is **git-ignored**, so a fresh clone has no baselines there and every `FormFactorScreenshotTest` fails with "original file was not found" until `recordRoborazziDebug` runs once.
- `greeting.png` is tracked but **stale on `main`** — it fails every run (same text, different background). Pre-existing, unrelated to whatever you are changing.
- `PhoneNavigationTest` / `TabletNavigationTest` are **flaky**: they render the whole `PadlaApp` with real DataStore and Room, so the async streak/week load races the capture, and their content depends on the calendar date (the daily question rotates by day-of-year). Three runs on identical code have produced 3, 1 and 2 failures.
- The deterministic gate worth trusting: `ClosureStepScreenshotTest` plus the four `FormFactorScreenshotTest` subclasses, which render single screens with fixed state.

## Localization architecture

The app switches language **in-app, independently of the system locale**, and all strings live in `res/values/strings.xml` + `res/values-cs/strings.xml`. This constrains how strings may be reached:

- `AppLocalization` (`presentation/util/Localization.kt`) provides **`LocalResources`**, not `LocalContext`. This is deliberate and load-bearing: `stringResource()` reads `LocalResources`, which merely defaults to `LocalContext.current.resources`. Overriding `LocalContext` with a `createConfigurationContext` result detaches the Activity from the context chain and silently breaks `rememberLauncherForActivityResult`, `LocalActivity`, and `androidx.window` fold detection (which tests `context is Activity` and does not unwrap `ContextWrapper`). Do not reintroduce a `LocalContext` override.
- Outside Compose (`QuestionsData`, `NotificationReceiver`) use `Context.localizedFor(language)` from `util/LocalizedContext.kt`.
- Access resources only through static `R.string.*` / `R.array.*` / `R.plurals.*`. The release build runs R8 with `isShrinkResources = true`, so anything reached via `Resources.getIdentifier` or a name→id map would be stripped and crash at runtime.
- `bundle { language { enableSplit = false } }` in `app/build.gradle.kts` is required: AGP splits AABs by language by default and Play would install only the device-locale split, leaving the in-app switcher unable to find the other language.
- Domain enums (`MoodTag`, `MeditationSoundStyle`, `SoundChoice`, `LanguageOption`) hold **no strings and no `R` references**; the enum → resource-id mapping lives in `presentation/util/Localization.kt`.
- `RitualQuestion` is the deliberate exception: it carries `textEn`/`textCs` together, because history has to re-render a phrase stored in one language while the app runs in the other. `RitualLogLocalization` does that matching.
- Endonyms ("English", "Čeština") are intentionally not translated.
- Wording is migrated verbatim. The start button says "(2–3 min)" while surrounding copy says three minutes — that inconsistency is intentional; a test pins it.

Tests that render a screen directly must wrap it in `AppLocalization(language)`, otherwise strings resolve against the test's default locale rather than the state's language. `LocalizationProviderTest` is the home for per-string assertions.

## Ritual and data flow

`RitualStep` (`domain/model`) is a sealed interface with four steps — Question, Breathing, Closing, Log — driven from `RitualScreen` through `RitualIntent`. `PadlaApp` owns navigation, builds `RitualPreferences` and the Room database from the **original** context (above `AppLocalization`, so DataStore and Room never receive a locale-overridden context), and hosts an `overlay` slot where `MainActivity` injects `AppUpdateBanner`.

`QuestionsData` loads the whole question catalog for **both** languages at startup from string arrays, then `getTodayQuestion(dayOfYear, year)` walks a permutation reshuffled once per year — so the day's question is stable and the full pool is used before repeating.

Reminders are scheduled with `AlarmManager.setAndAllowWhileIdle` (inexact, but fires in Doze) and re-registered after reboot; `ReminderPolicy` and `RitualDayPolicy` decide whether a given day takes part, honouring the weekend setting.

Wide windows switch to a navigation rail and multi-column layouts; `PadlaApp` calls `currentWindowAdaptiveInfo()` for that, which is another reason `LocalContext` must stay the Activity.

## Release signing

`app/build.gradle.kts` reads `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` from `local.properties` (git-ignored), falling back to environment variables. When no keystore is found the release signing config is not created at all, so clean clones still configure.
