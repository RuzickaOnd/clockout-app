package org.clockout

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.clockout.domain.model.AppLanguage
import org.clockout.presentation.util.AppLocalization
import org.junit.Assert.assertNotNull
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

    @Test
    fun `the start ritual button keeps its deliberate wording in english`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.start_ritual))
            }
        }

        composeTestRule.onNodeWithText("Start ritual (2–3 min)").assertIsDisplayed()
    }

    @Test
    fun `the start ritual button keeps its deliberate wording in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.start_ritual))
            }
        }

        composeTestRule.onNodeWithText("Zahájit rituál (2–3 min)").assertIsDisplayed()
    }

    @Test
    fun `the czech app name is served under a czech provider`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.app_name))
            }
        }

        composeTestRule.onNodeWithText("Padla").assertIsDisplayed()
    }

    @Test
    fun `the breathing inhale cue follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.ritual_breathing_inhale))
            }
        }

        composeTestRule.onNodeWithText("Inhale...").assertIsDisplayed()
    }

    @Test
    fun `the breathing inhale cue follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.ritual_breathing_inhale))
            }
        }

        composeTestRule.onNodeWithText("Nádech...").assertIsDisplayed()
    }

    @Test
    fun `the breathing exhale cue follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.ritual_breathing_exhale))
            }
        }

        composeTestRule.onNodeWithText("Exhale...").assertIsDisplayed()
    }

    @Test
    fun `the breathing exhale cue follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.ritual_breathing_exhale))
            }
        }

        composeTestRule.onNodeWithText("Výdech...").assertIsDisplayed()
    }

    @Test
    fun `the about title follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.settings_about_title))
            }
        }

        composeTestRule.onNodeWithText("About ClockOut").assertIsDisplayed()
    }

    @Test
    fun `the about title follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.settings_about_title))
            }
        }

        composeTestRule.onNodeWithText("O aplikaci Padla").assertIsDisplayed()
    }

    @Test
    fun `the history nav label follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.nav_history))
            }
        }

        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun `the history nav label follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.nav_history))
            }
        }

        composeTestRule.onNodeWithText("Historie").assertIsDisplayed()
    }

    @Test
    fun `the settings nav label follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.nav_settings))
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `the settings nav label follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.nav_settings))
            }
        }

        composeTestRule.onNodeWithText("Nastavení").assertIsDisplayed()
    }

    @Test
    fun `the notifications disabled label follows the provider language`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.ENGLISH) {
                Text(stringResource(R.string.settings_notifications_disabled))
            }
        }

        composeTestRule.onNodeWithText("Disabled").assertIsDisplayed()
    }

    @Test
    fun `the notifications disabled label follows the provider language in czech`() {
        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                Text(stringResource(R.string.settings_notifications_disabled))
            }
        }

        composeTestRule.onNodeWithText("Vypnuto").assertIsDisplayed()
    }

    /**
     * Regrese: localizedFor() dřív vracelo createConfigurationContext() přímo, což je nový
     * Context nedohledatelný zpět k Activitě přes getBaseContext(). To tiše rozbilo každé
     * composition local dohledávané procházením řetězu ContextWrapperů - LocalActivity i
     * LocalActivityResultRegistryOwner - a spadlo na tom SettingsScreen, který uvnitř
     * AppLocalization volá rememberLauncherForActivityResult().
     */
    @Test
    fun `the activity and its result registry stay reachable under a localized provider`() {
        var activity: android.app.Activity? = null
        var registryOwner: androidx.activity.result.ActivityResultRegistryOwner? = null

        composeTestRule.setContent {
            AppLocalization(AppLanguage.CZECH) {
                activity = LocalActivity.current
                registryOwner = LocalActivityResultRegistryOwner.current
            }
        }

        assertNotNull("LocalActivity.current should resolve under AppLocalization", activity)
        assertNotNull(
            "LocalActivityResultRegistryOwner.current should resolve under AppLocalization",
            registryOwner
        )
    }
}
