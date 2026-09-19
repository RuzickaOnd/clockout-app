# Firebase Crashlytics and Analytics with opt-in consent

Date: 2026-09-18
Status: implemented on 2026-09-18 (branch `develop`, uncommitted at time of writing)

## Problem

The app has been on Google Play since 1.0.3 and reports nothing. Firebase shows no
crashes and no analytics because **no Firebase SDK is in the app at all**:

- `app/build.gradle.kts` pulls in `platform(libs.firebase.bom)` only. A BOM is a
  version table; it contributes no code. `firebase-analytics` and
  `firebase-crashlytics` exist in `gradle/libs.versions.toml` but are never
  referenced from `dependencies {}`.
- The Crashlytics Gradle plugin is declared `apply false` in the root
  `build.gradle.kts` and never applied in `:app`.
- The app has no `INTERNET` permission, so nothing could be uploaded regardless.
- `app/google-services.json` is git-ignored and was created locally on
  2026-09-18. The released build never had it, and
  `googleServices { missingGoogleServicesStrategy = WARN }` let that build pass.

Firebase project `padla-87289`, app id `1:651677165598:android:6d69b6c0530a597175e60f`,
package `org.clockout` — the configuration itself is correct.

## Goal

Report crashes and measure how far users get through the ritual, without breaking
the promise the app is built on. Concretely: nothing leaves the device until the
user has said yes, and the answer is reversible.

## Non-goals

- No advertising id, no ad SDK, no user-level identifiers of our own.
- No remote config, no A/B testing, no Firebase Auth or Firestore. They stay
  commented out in `app/build.gradle.kts`.
- No logging of ritual *content* — never the question text, never the note. Only
  that a step happened.

## Decisions

### Consent has three states, not two

`TelemetryConsent` (`domain/model`): `UNKNOWN`, `GRANTED`, `DENIED`.

A boolean cannot distinguish "not asked yet" from "said no". With a boolean the
dialog would either reappear forever or never appear at all.

Stored in DataStore under `telemetry_consent` as the enum **id string**, matching
how `breathing_style` and `app_language` are stored, so reordering the enum cannot
change the meaning of a stored value. `fromId` falls back to `UNKNOWN`.

### Consent survives "Clear data and history"

Same treatment as `review_prompt_shown` and `ritual_completed_count`. Clearing the
history is not a withdrawal of consent, and re-asking after every wipe would be
noise. Withdrawal happens through the Settings switch, which is the only control
that writes `DENIED` after the dialog is gone.

### Collection is off in the manifest, not just in our code

```xml
<meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
<meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false" />
```

Both SDKs start collecting when `FirebaseInitProvider` runs, which happens during
application startup — long before the user can see a dialog. Without these two
entries the first session of every install would be collected and uploaded before
consent existed, and the opt-in would be decoration. Runtime code turns collection
on only after `GRANTED`.

This is the single most important line in this document.

### A domain interface, not Firebase calls at the call sites

```
domain/telemetry/AnalyticsTracker.kt      pure Kotlin interface
domain/telemetry/AnalyticsEvent.kt        sealed interface, the event catalog
domain/telemetry/UserProperty.kt          enum of property names
domain/telemetry/ConsentGatedTracker.kt   the gate, wrapping any tracker
domain/telemetry/NoOpAnalyticsTracker.kt  tests and previews
telemetry/FirebaseAnalyticsTracker.kt     maps the catalog onto Firebase
presentation/TelemetryDependencies.kt     rememberAnalyticsTracker()
```

The gate is a **decorator in `domain/`**, not a flag inside the Firebase tracker.
That keeps it pure Kotlin and testable without a Firebase instance, which a gate
living inside `FirebaseAnalyticsTracker` would not be. It stops our own events;
`setCollectionEnabled` is passed through to the Firebase tracker as well, because
the SDK's automatic events (`session_start`, `screen_view`) never travel through
our interface and only the SDK flag can stop those.

`domain/` keeps its rule: no `R`, no androidx imports, pure Kotlin.

Calling Firebase directly from the ViewModels was rejected. `RitualViewModel` and
`SettingsViewModel` both have unit tests that run under Robolectric; static Firebase
accessors in them would force either a Firebase initialization in every test or
static mocking. The interface also keeps every event name in one readable list
instead of scattering string literals across the presentation layer.

The tracker owns the consent gate: `track()` is a no-op until collection is
enabled, so no call site ever tests for consent.

## Event catalog

Firebase limits: event names ≤ 40 characters, event parameter names ≤ 40, **user
property names ≤ 24** and their values ≤ 36. All must be snake_case, start with a
letter, and the prefixes `firebase_`, `google_`, `ga_` are reserved. Names that
break the rules are dropped silently at runtime, which is why `AnalyticsEventTest`
asserts them rather than leaving it to review. The 24-character ceiling on user
properties is the tight one — `include_weekends` (16) is the longest we declare.

| Event | Parameters | Raised in |
|---|---|---|
| `ritual_started` | — | `RitualViewModel.startSession` |
| `ritual_step_completed` | `step` = `question` / `breathing` / `closing` / `log` | `RitualViewModel` on each step transition |
| `ritual_completed` | `mood` (`MoodTag.id`), `days_closed_week` (Int) | `RitualViewModel` after `LogMoodUseCase` |
| `ritual_abandoned` | `step` (where the user left) | `RitualViewModel.stopSession`, only before the log step |

Settings are **user properties**, not events, so every metric can be broken down by
them in the Firebase console — something events cannot do:

| Property | Value |
|---|---|
| `breathing_style` | `BreathingStyle.id` |
| `sound_enabled` | `"true"` / `"false"` |
| `app_language` | `AppLanguage` code |
| `include_weekends` | `"true"` / `"false"` |

Set from a `LaunchedEffect` in `PadlaApp` that collects the preference flows and
writes the current values once collection is enabled, and again on every change in
`SettingsViewModel`. `PadlaApp` is the right place because it already holds the
repository and outlives every screen; the `Application` class does not, since
consent may be granted long after startup.

`days_closed_week` is `RitualStreakInfo.completedThisWeekCount`, read after the log
is written so it counts today. The repository has no consecutive-day streak — the
only streak figure it computes is per-week — so the parameter is named for what it
actually holds rather than for the word "streak".

Retention and frequency come from Firebase's own session data; `ritual_completed`
sharpens it. No custom retention event is needed.

## UI

**Consent dialog.** `TelemetryConsentDialog` in `presentation/components`, shown by
`PadlaApp` when consent is `UNKNOWN`. Modal, non-dismissable by back or outside
tap — an unanswered dialog must not resolve to silent collection. Two buttons
("Allow" / "No thanks") and a link to the privacy policy.

**Settings card.** A new section with a switch ("Anonymous diagnostics") and a
privacy-policy link, so a `DENIED` answer is reversible and the policy is reachable
without the dialog.

**Link handling.** `Context.openUrl(url)` in `util/`, a sibling of the existing
`openPlayStoreListing()` in `util/PlayStoreLink.kt`, with the same
`ActivityNotFoundException` fallback chain. Privacy policy:
<https://ruzickaond.github.io/clockout-legal/>

Strings go into **both** `res/values/strings.xml` and `res/values-cs/strings.xml`.

## Testing

| Test | Asserts |
|---|---|
| `TelemetryConsentTest` | default is `UNKNOWN`; `fromId` falls back to `UNKNOWN`; `GRANTED`/`DENIED` round-trip through DataStore; clearing data leaves consent intact |
| `AnalyticsEventTest` | every event and parameter name is snake_case, within the length limits, and carries no reserved prefix |
| `TelemetryGateTest` | the tracker drops events while consent is not `GRANTED` and forwards them once it is |
| `SettingsViewModel` tests | changing a setting updates the matching user property |

Existing tests are protected two ways: `RitualViewModel` takes the tracker as a
constructor parameter defaulting to `NoOpAnalyticsTracker()`, so no existing test
signature changes; and `PhoneNavigationTest` / `TabletNavigationTest`
(`screenshot/AdaptiveNavigationTest.kt`) seed `telemetry_consent` in `@Before`
through `RitualPreferences`, because they render the real `PadlaApp` against the
real DataStore and would otherwise photograph the dialog.

**Screenshot baselines are not re-recorded.** If a capture changes, the cause is
investigated first.

### Known risk — what actually happened

`FirebaseInitProvider` does auto-initialize under Robolectric, but harmlessly: adding the
dependencies and the manifest entries left the whole suite green, and no test manifest was
needed.

The risk surfaced one step later instead. `FirebaseCrashlytics.getInstance()` **throws**
`IllegalStateException: Default FirebaseApp is not initialized` rather than returning a dormant
instance, so the moment `PadlaApp` constructed a `FirebaseAnalyticsTracker`, every Robolectric
test that renders the real app died on it.

The fix is in `rememberAnalyticsTracker`, not in a test manifest: when `FirebaseApp.getApps()` is
empty it returns `NoOpAnalyticsTracker`. That keeps the remedy in production code where it is also
correct — a build assembled without `google-services.json` reports nothing rather than crashing —
instead of hiding the problem from tests alone.

## Documentation

`CLAUDE.md` and `AGENTS.md` are updated in the same commit, as the repo requires:

- Project overview: the sentence stating the app ships without `INTERNET` is no
  longer true and is replaced by a description of what is collected and when.
- A new *Telemetry* section covering the three consent states, the manifest kill
  switch, and the tracker layering.
- *What NOT to do* gains: do not enable collection before `GRANTED`; do not clear
  consent when clearing data; do not call Firebase from a ViewModel; do not log
  ritual content.
- Dependency management: `firebase-analytics` and `firebase-crashlytics` are no
  longer in the commented-out list.

## Outside the codebase

Both are the author's to do; the release must not ship before the first one.

1. **Play Console → Data safety.** Declare crash logs and diagnostics (Crashlytics)
   and app interactions plus approximate location derived from IP (Analytics). An
   incorrect declaration is the usual reason an update is held.
2. **Privacy policy.** The published policy already names "Google Analytics for
   Firebase" and "Firebase Crashlytics", so it needs no rewrite for Play. It is
   English-only while the app is Czech-first; a Czech version is worth adding but
   does not block the release.

## Out of scope for this change

A telemetry kill switch driven from Remote Config, and any event on the settings
screens beyond the four user properties.
