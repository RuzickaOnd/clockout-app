# Firebase Telemetry with Opt-In Consent — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Report crashes through Firebase Crashlytics and measure ritual completion through Firebase Analytics, with nothing collected until the user opts in.

**Architecture:** A pure-Kotlin `AnalyticsTracker` interface in `domain/telemetry` is implemented by `FirebaseAnalyticsTracker` and wrapped by `ConsentGatedTracker`, which drops events until consent is `GRANTED`. Both Firebase SDKs start with collection disabled in the manifest and are switched on at runtime only after the user answers the opt-in dialog. ViewModels talk to the interface and never to Firebase.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, Firebase Analytics + Crashlytics (via `firebase-bom` 34.18.0), Crashlytics Gradle plugin 3.0.8, Robolectric 4.16.1, Roborazzi 1.74.0.

**Spec:** `docs/superpowers/specs/2026-09-18-telemetry-design.md`

## Global Constraints

Copied verbatim from the spec and from `CLAUDE.md`; every task inherits them.

- **Never commit, stage, or push without explicit approval in the same turn.** Each task's commit step means: stop, show the diff, ask. Do not run `git add` or `git commit` until the user says yes in that same turn.
- `domain/` contains **no `R` references and no androidx imports**. Pure Kotlin only.
- All code, KDoc and inline comments in **English**. User-facing strings live only in `res/values/strings.xml` and `res/values-cs/strings.xml`, and **both files always hold the same key set**.
- **Static `R.string.*` only.** The release build runs R8 with `isShrinkResources = true`; a resource reached by name is stripped and crashes at runtime.
- Dependency versions go through `gradle/libs.versions.toml`. Never hardcode a version in a build file.
- **Do not re-record screenshot baselines to make a failing test pass.** Establish first whether the rendering change was intended.
- Firebase naming limits: event name ≤ 40 chars, event parameter name ≤ 40, **user property name ≤ 24**, user property value ≤ 36. snake_case, must start with a letter, and the prefixes `firebase_`, `google_`, `ga_` are reserved.
- Never log ritual **content** — not the question text, not the user's answer, not the note. Only that a step happened.
- `CLAUDE.md` and `AGENTS.md` are updated **together**, in the same commit as the code they describe.
- Privacy policy URL: `https://ruzickaond.github.io/clockout-legal/`

## Verification commands

```bash
./gradlew :app:compileDebugKotlin                  # fastest correctness check
./gradlew :app:testDebugUnitTest                   # unit + Robolectric
./gradlew :app:testDebugUnitTest --tests "*Telemetry*"
./gradlew :app:lintDebug                           # abortOnError is on, no baseline
./gradlew :app:verifyRoborazziDebug                # screenshot comparison
```

`testDebugUnitTest` runs the screenshot tests but does **not** compare images — only `verifyRoborazzi*` does.

---

## File Structure

**Created:**

| File | Responsibility |
|---|---|
| `app/src/main/java/org/clockout/domain/model/TelemetryConsent.kt` | The three consent states and `fromId` |
| `app/src/main/java/org/clockout/domain/telemetry/AnalyticsTracker.kt` | The interface every call site uses |
| `app/src/main/java/org/clockout/domain/telemetry/AnalyticsEvent.kt` | Sealed event catalog with parameter names |
| `app/src/main/java/org/clockout/domain/telemetry/UserProperty.kt` | Enum of the four user property names |
| `app/src/main/java/org/clockout/domain/telemetry/RitualStepAnalytics.kt` | `RitualStep` → analytics id, keeping `RitualStep` untouched |
| `app/src/main/java/org/clockout/domain/telemetry/ConsentGatedTracker.kt` | The gate: drops events until enabled |
| `app/src/main/java/org/clockout/domain/telemetry/NoOpAnalyticsTracker.kt` | Default for tests and previews |
| `app/src/main/java/org/clockout/telemetry/FirebaseAnalyticsTracker.kt` | Maps the catalog onto the Firebase SDK |
| `app/src/main/java/org/clockout/presentation/TelemetryDependencies.kt` | `rememberAnalyticsTracker()` |
| `app/src/main/java/org/clockout/presentation/components/TelemetryConsentDialog.kt` | The opt-in dialog |
| `app/src/main/java/org/clockout/util/ExternalLink.kt` | `Context.openUrl(url)` |
| `app/src/test/java/org/clockout/telemetry/RecordingAnalyticsTracker.kt` | Test fake that records calls |
| `app/src/test/java/org/clockout/telemetry/AnalyticsEventTest.kt` | Firebase naming limits |
| `app/src/test/java/org/clockout/telemetry/ConsentGatedTrackerTest.kt` | The gate's behaviour |
| `app/src/test/java/org/clockout/TelemetryConsentPersistenceTest.kt` | Consent through a real DataStore |

**Modified:**

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | nothing — `firebase-analytics`, `firebase-crashlytics` and the `crashlytics` plugin are already declared |
| `app/build.gradle.kts` | apply the Crashlytics plugin; add the two Firebase dependencies |
| `app/src/main/AndroidManifest.xml` | `INTERNET` permission; two collection-disabled meta-data entries |
| `app/src/main/java/org/clockout/data/local/RitualPreferences.kt` | `telemetry_consent` key, flow and setter |
| `app/src/main/java/org/clockout/data/repository/RitualRepository.kt` | `getTelemetryConsent()` / `setTelemetryConsent()` |
| `app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt` | their implementations |
| `app/src/main/java/org/clockout/presentation/PadlaApp.kt` | tracker, consent state, dialog, user properties |
| `app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt` | the four ritual events |
| `app/src/main/java/org/clockout/presentation/settings/SettingsState.kt` | `telemetryConsent` field |
| `app/src/main/java/org/clockout/presentation/settings/SettingsViewModel.kt` | consent setter, user properties on change |
| `app/src/main/java/org/clockout/presentation/settings/SettingsScreen.kt` | the diagnostics card |
| `app/src/main/res/values/strings.xml`, `values-cs/strings.xml` | the new strings, both files |
| `app/src/test/java/org/clockout/screenshot/AdaptiveNavigationTest.kt` | seed consent so the dialog is not photographed |
| `CLAUDE.md`, `AGENTS.md` | the Telemetry section and the new prohibitions |

---

## Task 1: Firebase dependencies and the kill switch

The riskiest step, so it goes first and on its own: `FirebaseInitProvider` auto-initializes under Robolectric, and if that breaks the suite we want to know before any application code exists.

**Files:**
- Modify: `app/build.gradle.kts` (plugins block; dependencies block)
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing
- Produces: a build in which `com.google.firebase.analytics.FirebaseAnalytics` and `com.google.firebase.crashlytics.FirebaseCrashlytics` resolve, with collection disabled at startup.

- [ ] **Step 1: Apply the Crashlytics plugin**

In `app/build.gradle.kts`, add the last line to the existing `plugins` block:

```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.crashlytics)
}
```

`libs.plugins.crashlytics` already exists in `gradle/libs.versions.toml:120` and is already declared `apply false` in the root `build.gradle.kts`. Nothing to add to the catalog.

- [ ] **Step 2: Add the two dependencies**

In the `dependencies` block of `app/build.gradle.kts`, immediately after `implementation(platform(libs.firebase.bom))`:

```kotlin
  implementation(platform(libs.firebase.bom))
  // Crash reporting and product analytics. Both start with collection disabled in the manifest and
  // are switched on only after the user opts in - see TelemetryConsent.
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)
```

Leave every commented-out dependency exactly as it is.

- [ ] **Step 3: Add the permission and the kill switch**

In `app/src/main/AndroidManifest.xml`, add the permission next to the existing two:

```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.INTERNET" />
```

and these two entries as the **first children** of `<application>`, before `<activity>`:

```xml
        <!-- Both SDKs would otherwise start collecting when FirebaseInitProvider runs, which is
             before the opt-in dialog can be shown. Runtime code turns collection on only after the
             user has granted consent; without these two entries the first session of every install
             would be uploaded and the opt-in would be decoration. -->
        <meta-data
            android:name="firebase_analytics_collection_enabled"
            android:value="false" />
        <meta-data
            android:name="firebase_crashlytics_collection_enabled"
            android:value="false" />
```

- [ ] **Step 4: Verify the build configures and compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. A failure here is almost certainly a missing `app/google-services.json`; it exists and is git-ignored, so a fresh clone needs it downloaded from the Firebase console for project `padla-87289`.

- [ ] **Step 5: Verify the existing test suite still passes — this is the risk check**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS, the same set as before this task.

If it fails with a Firebase initialization error, add to `app/src/test/AndroidManifest.xml` (create it if absent) and record the fact in the spec's "Known risk" section:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <application>
        <provider
            android:name="com.google.firebase.provider.FirebaseInitProvider"
            android:authorities="${applicationId}.firebaseinitprovider"
            tools:node="remove" />
    </application>
</manifest>
```

Then re-run and confirm PASS before continuing.

- [ ] **Step 6: Verify lint is still green**

Run: `./gradlew :app:lintDebug`
Expected: BUILD SUCCESSFUL. `abortOnError` is on and there is no baseline.

- [ ] **Step 7: Commit — ask first**

Show the diff and ask for approval. Only after an explicit yes in the same turn:

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat: přidána Firebase Analytics a Crashlytics se sběrem vypnutým v manifestu"
```

---

## Task 2: Consent as a persisted three-state value

**Files:**
- Create: `app/src/main/java/org/clockout/domain/model/TelemetryConsent.kt`
- Create: `app/src/test/java/org/clockout/TelemetryConsentPersistenceTest.kt`
- Modify: `app/src/main/java/org/clockout/data/local/RitualPreferences.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepository.kt`
- Modify: `app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces:
  - `enum class TelemetryConsent(val id: String)` with `UNKNOWN`, `GRANTED`, `DENIED` and `companion object { fun fromId(id: String?): TelemetryConsent }`
  - `RitualPreferences.telemetryConsentFlow: Flow<TelemetryConsent>` and `suspend fun setTelemetryConsent(consent: TelemetryConsent)`
  - `RitualRepository.getTelemetryConsent(): Flow<TelemetryConsent>` and `suspend fun setTelemetryConsent(consent: TelemetryConsent)`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/clockout/TelemetryConsentPersistenceTest.kt`:

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
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.MoodTag
import org.clockout.domain.model.TelemetryConsent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Consent is the one preference that must never default to "yes", and the one that must survive a
 * wipe: clearing the history is not a withdrawal of consent, and re-asking after every wipe would
 * be noise. Each test gets its own preferences file so nothing leaks between them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelemetryConsentPersistenceTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build()
        val preferences = RitualPreferences(
            PreferenceDataStoreFactory.create {
                preferencesFolder.newFile("padla_test.preferences_pb").apply { delete() }
            }
        )
        repository = RitualRepositoryImpl(
            preferences = preferences,
            ritualLogDao = database.ritualLogDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a fresh install has not been asked yet`() = runTest {
        assertEquals(TelemetryConsent.UNKNOWN, repository.getTelemetryConsent().first())
    }

    @Test
    fun `granting and withdrawing both round-trip`() = runTest {
        repository.setTelemetryConsent(TelemetryConsent.GRANTED)
        assertEquals(TelemetryConsent.GRANTED, repository.getTelemetryConsent().first())

        repository.setTelemetryConsent(TelemetryConsent.DENIED)
        assertEquals(TelemetryConsent.DENIED, repository.getTelemetryConsent().first())
    }

    @Test
    fun `an unrecognised stored value reads as not asked`() {
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromId("yes_please"))
        assertEquals(TelemetryConsent.UNKNOWN, TelemetryConsent.fromId(null))
    }

    @Test
    fun `clearing data and history leaves consent alone`() = runTest {
        repository.setTelemetryConsent(TelemetryConsent.GRANTED)
        repository.logRitual(selectedAnswer = "done", mood = MoodTag.CALM, questionText = "q")

        repository.clearLogs()

        assertEquals(TelemetryConsent.GRANTED, repository.getTelemetryConsent().first())
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*TelemetryConsentPersistenceTest*"`
Expected: FAIL — compilation error, `TelemetryConsent` is unresolved.

- [ ] **Step 3: Add the domain enum**

Create `app/src/main/java/org/clockout/domain/model/TelemetryConsent.kt`:

```kotlin
package org.clockout.domain.model

/**
 * Whether the user has agreed to anonymous diagnostics.
 *
 * Three states rather than a boolean: "not asked yet" and "said no" have to be distinguishable, or
 * the opt-in dialog would either reappear forever or never appear at all. Stored by [id] rather
 * than by ordinal, so reordering the entries cannot change the meaning of a stored value.
 */
enum class TelemetryConsent(val id: String) {
    UNKNOWN("unknown"),
    GRANTED("granted"),
    DENIED("denied");

    val isGranted: Boolean get() = this == GRANTED

    companion object {
        /** Anything unrecognised means the question still has to be asked. */
        fun fromId(id: String?): TelemetryConsent = entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}
```

- [ ] **Step 4: Persist it**

In `app/src/main/java/org/clockout/data/local/RitualPreferences.kt`, add the import:

```kotlin
import org.clockout.domain.model.TelemetryConsent
```

add the key below `KEY_REVIEW_PROMPT_SHOWN`:

```kotlin
    private val KEY_TELEMETRY_CONSENT = stringPreferencesKey("telemetry_consent")
```

and add the flow and the setter next to the other Settings values:

```kotlin
    /**
     * Whether anonymous diagnostics may be collected. Deliberately outlives "Clear data and
     * history", the same way the review flag does: a wipe clears the history, not the answer to a
     * question the user has already been asked.
     */
    val telemetryConsentFlow: Flow<TelemetryConsent> = dataStore.data.map { preferences ->
        TelemetryConsent.fromId(preferences[KEY_TELEMETRY_CONSENT])
    }

    suspend fun setTelemetryConsent(consent: TelemetryConsent) {
        dataStore.edit { preferences ->
            preferences[KEY_TELEMETRY_CONSENT] = consent.id
        }
    }
```

- [ ] **Step 5: Expose it on the repository**

In `app/src/main/java/org/clockout/data/repository/RitualRepository.kt`, add the import for `TelemetryConsent` and these two members to the interface, next to `getBreathingStyle` / `setBreathingStyle`:

```kotlin
    fun getTelemetryConsent(): Flow<TelemetryConsent>
    suspend fun setTelemetryConsent(consent: TelemetryConsent)
```

In `RitualRepositoryImpl.kt`, add the import and the two delegating overrides, following the shape the file already uses for `getBreathingStyle`:

```kotlin
    override fun getTelemetryConsent(): Flow<TelemetryConsent> = preferences.telemetryConsentFlow

    override suspend fun setTelemetryConsent(consent: TelemetryConsent) {
        preferences.setTelemetryConsent(consent)
    }
```

Before writing these, open `RitualRepositoryImpl.kt` and confirm `clearLogs()` does **not** touch `KEY_TELEMETRY_CONSENT`. It must not — the fourth test pins that.

- [ ] **Step 6: Run the test and watch it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*TelemetryConsentPersistenceTest*"`
Expected: PASS, 4 tests.

- [ ] **Step 7: Confirm nothing else broke**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. Any other implementation of `RitualRepository` (test fakes included) now needs the two new members — fix them by delegating to a `MutableStateFlow(TelemetryConsent.UNKNOWN)` rather than by throwing.

- [ ] **Step 8: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/domain/model/TelemetryConsent.kt \
        app/src/main/java/org/clockout/data/local/RitualPreferences.kt \
        app/src/main/java/org/clockout/data/repository/RitualRepository.kt \
        app/src/main/java/org/clockout/data/repository/RitualRepositoryImpl.kt \
        app/src/test/java/org/clockout/TelemetryConsentPersistenceTest.kt
git commit -m "feat: souhlas s diagnostikou jako tři stavy v DataStore"
```

---

## Task 3: The event catalog and the tracker interface

Pure Kotlin, no Android. Everything here is testable on the JVM.

**Files:**
- Create: `app/src/main/java/org/clockout/domain/telemetry/AnalyticsTracker.kt`
- Create: `app/src/main/java/org/clockout/domain/telemetry/AnalyticsEvent.kt`
- Create: `app/src/main/java/org/clockout/domain/telemetry/UserProperty.kt`
- Create: `app/src/main/java/org/clockout/domain/telemetry/RitualStepAnalytics.kt`
- Create: `app/src/main/java/org/clockout/domain/telemetry/NoOpAnalyticsTracker.kt`
- Create: `app/src/test/java/org/clockout/telemetry/AnalyticsEventTest.kt`

**Interfaces:**
- Consumes: `RitualStep` from `domain/model`.
- Produces:
  - `interface AnalyticsTracker { fun setCollectionEnabled(enabled: Boolean); fun track(event: AnalyticsEvent); fun setUserProperty(property: UserProperty, value: String) }`
  - `sealed interface AnalyticsEvent { val name: String; val params: Map<String, Any> }` with `RitualStarted`, `RitualStepCompleted(step: String)`, `RitualCompleted(mood: String, daysClosedThisWeek: Int)`, `RitualAbandoned(step: String)`
  - `enum class UserProperty(val propertyName: String)` — `BREATHING_STYLE`, `SOUND_ENABLED`, `APP_LANGUAGE`, `INCLUDE_WEEKENDS`
  - `fun RitualStep.analyticsId(): String`
  - `object NoOpAnalyticsTracker : AnalyticsTracker`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/clockout/telemetry/AnalyticsEventTest.kt`:

```kotlin
package org.clockout.telemetry

import org.clockout.domain.model.RitualStep
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.UserProperty
import org.clockout.domain.telemetry.analyticsId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Firebase drops a malformed event name silently - no exception, no log, the event simply never
 * appears in the console. A name that breaks a limit is therefore invisible until someone wonders
 * months later why a funnel has a hole in it, which is why the rules are a test rather than a
 * review comment.
 */
class AnalyticsEventTest {

    private val allEvents = listOf(
        AnalyticsEvent.RitualStarted,
        AnalyticsEvent.RitualStepCompleted(RitualStep.Breathing.analyticsId()),
        AnalyticsEvent.RitualCompleted(mood = "CALM", daysClosedThisWeek = 3),
        AnalyticsEvent.RitualAbandoned(RitualStep.Question.analyticsId())
    )

    private val namePattern = Regex("^[a-z][a-z0-9_]*$")
    private val reservedPrefixes = listOf("firebase_", "google_", "ga_")

    @Test
    fun `event names fit Firebase's rules`() {
        allEvents.forEach { event ->
            assertTrue("${event.name} is not snake_case", namePattern.matches(event.name))
            assertTrue("${event.name} is longer than 40 characters", event.name.length <= 40)
            reservedPrefixes.forEach { prefix ->
                assertTrue("${event.name} uses the reserved prefix $prefix", !event.name.startsWith(prefix))
            }
        }
    }

    @Test
    fun `event parameter names fit Firebase's rules`() {
        allEvents.flatMap { it.params.keys }.forEach { key ->
            assertTrue("$key is not snake_case", namePattern.matches(key))
            assertTrue("$key is longer than 40 characters", key.length <= 40)
            reservedPrefixes.forEach { prefix ->
                assertTrue("$key uses the reserved prefix $prefix", !key.startsWith(prefix))
            }
        }
    }

    @Test
    fun `user property names fit the tighter 24-character ceiling`() {
        UserProperty.entries.forEach { property ->
            assertTrue(
                "${property.propertyName} is not snake_case",
                namePattern.matches(property.propertyName)
            )
            assertTrue(
                "${property.propertyName} is longer than 24 characters",
                property.propertyName.length <= 24
            )
        }
    }

    @Test
    fun `every ritual step has a distinct analytics id`() {
        val ids = listOf(
            RitualStep.Question, RitualStep.Breathing, RitualStep.Closing, RitualStep.Log
        ).map { it.analyticsId() }

        assertEquals(listOf("question", "breathing", "closing", "log"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a completed ritual carries the mood and the week count`() {
        val event = AnalyticsEvent.RitualCompleted(mood = "SATISFIED", daysClosedThisWeek = 4)

        assertEquals("ritual_completed", event.name)
        assertEquals("SATISFIED", event.params["mood"])
        assertEquals(4, event.params["days_closed_week"])
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*AnalyticsEventTest*"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Write the catalog**

Create `app/src/main/java/org/clockout/domain/telemetry/AnalyticsEvent.kt`:

```kotlin
package org.clockout.domain.telemetry

/**
 * Everything the app may report, in one list.
 *
 * Nothing here carries ritual content - not the question, not the answer, not the note. Only that
 * a step happened, which mood closed the day, and how much of the week is already closed.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any>

    data object RitualStarted : AnalyticsEvent {
        override val name: String = "ritual_started"
        override val params: Map<String, Any> = emptyMap()
    }

    data class RitualStepCompleted(val step: String) : AnalyticsEvent {
        override val name: String = "ritual_step_completed"
        override val params: Map<String, Any> get() = mapOf(PARAM_STEP to step)
    }

    data class RitualCompleted(val mood: String, val daysClosedThisWeek: Int) : AnalyticsEvent {
        override val name: String = "ritual_completed"
        override val params: Map<String, Any>
            get() = mapOf(PARAM_MOOD to mood, PARAM_DAYS_CLOSED_WEEK to daysClosedThisWeek)
    }

    /** Raised when the user leaves before the mood is logged, naming the step they left from. */
    data class RitualAbandoned(val step: String) : AnalyticsEvent {
        override val name: String = "ritual_abandoned"
        override val params: Map<String, Any> get() = mapOf(PARAM_STEP to step)
    }

    companion object {
        const val PARAM_STEP: String = "step"
        const val PARAM_MOOD: String = "mood"

        /**
         * [org.clockout.domain.model.RitualStreakInfo.completedThisWeekCount]. The repository has
         * no consecutive-day streak, so the parameter is named for what it actually holds.
         */
        const val PARAM_DAYS_CLOSED_WEEK: String = "days_closed_week"
    }
}
```

- [ ] **Step 4: Write the user properties and the step mapping**

Create `app/src/main/java/org/clockout/domain/telemetry/UserProperty.kt`:

```kotlin
package org.clockout.domain.telemetry

/**
 * Settings reported as user properties rather than as events, so every metric in the console can be
 * broken down by them - something events cannot do.
 *
 * Firebase allows 24 characters for a user property name, well below the 40 an event name gets.
 */
enum class UserProperty(val propertyName: String) {
    BREATHING_STYLE("breathing_style"),
    SOUND_ENABLED("sound_enabled"),
    APP_LANGUAGE("app_language"),
    INCLUDE_WEEKENDS("include_weekends")
}
```

Create `app/src/main/java/org/clockout/domain/telemetry/RitualStepAnalytics.kt`:

```kotlin
package org.clockout.domain.telemetry

import org.clockout.domain.model.RitualStep

/**
 * An extension rather than a property on [RitualStep], so the ritual's own model stays free of a
 * concern that belongs to reporting.
 */
fun RitualStep.analyticsId(): String = when (this) {
    RitualStep.Question -> "question"
    RitualStep.Breathing -> "breathing"
    RitualStep.Closing -> "closing"
    RitualStep.Log -> "log"
}
```

- [ ] **Step 5: Write the interface and the no-op**

Create `app/src/main/java/org/clockout/domain/telemetry/AnalyticsTracker.kt`:

```kotlin
package org.clockout.domain.telemetry

/**
 * What the app reports, independent of who receives it.
 *
 * Call sites talk to this and never to Firebase: the ViewModels behind it are covered by Robolectric
 * tests, and a static Firebase accessor in them would force either an initialized Firebase in every
 * test or static mocking.
 */
interface AnalyticsTracker {
    /** Turns reporting on or off wholesale. Off is the state every install starts in. */
    fun setCollectionEnabled(enabled: Boolean)

    fun track(event: AnalyticsEvent)

    fun setUserProperty(property: UserProperty, value: String)
}
```

Create `app/src/main/java/org/clockout/domain/telemetry/NoOpAnalyticsTracker.kt`:

```kotlin
package org.clockout.domain.telemetry

/** The default a ViewModel gets, so tests and previews report nothing without arranging anything. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserProperty(property: UserProperty, value: String) = Unit
}
```

- [ ] **Step 6: Run the test and watch it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*AnalyticsEventTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 7: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/domain/telemetry/ \
        app/src/test/java/org/clockout/telemetry/AnalyticsEventTest.kt
git commit -m "feat: katalog analytických událostí a rozhraní trackeru"
```

---

## Task 4: The consent gate and the Firebase tracker

**Files:**
- Create: `app/src/main/java/org/clockout/domain/telemetry/ConsentGatedTracker.kt`
- Create: `app/src/main/java/org/clockout/telemetry/FirebaseAnalyticsTracker.kt`
- Create: `app/src/test/java/org/clockout/telemetry/RecordingAnalyticsTracker.kt`
- Create: `app/src/test/java/org/clockout/telemetry/ConsentGatedTrackerTest.kt`

**Interfaces:**
- Consumes: `AnalyticsTracker`, `AnalyticsEvent`, `UserProperty` from Task 3.
- Produces:
  - `class ConsentGatedTracker(delegate: AnalyticsTracker) : AnalyticsTracker`
  - `class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker`
  - `class RecordingAnalyticsTracker : AnalyticsTracker` with `val events: List<AnalyticsEvent>`, `val properties: Map<UserProperty, String>`, `var collectionEnabled: Boolean`

- [ ] **Step 1: Write the test fake**

Create `app/src/test/java/org/clockout/telemetry/RecordingAnalyticsTracker.kt`:

```kotlin
package org.clockout.telemetry

import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.UserProperty

/** Records what it was asked to report, so a test can assert on it. */
class RecordingAnalyticsTracker : AnalyticsTracker {

    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events

    private val _properties = mutableMapOf<UserProperty, String>()
    val properties: Map<UserProperty, String> get() = _properties

    var collectionEnabled: Boolean = false
        private set

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }

    override fun track(event: AnalyticsEvent) {
        _events += event
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        _properties[property] = value
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/org/clockout/telemetry/ConsentGatedTrackerTest.kt`:

```kotlin
package org.clockout.telemetry

import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.ConsentGatedTracker
import org.clockout.domain.telemetry.UserProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate is what makes the opt-in real. A fresh instance reports nothing, because an install that
 * has not answered the dialog yet must behave exactly like one that said no.
 */
class ConsentGatedTrackerTest {

    private val delegate = RecordingAnalyticsTracker()
    private val tracker = ConsentGatedTracker(delegate)

    @Test
    fun `nothing is reported before consent is granted`() {
        tracker.track(AnalyticsEvent.RitualStarted)
        tracker.setUserProperty(UserProperty.APP_LANGUAGE, "cs")

        assertTrue(delegate.events.isEmpty())
        assertTrue(delegate.properties.isEmpty())
    }

    @Test
    fun `events flow once collection is enabled`() {
        tracker.setCollectionEnabled(true)

        tracker.track(AnalyticsEvent.RitualStarted)
        tracker.setUserProperty(UserProperty.APP_LANGUAGE, "cs")

        assertEquals(listOf(AnalyticsEvent.RitualStarted), delegate.events)
        assertEquals(mapOf(UserProperty.APP_LANGUAGE to "cs"), delegate.properties)
    }

    @Test
    fun `withdrawing consent stops the flow again`() {
        tracker.setCollectionEnabled(true)
        tracker.track(AnalyticsEvent.RitualStarted)

        tracker.setCollectionEnabled(false)
        tracker.track(AnalyticsEvent.RitualCompleted(mood = "CALM", daysClosedThisWeek = 1))

        assertEquals(listOf(AnalyticsEvent.RitualStarted), delegate.events)
    }

    @Test
    fun `the switch itself always reaches the delegate`() {
        tracker.setCollectionEnabled(true)
        assertTrue(delegate.collectionEnabled)

        tracker.setCollectionEnabled(false)
        assertTrue(!delegate.collectionEnabled)
    }
}
```

The last test matters: the SDK's automatic events (`session_start`, `screen_view`) never pass through our interface, so only the delegate forwarding the switch can stop them.

- [ ] **Step 3: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConsentGatedTrackerTest*"`
Expected: FAIL — `ConsentGatedTracker` is unresolved.

- [ ] **Step 4: Write the gate**

Create `app/src/main/java/org/clockout/domain/telemetry/ConsentGatedTracker.kt`:

```kotlin
package org.clockout.domain.telemetry

/**
 * Drops every event until consent has been granted.
 *
 * A decorator rather than a flag inside the Firebase tracker, so the gate is pure Kotlin and can be
 * tested without a Firebase instance. [setCollectionEnabled] is still passed through, because the
 * SDK's own automatic events never travel through [AnalyticsTracker] and only the SDK's flag can
 * stop those.
 *
 * The flag is volatile: the ritual reports from a coroutine while consent is written from another.
 */
class ConsentGatedTracker(private val delegate: AnalyticsTracker) : AnalyticsTracker {

    @Volatile
    private var enabled: Boolean = false

    override fun setCollectionEnabled(enabled: Boolean) {
        this.enabled = enabled
        delegate.setCollectionEnabled(enabled)
    }

    override fun track(event: AnalyticsEvent) {
        if (enabled) delegate.track(event)
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        if (enabled) delegate.setUserProperty(property, value)
    }
}
```

- [ ] **Step 5: Run the test and watch it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConsentGatedTrackerTest*"`
Expected: PASS, 4 tests.

- [ ] **Step 6: Write the Firebase implementation**

Create `app/src/main/java/org/clockout/telemetry/FirebaseAnalyticsTracker.kt`:

```kotlin
package org.clockout.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.UserProperty

/**
 * The only class in the app that knows Firebase exists.
 *
 * Collection starts disabled in the manifest, so this switch is the only thing that ever turns it
 * on. Both SDKs remember the value across launches once set, which is why consent is written to
 * DataStore as well rather than relied on from the SDKs.
 */
class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    override fun track(event: AnalyticsEvent) {
        val bundle = Bundle().apply {
            event.params.forEach { (key, value) ->
                when (value) {
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        analytics.logEvent(event.name, bundle)
    }

    override fun setUserProperty(property: UserProperty, value: String) {
        analytics.setUserProperty(property.propertyName, value)
    }
}
```

- [ ] **Step 7: Verify it compiles and nothing regressed**

Run: `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 8: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/domain/telemetry/ConsentGatedTracker.kt \
        app/src/main/java/org/clockout/telemetry/ \
        app/src/test/java/org/clockout/telemetry/
git commit -m "feat: brána souhlasu a napojení na Firebase SDK"
```

---

## Task 5: Strings, the external link helper, and the consent dialog

**Files:**
- Create: `app/src/main/java/org/clockout/util/ExternalLink.kt`
- Create: `app/src/main/java/org/clockout/presentation/components/TelemetryConsentDialog.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `fun Context.openUrl(url: String)`
  - `@Composable fun TelemetryConsentDialog(onGrant: () -> Unit, onDeny: () -> Unit, onOpenPrivacyPolicy: () -> Unit)`
  - String keys: `telemetry_consent_title`, `telemetry_consent_body`, `telemetry_consent_allow`, `telemetry_consent_deny`, `telemetry_consent_policy_link`, `settings_diagnostics_title`, `settings_diagnostics_desc`, `settings_privacy_policy_button`

- [ ] **Step 1: Add the English strings**

In `app/src/main/res/values/strings.xml`, next to the `settings_rate_*` block:

```xml
    <string name="telemetry_consent_title">Help improve ClockOut?</string>
    <string name="telemetry_consent_body">ClockOut can send anonymous crash reports and usage statistics — which step of the ritual you reach, which breathing style you use. Never your answers, your notes or anything you write. You can change this any time in Settings.</string>
    <string name="telemetry_consent_allow">Allow</string>
    <string name="telemetry_consent_deny">No thanks</string>
    <string name="telemetry_consent_policy_link">Privacy policy</string>
    <string name="settings_diagnostics_title">Anonymous diagnostics</string>
    <string name="settings_diagnostics_desc">Sends crash reports and anonymous usage statistics so problems can be found and fixed. Never your answers or your notes.</string>
    <string name="settings_privacy_policy_button">Privacy policy</string>
```

- [ ] **Step 2: Add the Czech strings**

In `app/src/main/res/values-cs/strings.xml`, at the matching position:

```xml
    <string name="telemetry_consent_title">Pomůžete Padle se zlepšit?</string>
    <string name="telemetry_consent_body">Padla může odesílat anonymní hlášení o pádech a statistiky používání — ke kterému kroku rituálu se dostanete, jaký styl dýchání používáte. Nikdy vaše odpovědi, poznámky ani cokoliv, co napíšete. Kdykoliv to změníte v nastavení.</string>
    <string name="telemetry_consent_allow">Povolit</string>
    <string name="telemetry_consent_deny">Nechci</string>
    <string name="telemetry_consent_policy_link">Zásady ochrany osobních údajů</string>
    <string name="settings_diagnostics_title">Anonymní diagnostika</string>
    <string name="settings_diagnostics_desc">Odesílá hlášení o pádech a anonymní statistiky používání, aby šlo problémy najít a opravit. Nikdy vaše odpovědi ani poznámky.</string>
    <string name="settings_privacy_policy_button">Zásady ochrany osobních údajů</string>
```

- [ ] **Step 3: Verify both files hold the same key set**

Run:

```bash
diff <(grep -o 'name="[a-z_]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[a-z_]*"' app/src/main/res/values-cs/strings.xml | sort)
```

Expected: no output. Any difference is a missing translation and lint will fail on it.

- [ ] **Step 4: Write the link helper**

Create `app/src/main/java/org/clockout/util/ExternalLink.kt`:

```kotlin
package org.clockout.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Where the app's privacy policy is published. */
const val PRIVACY_POLICY_URL: String = "https://ruzickaond.github.io/clockout-legal/"

/**
 * Hands a web address to whatever browser the device has.
 *
 * A sibling of [openPlayStoreListing] and deliberately as forgiving: a device with no browser at all
 * is unusual but not impossible, and a settings screen must not crash because of it.
 */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No browser installed: nothing sensible left to do.
    }
}
```

- [ ] **Step 5: Write the dialog**

Create `app/src/main/java/org/clockout/presentation/components/TelemetryConsentDialog.kt`:

```kotlin
package org.clockout.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.clockout.R
import org.clockout.ui.theme.IndigoAccent

/**
 * The opt-in question, asked once per install.
 *
 * Neither the back button nor a tap outside dismisses it: an unanswered dialog would otherwise
 * resolve to no answer at all, and the app would keep asking on every launch. The two buttons are
 * the only ways out.
 */
@Composable
fun TelemetryConsentDialog(
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { /* Answering is the only way out - see the KDoc. */ },
        modifier = modifier.testTag("telemetry_consent_dialog"),
        title = { Text(text = stringResource(R.string.telemetry_consent_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.telemetry_consent_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.telemetry_consent_policy_link),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = IndigoAccent,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .testTag("telemetry_privacy_policy_link")
                        .clickable { onOpenPrivacyPolicy() }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onGrant,
                modifier = Modifier.testTag("telemetry_consent_allow")
            ) {
                Text(text = stringResource(R.string.telemetry_consent_allow))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeny,
                modifier = Modifier.testTag("telemetry_consent_deny")
            ) {
                Text(text = stringResource(R.string.telemetry_consent_deny))
            }
        }
    )
}
```

Add the missing import for `clickable`: `import androidx.compose.foundation.clickable`.

- [ ] **Step 6: Verify it compiles and lint is green**

Run: `./gradlew :app:compileDebugKotlin && ./gradlew :app:lintDebug`
Expected: BUILD SUCCESSFUL both times.

- [ ] **Step 7: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/util/ExternalLink.kt \
        app/src/main/java/org/clockout/presentation/components/TelemetryConsentDialog.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-cs/strings.xml
git commit -m "feat: dialog souhlasu s diagnostikou a odkaz na zásady"
```

---

## Task 6: Wire the tracker into the app

The task that changes behaviour on screen, so it also repairs the two navigation screenshot tests.

**Files:**
- Create: `app/src/main/java/org/clockout/presentation/TelemetryDependencies.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt`
- Modify: `app/src/test/java/org/clockout/screenshot/AdaptiveNavigationTest.kt`

**Interfaces:**
- Consumes: `ConsentGatedTracker`, `FirebaseAnalyticsTracker`, `TelemetryConsent`, `TelemetryConsentDialog`, `openUrl`, `PRIVACY_POLICY_URL`.
- Produces: `@Composable fun rememberAnalyticsTracker(context: Context = LocalContext.current): AnalyticsTracker` — one gated tracker per process.

- [ ] **Step 1: Write the factory**

Create `app/src/main/java/org/clockout/presentation/TelemetryDependencies.kt`:

```kotlin
package org.clockout.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.ConsentGatedTracker
import org.clockout.telemetry.FirebaseAnalyticsTracker

/**
 * The gated tracker the app reports through.
 *
 * Built from the application context, so it outlives an Activity recreation, and above
 * [org.clockout.presentation.util.AppLocalization] for the same reason
 * [rememberRitualRepository] is: neither has any use for a context whose configuration has been
 * overridden to localize strings.
 */
@Composable
fun rememberAnalyticsTracker(context: Context = LocalContext.current): AnalyticsTracker {
    val appContext = context.applicationContext
    return remember(appContext) {
        ConsentGatedTracker(FirebaseAnalyticsTracker(appContext))
    }
}
```

- [ ] **Step 2: Hold consent and the tracker in `PadlaApp`**

In `app/src/main/java/org/clockout/presentation/PadlaApp.kt`, add these imports:

```kotlin
import org.clockout.domain.model.TelemetryConsent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.UserProperty
import org.clockout.presentation.components.TelemetryConsentDialog
import org.clockout.util.PRIVACY_POLICY_URL
import org.clockout.util.openUrl
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
```

Right after the existing `val repository = rememberRitualRepository(context)` line, add:

```kotlin
    val analyticsTracker = rememberAnalyticsTracker(context)
    val scope = rememberCoroutineScope()

    // Null until DataStore has actually emitted. Starting at UNKNOWN instead would flash the
    // opt-in dialog on every launch for users who answered it long ago, the same trap
    // HomeState.isTestModeEnabled documents.
    val consent by repository.getTelemetryConsent().collectAsState(initial = null)

    LaunchedEffect(consent) {
        analyticsTracker.setCollectionEnabled(consent == TelemetryConsent.GRANTED)
    }
```

- [ ] **Step 3: Report the user properties**

Still in `PadlaApp`, below the effect from Step 2:

```kotlin
    // Reported from here rather than from the Application class: consent may arrive long after
    // startup, and these have to be sent when it does, not only when a setting next changes.
    val breathingStyle by repository.getBreathingStyle().collectAsState(initial = null)
    val soundEnabled by repository.getSoundEnabled().collectAsState(initial = null)
    val includeWeekends by repository.getIncludeWeekends().collectAsState(initial = null)

    LaunchedEffect(consent, breathingStyle, soundEnabled, includeWeekends, language) {
        if (consent != TelemetryConsent.GRANTED) return@LaunchedEffect
        breathingStyle?.let { analyticsTracker.setUserProperty(UserProperty.BREATHING_STYLE, it.id) }
        soundEnabled?.let {
            analyticsTracker.setUserProperty(UserProperty.SOUND_ENABLED, it.toString())
        }
        includeWeekends?.let {
            analyticsTracker.setUserProperty(UserProperty.INCLUDE_WEEKENDS, it.toString())
        }
        analyticsTracker.setUserProperty(UserProperty.APP_LANGUAGE, language.code)
    }
```

This effect must be placed **after** the existing `val language by repository.getLanguage().collectAsState(...)` line, because it reads `language`. Move it below that declaration if the compiler complains.

- [ ] **Step 4: Show the dialog**

Inside the `AppLocalization(language) { Box(...) { ... } }` block, immediately before the closing `overlay()` call:

```kotlin
            if (consent == TelemetryConsent.UNKNOWN) {
                TelemetryConsentDialog(
                    onGrant = {
                        scope.launch { repository.setTelemetryConsent(TelemetryConsent.GRANTED) }
                    },
                    onDeny = {
                        scope.launch { repository.setTelemetryConsent(TelemetryConsent.DENIED) }
                    },
                    onOpenPrivacyPolicy = { context.openUrl(PRIVACY_POLICY_URL) }
                )
            }
            overlay()
```

- [ ] **Step 5: Pass the tracker to `RitualViewModel`**

Task 7 adds the parameter. For now, leave the `RitualViewModel` construction untouched — it still compiles because the parameter does not exist yet.

- [ ] **Step 6: Run the navigation tests and watch them fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*NavigationTest*"`
Expected: FAIL — the dialog is on screen, so `nav_history_button` and `start_ritual_button` are not reachable.

This failure is the point of the step: it proves the dialog really does block a fresh install.

- [ ] **Step 7: Seed consent in the navigation tests**

In `app/src/test/java/org/clockout/screenshot/AdaptiveNavigationTest.kt`, add the imports:

```kotlin
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.clockout.data.local.RitualPreferences
import org.clockout.domain.model.TelemetryConsent
import org.junit.Before
```

and add this `@Before` to **both** `PhoneNavigationTest` and `TabletNavigationTest` (repeat it in each class — they are separate classes with separate Robolectric configs):

```kotlin
    /**
     * These two render the real PadlaApp against the real DataStore, so a fresh install would put
     * the telemetry opt-in dialog over the screen they are here to photograph. Answering it up
     * front keeps the captures about navigation.
     */
    @Before
    fun answerTelemetryConsent() = runBlocking {
        RitualPreferences(ApplicationProvider.getApplicationContext<android.content.Context>())
            .setTelemetryConsent(TelemetryConsent.DENIED)
    }
```

- [ ] **Step 8: Run them and watch them pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*NavigationTest*"`
Expected: PASS.

Note from `CLAUDE.md`: both classes fail at the weekend for an unrelated reason (`start_ritual_button` is absent on a Saturday or Sunday while `includeWeekends` is off). Check the day of the week before reading a failure as a regression, and run each class on its own to confirm.

- [ ] **Step 9: Confirm the screenshots did not move**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: PASS. **If a capture differs, do not re-record it** — find out why the rendering changed first.

- [ ] **Step 10: Run everything**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:lintDebug`
Expected: PASS.

- [ ] **Step 11: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/presentation/TelemetryDependencies.kt \
        app/src/main/java/org/clockout/presentation/PadlaApp.kt \
        app/src/test/java/org/clockout/screenshot/AdaptiveNavigationTest.kt
git commit -m "feat: opt-in dialog při prvním spuštění a hlášení user properties"
```

---

## Task 7: The four ritual events

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt` (pass the tracker in)
- Create: `app/src/test/java/org/clockout/telemetry/RitualTelemetryTest.kt`

**Interfaces:**
- Consumes: `AnalyticsTracker`, `AnalyticsEvent`, `analyticsId()`, `RecordingAnalyticsTracker`.
- Produces: `RitualViewModel(..., analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker)` — a new **last** constructor parameter with a default, so no existing call site or test signature changes.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/clockout/telemetry/RitualTelemetryTest.kt`:

```kotlin
package org.clockout.telemetry

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.usecase.GetTodayRitualUseCase
import org.clockout.domain.usecase.LogMoodUseCase
import org.clockout.domain.usecase.ShouldRequestReviewUseCase
import org.clockout.presentation.ritual.RitualIntent
import org.clockout.presentation.ritual.RitualViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What the ritual reports, and - more importantly - what it does not. The question, the answer and
 * the note must never reach a parameter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RitualTelemetryTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualRepository
    private lateinit var tracker: RecordingAnalyticsTracker

    private fun viewModel(): RitualViewModel {
        val context: Context = ApplicationProvider.getApplicationContext()
        return RitualViewModel(
            appContext = context,
            getTodayRitualUseCase = GetTodayRitualUseCase(repository),
            logMoodUseCase = LogMoodUseCase(repository),
            shouldRequestReviewUseCase = ShouldRequestReviewUseCase(repository),
            ritualRepository = repository,
            analyticsTracker = tracker
        )
    }

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
            ritualLogDao = database.ritualLogDao()
        )
        tracker = RecordingAnalyticsTracker()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `starting a run reports it once`() = runTest {
        val model = viewModel()

        model.startSession(1)
        model.startSession(1) // the same run handed back after a rotation

        assertEquals(listOf(AnalyticsEvent.RitualStarted), tracker.events)
    }

    @Test
    fun `each step reports its own completion`() = runTest {
        val model = viewModel()
        model.startSession(1)

        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)

        val steps = tracker.events
            .filterIsInstance<AnalyticsEvent.RitualStepCompleted>()
            .map { it.step }

        assertEquals(listOf("question", "breathing", "closing"), steps)
    }

    @Test
    fun `leaving early reports the step it was left from`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.ProceedToBreathing)

        model.stopSession()

        assertEquals(
            listOf(AnalyticsEvent.RitualAbandoned("breathing")),
            tracker.events.filterIsInstance<AnalyticsEvent.RitualAbandoned>()
        )
    }

    @Test
    fun `finishing does not count as abandoning`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)
        model.onIntent(RitualIntent.MoodSelected(org.clockout.domain.model.MoodTag.CALM))

        model.stopSession()

        assertTrue(tracker.events.filterIsInstance<AnalyticsEvent.RitualAbandoned>().isEmpty())
    }

    @Test
    fun `no reported parameter carries what the user wrote`() = runTest {
        val model = viewModel()
        model.startSession(1)
        model.onIntent(RitualIntent.CustomAnswerChanged("finished the quarterly report"))
        model.onIntent(RitualIntent.ProceedToBreathing)
        model.onIntent(RitualIntent.SkipBreathing)
        model.onIntent(RitualIntent.ProceedToMoodLog)
        model.onIntent(RitualIntent.MoodSelected(org.clockout.domain.model.MoodTag.SATISFIED))

        val everyValue = tracker.events.flatMap { it.params.values }.map { it.toString() }
        assertTrue(everyValue.none { it.contains("quarterly") })
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*RitualTelemetryTest*"`
Expected: FAIL — `analyticsTracker` is not a parameter of `RitualViewModel`.

- [ ] **Step 3: Add the constructor parameter**

In `RitualViewModel.kt`, add the imports:

```kotlin
import org.clockout.domain.telemetry.AnalyticsEvent
import org.clockout.domain.telemetry.AnalyticsTracker
import org.clockout.domain.telemetry.NoOpAnalyticsTracker
import org.clockout.domain.telemetry.analyticsId
import kotlinx.coroutines.flow.first
```

and the parameter as the **last** one, after `audioPlayer`:

```kotlin
class RitualViewModel(
    private val appContext: Context,
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val logMoodUseCase: LogMoodUseCase,
    private val shouldRequestReviewUseCase: ShouldRequestReviewUseCase,
    private val ritualRepository: RitualRepository,
    private val audioPlayer: AmbientAudioPlayer = AmbientAudioPlayer(),
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {
```

- [ ] **Step 4: Report the start and the abandonment**

In `startSession`, after the `_state.update { ... }` block that resets the state, add:

```kotlin
        analyticsTracker.track(AnalyticsEvent.RitualStarted)
```

It sits after the early `if (sessionId == id) return`, so a rotation does not report a second start.

Replace `stopSession` with:

```kotlin
    /** Leaves the ritual: the countdown stops and the ambient sound does not follow the user out. */
    fun stopSession() {
        // A finished ritual reaches this too, on the way home from the completion card. Only a
        // ritual left before the mood was logged is an abandonment.
        if (!_state.value.isCompletedAndSaved) {
            analyticsTracker.track(AnalyticsEvent.RitualAbandoned(_state.value.currentStep.analyticsId()))
        }
        timerJob?.cancel()
        timerJob = null
        audioPlayer.stopAmbient()
    }
```

- [ ] **Step 5: Report the step transitions**

In `onIntent`, add one line to each of the three branches, immediately after its `_state.update { ... }` block:

`ProceedToBreathing` — the question step is now behind the user:

```kotlin
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Question.analyticsId())
                )
```

`BreathingFinished, SkipBreathing`:

```kotlin
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Breathing.analyticsId())
                )
```

`ProceedToMoodLog`:

```kotlin
                analyticsTracker.track(
                    AnalyticsEvent.RitualStepCompleted(RitualStep.Closing.analyticsId())
                )
```

- [ ] **Step 6: Report the completion**

In `handleMoodSelected`, inside the `viewModelScope.launch { ... }` block, after the `_state.update { ... }` that sets `isCompletedAndSaved`:

```kotlin
            // Read after the log is written, so the count includes today.
            val daysClosedThisWeek = ritualRepository.getStreakInfo().first().completedThisWeekCount
            analyticsTracker.track(
                AnalyticsEvent.RitualStepCompleted(RitualStep.Log.analyticsId())
            )
            analyticsTracker.track(
                AnalyticsEvent.RitualCompleted(
                    mood = mood.id,
                    daysClosedThisWeek = daysClosedThisWeek
                )
            )
```

- [ ] **Step 7: Hand the tracker to the ViewModel**

In `PadlaApp.kt`, in the `Screen.Ritual` branch, add the argument to the `RitualViewModel(...)` construction:

```kotlin
                            val ritualViewModel: RitualViewModel = viewModel {
                                RitualViewModel(
                                    context.applicationContext,
                                    getTodayRitualUseCase,
                                    logMoodUseCase,
                                    shouldRequestReviewUseCase,
                                    repository,
                                    analyticsTracker = analyticsTracker
                                )
                            }
```

The named argument skips `audioPlayer`, which keeps its default.

- [ ] **Step 8: Run the test and watch it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*RitualTelemetryTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 9: Confirm the ritual's own tests still pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. `RitualSessionSurvivalTest`, `RitualReviewRequestTest` and the screenshot tests all construct `RitualViewModel` without a tracker and must still compile untouched.

- [ ] **Step 10: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/presentation/ritual/RitualViewModel.kt \
        app/src/main/java/org/clockout/presentation/PadlaApp.kt \
        app/src/test/java/org/clockout/telemetry/RitualTelemetryTest.kt
git commit -m "feat: rituál hlásí start, kroky, dokončení a opuštění"
```

---

## Task 8: The diagnostics card in Settings

**Files:**
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsState.kt`
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/org/clockout/presentation/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/org/clockout/presentation/PadlaApp.kt`
- Create: `app/src/test/java/org/clockout/telemetry/SettingsTelemetryTest.kt`

**Interfaces:**
- Consumes: `TelemetryConsent`, `RitualRepository.setTelemetryConsent`.
- Produces:
  - `SettingsState.telemetryConsent: TelemetryConsent` (default `TelemetryConsent.UNKNOWN`)
  - `SettingsViewModel.setTelemetryConsent(granted: Boolean)`
  - `SettingsScreen(..., onTelemetryConsentToggled: (Boolean) -> Unit, onOpenPrivacyPolicy: () -> Unit)`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/clockout/telemetry/SettingsTelemetryTest.kt`:

```kotlin
package org.clockout.telemetry

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.clockout.data.repository.RitualRepository
import org.clockout.data.repository.RitualRepositoryImpl
import org.clockout.domain.model.TelemetryConsent
import org.clockout.presentation.settings.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Settings switch is the only way back from a "no", so it has to write both directions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsTelemetryTest {

    @get:Rule val preferencesFolder = TemporaryFolder()

    private lateinit var database: ClockoutDatabase
    private lateinit var repository: RitualRepository

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
            ritualLogDao = database.ritualLogDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the switch grants and withdraws`() = runTest {
        val model = SettingsViewModel(repository)

        model.setTelemetryConsent(true)
        assertEquals(TelemetryConsent.GRANTED, repository.getTelemetryConsent().first())

        model.setTelemetryConsent(false)
        assertEquals(TelemetryConsent.DENIED, repository.getTelemetryConsent().first())
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsTelemetryTest*"`
Expected: FAIL — `setTelemetryConsent` is not a member of `SettingsViewModel`.

- [ ] **Step 3: Add the state field**

In `SettingsState.kt`, add the import for `TelemetryConsent` and the field after `isPreviewPlaying`:

```kotlin
    val telemetryConsent: TelemetryConsent = TelemetryConsent.UNKNOWN
```

- [ ] **Step 4: Observe and write it in the ViewModel**

In `SettingsViewModel.kt`, add the import for `TelemetryConsent` and this method next to the other setters:

```kotlin
    fun setTelemetryConsent(granted: Boolean) {
        viewModelScope.launch {
            repository.setTelemetryConsent(
                if (granted) TelemetryConsent.GRANTED else TelemetryConsent.DENIED
            )
        }
    }
```

The existing `observeSettings()` already combines four flows into `SettingsDataHolder` and cannot take a fifth without restructuring; collect consent in its own coroutine inside `init` instead:

```kotlin
    init {
        observeSettings()
        viewModelScope.launch {
            repository.getTelemetryConsent().collect { consent ->
                _state.update { it.copy(telemetryConsent = consent) }
            }
        }
    }
```

- [ ] **Step 5: Run the test and watch it pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsTelemetryTest*"`
Expected: PASS.

- [ ] **Step 6: Add the card to the screen**

In `SettingsScreen.kt`, add two parameters to the composable's signature, immediately **before** `onClearData`:

```kotlin
    onTelemetryConsentToggled: (Boolean) -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
```

The `= {}` defaults are deliberate and match the file: most of its callbacks already carry them, and they keep all six existing test call sites (`CustomSoundMissingFileUiTest`, `SettingsScreenLocalizationTest`, `SettingsWeekendToggleTest`, `SettingsRateCardTest`, `BreathingStylePickerTest`, `FormFactorScreenshotTest`) compiling untouched.

and add the import `import org.clockout.domain.model.TelemetryConsent`.

Place the new card immediately **before** the "Rate the app" card. Copy the surrounding `Box`/`Column` wrapper from the existing rate card verbatim so the frosted styling matches, and put this inside it:

```kotlin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_diagnostics_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_diagnostics_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSlate,
                                    lineHeight = 20.sp
                                )
                            }
                            Switch(
                                checked = state.telemetryConsent == TelemetryConsent.GRANTED,
                                onCheckedChange = onTelemetryConsentToggled,
                                modifier = Modifier.testTag("telemetry_consent_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenPrivacyPolicy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("privacy_policy_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_privacy_policy_button),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
```

Match the `Switch` colors to whichever the existing `settings_test_mode_title` switch uses — copy that `SwitchDefaults.colors(...)` block rather than leaving the stock palette, or this switch will not look like its neighbours.

- [ ] **Step 7: Pass the callbacks from `PadlaApp`**

In the `Screen.Settings` branch of `PadlaApp.kt`, add to the `SettingsScreen(...)` call:

```kotlin
                                onTelemetryConsentToggled = { settingsViewModel.setTelemetryConsent(it) },
                                onOpenPrivacyPolicy = { context.openUrl(PRIVACY_POLICY_URL) },
```

- [ ] **Step 8: Confirm no other call site needs touching**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL with no changes to any test. The `= {}` defaults from Step 6 are what makes that true; if the compiler reports a missing argument anywhere, a default was dropped — restore it rather than editing the test.

- [ ] **Step 9: Run everything**

Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:lintDebug`
Expected: PASS.

Watch `SettingsRateCardTest` in particular: the new card sits directly above the rate card and pushes `rate_app_button` further down the scroll. If that test now fails to find or click the button, the fix is a `performScrollTo()` on the node, not moving the new card.

- [ ] **Step 10: Check the Settings screenshots**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: the Settings captures in `FormFactorScreenshotTest` **will** differ — a new card is genuinely on the screen. This is the one intended rendering change in the plan. Confirm by eye that the diff shows only the new card, then:

```bash
./gradlew :app:recordRoborazziDebug --tests "org.clockout.screenshot.*"
```

Only `closure_lamp_off.png` and `closure_lamp_on.png` are tracked in git; everything under `app/src/test/screenshots/formfactor/**` is git-ignored and per-machine, so this re-record changes nothing for anyone else.

- [ ] **Step 11: Commit — ask first**

```bash
git add app/src/main/java/org/clockout/presentation/settings/ \
        app/src/main/java/org/clockout/presentation/PadlaApp.kt \
        app/src/test/java/org/clockout/telemetry/SettingsTelemetryTest.kt
git commit -m "feat: karta anonymní diagnostiky a odkaz na zásady v nastavení"
```

---

## Task 9: Documentation

`CLAUDE.md` and `AGENTS.md` must be updated together — the repo treats that as a hard rule.

**Files:**
- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`
- Modify: `docs/superpowers/specs/2026-09-18-telemetry-design.md` (only if Task 1 Step 5 needed the test manifest)

- [ ] **Step 1: Correct the project overview**

In **both** files, the sentence claiming the app ships without `INTERNET` is now false. Replace the paragraph beginning "All data stays on the device" with:

```markdown
Ritual logs and preferences stay on the device — Room for ritual logs, DataStore for preferences.
No accounts, no cloud sync, no ads. The app does carry the `INTERNET` permission, for Firebase
Crashlytics and Analytics, but **both start disabled and stay disabled until the user opts in** —
see *Telemetry*. Play In-App Update and Play In-App Review still reach the Play Store over IPC
rather than over the network, and the "Rate the app" button hands a `market://` intent to the Play
app rather than fetching anything itself.
```

- [ ] **Step 2: Add the Telemetry section**

In **both** files, after the "Persistence and backup" section:

```markdown
## Telemetry

Crashes go to Firebase Crashlytics, ritual completion to Firebase Analytics, and neither collects
anything until the user says yes.

`TelemetryConsent` (`domain/model`) has three states, not two: `UNKNOWN`, `GRANTED`, `DENIED`. A
boolean cannot separate "not asked yet" from "said no", and the opt-in dialog would then either
reappear forever or never appear. It is stored in DataStore under `telemetry_consent` by its id
string, and it deliberately **survives "Clear data and history"** — clearing the history is not a
withdrawal of consent. The Settings switch is the way back from a "no".

Both SDKs are switched off in `AndroidManifest.xml` through
`firebase_analytics_collection_enabled` and `firebase_crashlytics_collection_enabled`. This is
load-bearing: `FirebaseInitProvider` starts collecting during application startup, long before the
dialog can be shown, so without those two entries the first session of every install would be
uploaded and the opt-in would be decoration. `ConsentGatedTracker` then turns collection on only
once consent is `GRANTED`.

The layering is `AnalyticsTracker` (a pure-Kotlin interface in `domain/telemetry`) →
`ConsentGatedTracker` (the gate, also pure Kotlin, so it is testable without Firebase) →
`FirebaseAnalyticsTracker` (`telemetry/`, the only class that knows Firebase exists). ViewModels
take the interface with `NoOpAnalyticsTracker` as the default, which is why no existing test
signature had to change. The gate stops our own events; `setCollectionEnabled` is passed through to
the SDK as well, because its automatic events never travel through our interface.

`AnalyticsEvent` is the whole catalog: `ritual_started`, `ritual_step_completed`,
`ritual_completed`, `ritual_abandoned`. Nothing carries ritual content — not the question, not the
answer, not the note; `RitualTelemetryTest` pins that. Settings are reported as **user properties**
rather than events, so every metric can be broken down by them. Firebase's name limits differ per
kind — 40 characters for an event or a parameter, **24 for a user property** — and a name that
breaks one is dropped silently at runtime, so `AnalyticsEventTest` asserts them.

`PhoneNavigationTest` and `TabletNavigationTest` seed `telemetry_consent` in `@Before`: they render
the real `PadlaApp` against the real DataStore, and a fresh install would otherwise put the opt-in
dialog over the screen they exist to photograph.
```

- [ ] **Step 3: Extend "What NOT to do"**

In **both** files:

```markdown
- Do not remove the two `firebase_*_collection_enabled` meta-data entries from the manifest — the SDKs would then collect the first session of every install before the user has been asked.
- Do not clear `telemetry_consent` when clearing data and history — a wipe is not a withdrawal of consent, and the dialog would return on every wipe.
- Do not call Firebase from a ViewModel — go through `AnalyticsTracker`, or the Robolectric tests behind it need an initialized Firebase.
- Do not put ritual content into an analytics parameter — not the question, not the answer, not the note.
- Do not give `TelemetryConsent` a boolean representation — "not asked" and "said no" have to stay distinguishable.
```

- [ ] **Step 4: Update the dependency note**

In **both** files, the "Dependency management" section lists Firebase libraries as deliberately commented out. Remove `firebase-analytics` and `firebase-crashlytics` from that list and note that the app now carries `INTERNET`, so the argument against re-enabling Retrofit/OkHttp is no longer the permission but the absence of a feature that needs them.

- [ ] **Step 5: Verify the two files agree**

Run:

```bash
diff <(grep '^##' CLAUDE.md) <(grep '^##' AGENTS.md)
```

Expected: no output, or only differences that were already there before this task.

- [ ] **Step 6: Commit — ask first**

```bash
git add CLAUDE.md AGENTS.md docs/superpowers/
git commit -m "docs: telemetrie, souhlas a co u ní nedělat"
```

---

## Task 10: Release readiness

No code. The release must not go out before these are done, and they are the author's to do.

- [ ] **Step 1: Verify the whole gate**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:verifyRoborazziDebug
```

All four must pass. `verifyRoborazziDebug` is the only one that compares screenshots.

- [ ] **Step 2: Confirm the opt-in really gates on a device**

Install a debug build on a clean profile, deny the dialog, and confirm in the Firebase console's DebugView that no event arrives. Then grant it and confirm `ritual_started` does. Enable DebugView with:

```bash
adb shell setprop debug.firebase.analytics.app org.clockout
```

and turn it off again with `adb shell setprop debug.firebase.analytics.app .none.`.

This is the only check that proves the manifest kill switch works; no unit test can.

- [ ] **Step 3: Bump the version**

`versionCode` is already `1000302` in `app/build.gradle.kts` while `versionName` is still `1.0.3`. Decide the user-facing version for this release — a new permission and a new dialog argue for `1.1.0` — and set both.

- [ ] **Step 4: Update Play Console → Data safety**

Declare, before rolling out:
- **Crash logs** and **Diagnostics** — collected, not shared, not required (Crashlytics).
- **App interactions** — collected, not shared, not required (Analytics).
- **Approximate location** — Analytics derives it from the IP address; declare it or the review will flag the mismatch.

An incorrect Data safety declaration is the usual reason an update is held.

- [ ] **Step 5: Consider a Czech privacy policy**

The published policy at <https://ruzickaond.github.io/clockout-legal/> already names "Google Analytics for Firebase" and "Firebase Crashlytics", so it needs no rewrite for Play. It is English-only while the app is Czech-first. Worth adding a Czech version; it does not block the release.

---

## Self-review notes

Checked against the spec:

- Every spec section maps to a task: consent states → Task 2; manifest kill switch → Task 1; tracker layering → Tasks 3 and 4; event catalog → Tasks 3 and 7; UI → Tasks 5, 6 and 8; testing → folded into each task; documentation → Task 9; the outside-the-codebase items → Task 10.
- The spec's "Known risk" (Robolectric and `FirebaseInitProvider`) is Task 1 Step 5, before any application code exists, with the remedy written out.
- Names are consistent across tasks: `setCollectionEnabled`, `track`, `setUserProperty`, `analyticsId()`, `telemetryConsent`, `days_closed_week`.
- The one place the plan knowingly re-records a screenshot baseline is Task 8 Step 10, where a new Settings card genuinely changes the rendering — and it says so rather than treating it as routine.
