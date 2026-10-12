package org.clockout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.BreathingStyle
import org.clockout.presentation.appContainer
import org.clockout.presentation.settings.BreathingStyleScreen
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.util.applyFrostedWindowChrome

/**
 * Picking the breathing visual, hosted in its own Activity rather than as a fifth destination
 * inside [org.clockout.presentation.PadlaApp].
 *
 * The app's navigation is a hand-rolled sealed interface with a saver that writes screens down as
 * plain integers, and every top-level destination is reachable from the navigation rail. A picker
 * that hangs off Settings fits neither: it has no rail entry of its own, and adding one would put
 * it next to Home and History as though it were a peer. As a separate Activity it gets the system
 * back stack, state restoration across a rotation and a fold, and a task the launcher never shows
 * for free, and the rail simply is not part of it.
 *
 * What it does have to bring itself is the window chrome and the language: the app switches
 * language independently of the system locale, so the screen has to be wrapped in
 * [AppLocalization] exactly as [org.clockout.presentation.PadlaApp] wraps its own content, and the
 * container has to be reached above that wrapper so DataStore and Room never see a
 * locale-overridden context.
 */
class BreathingStyleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFrostedWindowChrome()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FrostedCanvas) {
                    val settings = appContainer().settings
                    val language by settings.getLanguage()
                        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
                    val style by settings.getBreathingStyle()
                        .collectAsStateWithLifecycle(initialValue = BreathingStyle.CLASSIC_CIRCLE)

                    AppLocalization(language) {
                        BreathingStyleScreen(
                            selectedStyle = style,
                            onStyleSelected = { chosen ->
                                lifecycleScope.launch { settings.setBreathingStyle(chosen) }
                            },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}
