package org.clockout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.presentation.path.PathScreen
import org.clockout.presentation.appContainer
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.util.applyFrostedWindowChrome

/**
 * The path explained, reached from the home card and from the About card in Settings. An
 * Activity of its own for the same reasons as [BreathingStyleActivity]: it is a peer of neither
 * Home nor History, and has no place on the navigation rail.
 */
class PathActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFrostedWindowChrome()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FrostedCanvas) {
                    val container = appContainer()
                    val language by container.settings.getLanguage()
                        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
                    // Null until Room answers, so the first frame does not show a newcomer's path.
                    // Remembered, so a change of language does not restart the collection.
                    val progressFlow = remember(container) { container.progress.getPathProgress() }
                    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = null as PathProgress?)

                    AppLocalization(language) {
                        progress?.let { PathScreen(progress = it, onBack = { finish() }) }
                    }
                }
            }
        }
    }
}
