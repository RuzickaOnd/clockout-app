import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Release signing credentials come from local.properties (git-ignored) so the keystore and its
// passwords never reach version control; a build machine without that file - CI, or a fresh clone -
// falls back to the identically named environment variables. When neither source yields an existing
// keystore the release signing config is not created at all, so debug builds and clean checkouts
// still configure instead of failing.
val localProperties = Properties().apply {
  val file = rootProject.file("local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

fun signingSecret(name: String): String? =
  (localProperties.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

android {
  namespace = "org.clockout"
  compileSdk { version = release(37) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "org.clockout"
    minSdk = 26
    targetSdk = 37
    versionCode = 1000301
    versionName = "1.0.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val releaseKeystore =
      signingSecret("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
    if (releaseKeystore?.exists() == true) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = signingSecret("RELEASE_STORE_PASSWORD")
        keyAlias = signingSecret("RELEASE_KEY_ALIAS")
        keyPassword = signingSecret("RELEASE_KEY_PASSWORD")
        // v3 on top of the AGP default enables signing-key rotation later on.
        enableV3Signing = true
      }
    }
    val debugKeystore = file("${rootDir}/debug.keystore")
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8 shrinks and obfuscates the release build; AGP then ships the mapping file inside the
      // AAB, which is what Play uses to deobfuscate crash and ANR stack traces.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
      // Uploads native debug symbols with the bundle. Today it emits nothing: the only .so files
      // in the app come from AndroidX AARs and arrive already stripped, so AGP has nothing to
      // extract and Play keeps warning about missing symbols. Kept so the symbols do ship if the
      // app ever bundles native code of its own.
      ndk { debugSymbolLevel = "FULL" }
    }
    debug {
      signingConfigs.findByName("debugConfig")?.let {
        signingConfig = it
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
  // Aplikace přepíná jazyk v appce nezávisle na systémovém locale, takže musí mít všechny
  // jazykové zdroje v základním APK - jinak by Play instaloval jen jazykový split odpovídající
  // systémovým locale zařízení a přepnutí na jazyk bez nainstalovaného splitu by potichu spadlo
  // zpátky na angličtinu. Při dvou locale je cena za to pár kB navíc ve staženém balíčku.
  bundle {
    language {
      enableSplit = false
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Without a signing config AGP would happily package an unsigned release artifact, so refuse to
// build one rather than let an unshippable APK or AAB look like a successful release.
if (android.signingConfigs.findByName("release") == null) {
  tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    doFirst {
      throw GradleException(
        "Cannot build a release: no signing keystore was found. Set RELEASE_STORE_FILE, " +
          "RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD in local.properties " +
          "(RELEASE_STORE_FILE is resolved relative to the repository root) or supply them as " +
          "environment variables."
      )
    }
  }
}

// Export the Room schema so future schema changes can be diffed and migrated.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  // Pins androidx.fragment above the 1.1.0 that play-services-basement pulls in, which the
  // ActivityResult APIs require (InvalidFragmentVersionForActivityResult).
  implementation(libs.androidx.fragment)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // Uncomment both together to use App Check; the debug provider must be swapped for a
  // debugImplementation, otherwise it ships in the release build as well.
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  implementation(libs.play.app.update)
  implementation(libs.play.review)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
