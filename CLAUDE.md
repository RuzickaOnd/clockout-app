# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
<!-- Last reviewed: September 10, 2026 -->

`AGENTS.md` is the same reference for other coding agents. **The two files are always updated together** — see *Keeping documentation current*.

## Code style

- **Act as an expert developer.** Always write clean, robust, and idiomatic Kotlin code.
- **Verify and test implementations.** Always compile, run unit tests, and verify that changes work correctly.
- All code, KDoc, and inline comments must be in **English**. User-facing strings are Czech and English, in resources only.
- Kotlin throughout. No new Java files.
- No unnecessary comments — only add one when the *why* is non-obvious. Comments in this repo explain reasoning, not mechanics.
- **Use the latest stable APIs and patterns.** Verify the approach against current official documentation — do not assume patterns from older code in this repo are still recommended.

## Git — commit, stage, and push policy

**Never commit, stage (git add), or push automatically.** After completing any code change, always stop and explicitly ask the user whether to stage and commit. Only run `git add`, create a commit, or run `git push` after receiving a direct confirmation in that same turn. This applies even when the user previously asked to "stage/commit changes" during an earlier task — treat each action as a new decision requiring fresh approval.

Commit messages are written in **Czech**, with a conventional prefix (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, or the project's own `CL:`).

### Remote access

A single `origin` at `git@github.com:RuzickaOnd/clockout-app.git`. The SSH key is `~/.ssh/id_ed25519` and is passphrase-protected; `~/.ssh/config` loads it from the macOS Keychain, but it must be unlocked once per machine with `ssh-add --apple-use-keychain ~/.ssh/id_ed25519`. Until that happens SSH pushes fail with `Permission denied (publickey)` and the working fallback is an explicit HTTPS URL, which authenticates through the `gh` credential helper.

## Keeping documentation current

Every code change must include an update to the relevant MD file(s) **in the same commit/task**. No exceptions.

**`CLAUDE.md` and `AGENTS.md` are always updated together.** Any change to one must be reflected in the other in the same commit. `AGENTS.md` is the complete standalone reference; `CLAUDE.md` carries the same material for Claude Code, which loads it automatically.

| Type of change | What to update |
|---|---|
| New screen, feature, or significant logic added | Architecture section |
| Existing screen or class refactored / renamed / moved | Affected package descriptions |
| Library added, removed, or version changed | Key stack versions |
| App version bumped (`versionCode` / `versionName`) | Project overview version line |
| Project-wide rule or pattern changed | The relevant section |
| A test gate changes behaviour (new flaky test, baseline re-recorded) | Testing section |

When in doubt: if a future agent reading only the MD files would be misled by the old content, update it.

## Project overview

Padla (Czech) / ClockOut (English) — an Android app for closing the workday with a four-step, roughly three-minute ritual: today's question, 4 s / 4 s guided breathing, a visual lamp-off, and a mood log. Aimed at home-office and hybrid workers who struggle to switch off.

Version 1.0.1 (`versionCode` 1000100). `minSdk` 26, `targetSdk` 37, `compileSdk` 37.1. AGP 9.4.0, Kotlin 2.4.20, Compose BOM 2026.08.00, Robolectric 4.16.1, Roborazzi 1.74.0.

All data stays on the device — Room for ritual logs, DataStore for preferences. No accounts, no cloud sync, no ads. The app ships **without the `INTERNET` permission**; Play In-App Update reaches the Play Store over IPC, not over the network.

## Module layout

Single Gradle module, `:app` (root project name `Padla`), package `org.clockout`:

| Package | Responsibility |
|---|---|
| `data/local`, `data/local/db`, `data/repository` | DataStore preferences, Room database, DAO and entity mappers, repository implementations, question catalog, custom-sound storage, legacy-log migration |
| `domain/model`, `domain/usecase` | Pure Kotlin models and use cases. **No `R` references, no androidx imports.** |
| `presentation/{home,history,ritual,settings}` | Compose screens with their state holders |
| `presentation/components`, `presentation/util` | Shared composables; localization provider and enum → resource mapping |
| `notification`, `receiver` | Reminder scheduling and the broadcast receiver that fires it |
| `sound`, `update`, `ui/theme`, `util` | Ambient sound playback, in-app update flow, Material 3 theme, context helpers |

## Commands

```bash
# Unit and Robolectric tests
./gradlew :app:testDebugUnitTest

# A single test class or pattern
./gradlew :app:testDebugUnitTest --tests "*HomeScreen*"

# Fastest correctness check
./gradlew :app:compileDebugKotlin

# Debug APK
./gradlew :app:assembleDebug

# Lint — abortOnError is on and there is no baseline, so it must stay green
./gradlew :app:lintDebug

# Screenshot tests
./gradlew :app:verifyRoborazziDebug   # compare against baselines
./gradlew :app:recordRoborazziDebug   # rewrite baselines — only for an intended UI change
```

## Testing and screenshot baselines

`testDebugUnitTest` runs the screenshot tests but does **not** compare images — only the `verifyRoborazzi*` tasks do. A green `testDebugUnitTest` therefore says nothing about screenshots.

Not every screenshot failure is a regression:

- `app/src/test/screenshots/formfactor/**` is **git-ignored**. A fresh clone has no baselines there and every `FormFactorScreenshotTest` fails with "original file was not found" until `recordRoborazziDebug` runs once.
- `greeting.png` is tracked but **stale on `main`** — it fails every run (same text, different background).
- `PhoneNavigationTest` and `TabletNavigationTest` are **flaky**: they render the whole `PadlaApp` with real DataStore and Room, so the async streak/week load races the capture, and their content depends on the calendar date (the daily question rotates by day-of-year). Three runs on identical code have produced 3, 1 and 2 failures.
- The deterministic gate worth trusting: `ClosureStepScreenshotTest` plus the four `FormFactorScreenshotTest` subclasses, which render single screens with fixed state.

A test that renders a screen directly must wrap it in `AppLocalization(language)`; otherwise strings resolve against the test's default locale instead of the app's chosen language.

## Strings and localization — non-negotiable rules

The app switches language **in-app, independently of the system locale**. All user-facing text lives in `res/values/strings.xml` and `res/values-cs/strings.xml`, and both files always hold the same key set.

- **`AppLocalization` (`presentation/util/Localization.kt`) provides `LocalResources`, never `LocalContext`.** This is load-bearing: `stringResource()` reads `LocalResources`, which merely defaults to `LocalContext.current.resources`. Overriding `LocalContext` with a `createConfigurationContext` result detaches the Activity from the context chain and silently breaks `rememberLauncherForActivityResult`, `LocalActivity`, and `androidx.window` fold detection — that last one tests `context is Activity` and does not unwrap a `ContextWrapper`, so even wrapping does not save it. Do not reintroduce a `LocalContext` override.
- Outside Compose (`QuestionsData`, `NotificationReceiver`) use `Context.localizedFor(language)` from `util/LocalizedContext.kt`.
- **Static `R.string.*` / `R.array.*` / `R.plurals.*` only.** The release build runs R8 with `isShrinkResources = true`, so anything reached through `Resources.getIdentifier` or a name → id map is stripped and crashes at runtime.
- **`bundle { language { enableSplit = false } }` must stay in `app/build.gradle.kts`.** AGP splits AABs by language by default; Play would install only the device-locale split and the in-app switcher would silently do nothing.
- Domain enums (`MoodTag`, `MeditationSoundStyle`, `SoundChoice`, `LanguageOption`) hold **no strings and no `R` references**. The enum → resource-id mapping lives in `presentation/util/Localization.kt`.
- `RitualQuestion` is the deliberate exception: it carries `textEn` and `textCs` together, because history must re-render a phrase stored in one language while the app runs in the other. `RitualLogLocalization` performs that matching.
- Endonyms ("English", "Čeština") are intentionally untranslated.
- Czech plurals need `one` / `few` / `many` / `other`; lint fails on a missing `many` even though integer counts never select it.

## Architecture

`RitualStep` (`domain/model`) is a sealed interface with four steps — Question, Breathing, Closing, Log — driven from `RitualScreen` through `RitualIntent`.

`PadlaApp` owns navigation, builds `RitualPreferences` and the Room database from the **original** context (above `AppLocalization`, so DataStore and Room never receive a locale-overridden context), and exposes an `overlay` slot where `MainActivity` injects `AppUpdateBanner`.

`QuestionsData` loads the entire question catalog for **both** languages at startup from string arrays; `getTodayQuestion(dayOfYear, year)` then walks a permutation reshuffled once per year, so the day's question is stable and the whole pool is used before anything repeats.

Reminders are scheduled with `AlarmManager.setAndAllowWhileIdle` — inexact, but it fires in Doze — and are re-registered after reboot. `ReminderPolicy` and `RitualDayPolicy` decide whether a given day takes part, honouring the weekend setting.

Wide windows switch to a navigation rail and multi-column layouts via `currentWindowAdaptiveInfo()`, which is a further reason `LocalContext` must stay the Activity.

## Dependency management

All versions go through the version catalog at `gradle/libs.versions.toml` — never hardcode a version in a build file. Several dependencies are deliberately commented out in `app/build.gradle.kts` (Retrofit, OkHttp, Moshi, Firebase AI, App Check); leave them that way unless a feature genuinely needs them. Re-enabling any of them would also mean adding the `INTERNET` permission the app currently does without. The Firebase BOM and the `google-services` plugin are applied, but no Firebase library is actually enabled.

## Release signing

`app/build.gradle.kts` reads `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` from `local.properties` (git-ignored), falling back to environment variables of the same name. When no keystore is found the release signing config is not created at all, so a clean clone still configures.

## What NOT to do

- Do not commit, stage, or push without explicit approval in the same turn.
- Do not override `LocalContext` to localize strings.
- Do not resolve resources by name at runtime.
- Do not remove `enableSplit = false` from the bundle config.
- Do not re-record screenshot baselines to make a failing test pass — establish first whether the change to the rendering was intended.
- Do not add strings to only one of the two locale files.
- Do not put user-facing text, `R` references, or androidx imports into `domain/`.
- Do not hardcode dependency versions outside the version catalog.
