# Agent instructions — clockout-app
<!-- Last reviewed: September 10, 2026 -->

This file is read by AI coding agents (Codex, Gemini, etc.). For Claude-specific overrides see `CLAUDE.md`.

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

Released on Google Play as <https://play.google.com/store/apps/details?id=org.clockout>.

Version 1.0.3 (`versionCode` 1000301). `minSdk` 26, `targetSdk` 37, `compileSdk` 37.1. AGP 9.4.0, Kotlin 2.4.20, Compose BOM 2026.08.00, Robolectric 4.16.1, Roborazzi 1.74.0, Play App Update 2.1.0, Play Review 2.0.2.

All data stays on the device — Room for ritual logs, DataStore for preferences. No accounts, no cloud sync, no ads. The app ships **without the `INTERNET` permission**; Play In-App Update and Play In-App Review both reach the Play Store over IPC, not over the network, and the "Rate the app" button hands a `market://` intent to the Play app (or a browser) rather than fetching anything itself.

## Module layout

Single Gradle module, `:app` (root project name `Padla`), package `org.clockout`:

| Package | Responsibility |
|---|---|
| `data/local`, `data/local/db`, `data/repository` | DataStore preferences, Room database, DAO and entity mappers, repository implementations, question catalog, custom-sound storage, legacy-log migration |
| `domain/model`, `domain/usecase` | Pure Kotlin models and use cases. **No `R` references, no androidx imports.** |
| `presentation/{home,history,ritual,settings}` | Compose screens with their state holders |
| `presentation/components`, `presentation/util` | Shared composables, including the two breathing visuals and the closure bulb; localization provider and enum → resource mapping |
| `notification`, `receiver` | Reminder scheduling and the broadcast receiver that fires it |
| `sound`, `update`, `review`, `ui/theme`, `util` | Ambient sound playback, in-app update flow, Play in-app review flow, Material 3 theme, context and window-chrome helpers |

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

Only `closure_lamp_off.png` and `closure_lamp_on.png` are tracked in git. Everything under `app/src/test/screenshots/formfactor/**` is **git-ignored and recorded per machine**: a fresh clone has no baselines there, and every test in `org.clockout.screenshot` fails with "original file was not found" until

```bash
./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"
```

runs once. That also means a stale baseline there is a local condition, never something a commit can fix for anyone else.

`PhoneNavigationTest` and `TabletNavigationTest` render the whole `PadlaApp` against real DataStore and Room, so their captures contain today's question, which rotates by day-of-year. A baseline recorded on one day drifts on the next — re-record those two when the date rolls over rather than reading it as a regression.

Both also fail outright **at the weekend**: `TabletNavigationTest."the ritual keeps the whole window and hides the rail"` clicks `start_ritual_button`, which the home screen does not offer on a Saturday or Sunday while `includeWeekends` is off, and the thrown assertion then perturbs the shared process enough that the sibling `PhoneNavigationTest` capture can catch the week overview before its flow has emitted ("0 of 0 days closed" instead of "0 of 5"). Run either class on its own and both pass. Check the day of the week before treating either as a regression. Every other capture renders a single screen from a fixed fixture and is deterministic; `ClosureStepScreenshotTest` and the four `FormFactorScreenshotTest` subclasses are the gate worth trusting.

A test that renders a screen directly must wrap it in `AppLocalization(language)`, otherwise strings resolve against the test's default locale instead of the app's chosen language. `FormFactorScreenshotTest.captureScreen` does this for every capture it takes and accepts the language as a parameter.

A test that measures **text** needs `@GraphicsMode(GraphicsMode.Mode.NATIVE)` as well. Without it Robolectric does not lay text out at all: every string comes back about nine pixels wide, so an assertion about a label fitting its container passes whatever the real layout does. `TextLayoutResult.hasVisualOverflow` is no use either — it reads true for a label with room to spare. Assert on `lineCount` and the laid-out width against the constraint instead.

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

The breathing step draws `BreathingAurora` (`presentation/components`): an organic blob built from eight anchors bent into a closed bezier, filled with three phase-shifted wave layers that rise on the inhale and fall on the exhale, trailed by two lagging copies of its own outline. Two things about it are load-bearing. The layers are listed and drawn **from the highest surface down to the lowest**, because each one fills to the bottom of the blob — paint a lower layer last and it covers the band the layer above it was meant to leave visible, and the water flattens into a single slab. And the water never drains completely: below roughly 0.62 of the radius the lower bands fall outside the blob and clip away.

Its rhythm comes from `rememberBreathCycle()`, a linear 0f..1f over `BREATH_CYCLE_MILLIS`, turned into depth by `breathCurve()` — a raised cosine. That shape stalls at both turning points, moves fastest halfway through, and closes its loop without a seam, which an eased tween played in `RepeatMode.Reverse` does not: that combination made the exhale end in a jolt and left the phase to be guessed from the scale rather than read off the turning point. `isInhalePhase()` splits the cycle at 0.5, so the guidance text and the haptic tick land exactly where the breath turns. `BreathCurveTest` pins all of it. Everything the visual draws is a function of the cycle alone — no randomness — which is what lets the screenshot tests capture it by advancing the clock by hand.

Which visual the step draws is a Settings choice. `BreathingStyle` (`domain/model`) holds the options, `BreathingVisual` (`presentation/components`) is the single call that dispatches between `BreathingPulse` — the concentric rings the app shipped with — `BreathingAurora` and `BreathingRipple`. All three take the same cycle and centre the same content, so adding a style is a composable, a branch, an enum entry and two strings. `CLASSIC_CIRCLE` is the fallback in `fromId`, so an install predating the setting keeps the visual it already had rather than changing appearance on an update. `BreathingPulse` runs on `breathCurve` like everything else: what the classic style preserves is the look, not the old easing's jolt.

`BreathingRipple` spaces its rings by index and slides the whole train outwards with the breath. The obvious alternative — each ring reading `breathCurve` at its own lag, the way the aurora's echoes do — is wrong here and looks right at the turning points, which is how it survives a hand check: the curve is periodic, so the ring furthest behind wraps into the *previous* breath and re-emerges from the centre ahead of rings that should be inside it. Spacing by index cannot reorder the train. A ring past the rim fades out rather than wrapping round, and its alpha falls cubically rather than linearly, because a linear fall leaves it half gone by the halfway mark and the cool end of the palette it travels through is then never actually seen.

The picker is `BreathingStyleActivity`, an Activity of its own rather than a fifth `Screen`. Navigation in `PadlaApp` is a hand-rolled sealed interface whose saver writes screens down as plain integers, and every destination in it is reachable from the navigation rail; a picker hanging off Settings is a peer of neither Home nor History. As a separate Activity it gets the system back stack and state restoration for free and the rail is simply not part of it. The cost is that it has to build its own root: `applyFrostedWindowChrome()` (`util/ActivityChrome.kt`, shared with `MainActivity`) for the transparent system bars, and its own `AppLocalization` wrapper, with `rememberRitualRepository()` (`presentation/RitualDependencies.kt`, also shared with `PadlaApp`) called above it so DataStore and Room never see a locale-overridden context.

The label in the middle of the pill (`breath_label`) shrinks to fit rather than sitting at a fixed size. The pill's width is fixed while the label is in `sp`, so at a large system font setting the word wrapped and the round edge cut the ends off both lines — the Czech "Nádech..." rendered as "ádech." at 200%. `TextAutoSize.StepBased` with `maxLines = 1` keeps it whole and on one line at every font scale, and `BREATH_LABEL_INSET` keeps it clear of the curve. `BreathLabelFitsTest` pins it in both languages at 100% and 200%.

`BreathingStyleScreen` shows every style running rather than described — the choice is entirely visual — with one large preview above a grid of tiles. The rows are chunked and laid out by hand, and a short last row is padded with empty weights. Neither ready-made container manages it: a lazy grid measures its items with unbounded height, so they can never share one, and `FlowRow` either stretches a lone tile across its row (given a weight) or refuses to seat two tiles whose measured widths sum to the row exactly (given a width instead). Tile *height* comes from `DESCRIPTION_LINES`, which reserves the same text block in every tile and needs no help from the container at all — `fillMaxRowHeight` settles the row height before the text has wrapped and clips the longer description. Czech runs to three lines where English runs to two, so a new style's description has to fit three lines in both.

`PadlaApp` owns navigation, builds `RitualPreferences` and the Room database from the **original** context (above `AppLocalization`, so DataStore and Room never receive a locale-overridden context), and exposes an `overlay` slot where `MainActivity` injects `AppUpdateBanner`.

`QuestionsData` loads the entire question catalog for **both** languages at startup from string arrays; `getTodayQuestion(dayOfYear, year)` then walks a permutation reshuffled once per year, so the day's question is stable and the whole pool is used before anything repeats.

Reminders are scheduled with `AlarmManager.setAndAllowWhileIdle` — inexact, but it fires in Doze — and are re-registered after reboot. `ReminderPolicy` and `RitualDayPolicy` decide whether a given day takes part, honouring the weekend setting.

Asking for a Play Store review is governed by `ReviewPromptPolicy` (`domain/model`): at least ten finished rituals behind the user, a mood of `SATISFIED` or `CALM` on the ritual just logged, and never more than once. A bad day only postpones the question — the next good one picks it up. `ShouldRequestReviewUseCase` applies the policy and marks the question asked; `RitualViewModel` raises a one-shot `shouldRequestReview` flag, `PadlaApp` turns it into its `onRequestReview` callback, and `MainActivity` supplies `InAppReviewController`, which needs an Activity. The request goes out on the completion card, not on the way home. Settings also carries a plain "Rate the app" card, which opens the Play listing through `Context.openPlayStoreListing()` — Play exposes no public deep link straight to the review form.

Wide windows switch to a navigation rail and multi-column layouts via `currentWindowAdaptiveInfo()`, which is a further reason `LocalContext` must stay the Activity.

## Persistence and backup

Everything the user sets or records stays on the device and survives an app update untouched: DataStore (`padla_preferences`) holds every Settings value, Room holds the ritual logs, and imported audio sits in `filesDir/custom_sounds/`. Installing a new version clears none of it.

`RitualPreferences` takes its `DataStore` as a constructor parameter, with a `Context` convenience constructor for the app. `preferencesDataStore` caches one instance per process, so tests that went through the context would share the ritual counter and the review flag and become order-dependent; `ReviewPromptFlowTest` passes its own store instead.

The chosen breathing visual lives in DataStore under `breathing_style`, so it rides along with every other Settings value on both a cloud backup and a device transfer.

The ritual counter (`ritual_completed_count`) and the review flag (`review_prompt_shown`) are bumped inside `RitualRepositoryImpl.logRitual`, which is also why the counter is immune to Room collapsing several rituals logged on one day into a single row. Both deliberately survive "Clear data and history": that clears the history, not the fact that the app has already had its one chance to ask for a review.

Surviving a *reinstall* is Android Auto Backup's job, and the two rule files split on purpose:

| Data | Cloud backup | Device transfer |
|---|---|---|
| `clockout.db` — ritual logs | yes | yes |
| `datastore/` — every Settings value | yes | yes |
| `custom_sounds/` — imported audio | **no** | yes |

Auto Backup allows 25 MB of cloud data per app and backs up **nothing at all** once that is exceeded, while `CustomSoundStorage` permits ten files of 25 MB each. Putting the audio in the cloud backup would therefore trade away the ritual history. Device-to-device transfer has no such ceiling, so the files ride along there. `backup_rules.xml` (API ≤ 30) lists no sounds either, because those releases have no device transfer and the file feeds only the cloud backup. `BackupRulesTest` pins all of it.

A restored cloud backup consequently holds sound metadata whose file is gone. `RitualRepositoryImpl` resolves that on every read through its injected `soundFileExists` predicate — `PadlaApp` supplies the real file check — and stamps `CustomSound.isAvailable`, which is never serialized. `SoundChoice.fromId` treats an unavailable sound like a deleted one and falls back to the default style, so playback never silently fails. Settings keeps the entry visible under its old name, describes it as missing and opens the picker when tapped; `SettingsViewModel.replaceCustomSound` stores the new file under the **old id**, so an existing selection stays valid, and does not count the entry being replaced against the ten-sound limit.

Auto Backup is best effort: it needs backup enabled and a Google account, and runs roughly daily while charging on Wi-Fi. An update loses nothing; a reinstall restores only what a backup managed to capture.

## Dependency management

All versions go through the version catalog at `gradle/libs.versions.toml` — never hardcode a version in a build file. Several dependencies are deliberately commented out in `app/build.gradle.kts` (Retrofit, OkHttp, Moshi, Firebase AI, App Check); leave them that way unless a feature genuinely needs them. Re-enabling any of them would also mean adding the `INTERNET` permission the app currently does without. The Firebase BOM and the `google-services` plugin are applied, but no Firebase library is actually enabled.

## Release signing

`app/build.gradle.kts` reads `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` from `local.properties` (git-ignored), falling back to environment variables of the same name. When no keystore is found the release signing config is not created at all, so a clean clone still configures.

## What NOT to do

- Do not commit, stage, or push without explicit approval in the same turn.
- Do not add `custom_sounds/` to the cloud backup — the 25 MB quota would drop the whole backup, ritual history included.
- Do not persist `CustomSound.isAvailable` — it is resolved against the file system on every read.
- Do not reset the ritual counter or the review flag when clearing data — that would hand out a second review request on every wipe.
- Do not override `LocalContext` to localize strings.
- Do not resolve resources by name at runtime.
- Do not remove `enableSplit = false` from the bundle config.
- Do not reorder `WAVE_LAYERS` in `BreathingAurora` by depth — the list is in draw order, top surface first, and sorting it the other way collapses the water into one flat slab.
- Do not make `AURORA` the fallback in `BreathingStyle.fromId` — an update would then change the look of the ritual for every existing install without being asked.
- Do not drive a ring train in `BreathingRipple` off a per-ring phase lag — the breath curve is periodic and the ring furthest behind will wrap into the previous breath and overtake from the centre.
- Do not give the breathing pill's label a fixed font size — the pill's width is fixed and the label is not, so a large system font setting cuts the word in half.
- Do not rebuild the window chrome or the repository inside a new Activity by hand — call `applyFrostedWindowChrome()` and `rememberRitualRepository()`, and keep the repository call above `AppLocalization`.
- Do not re-record screenshot baselines to make a failing test pass — establish first whether the change to the rendering was intended.
- Do not add strings to only one of the two locale files.
- Do not put user-facing text, `R` references, or androidx imports into `domain/`.
- Do not hardcode dependency versions outside the version catalog.
