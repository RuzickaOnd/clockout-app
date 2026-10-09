package org.clockout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.clockout.domain.model.AppLanguage
import org.clockout.domain.model.PathProgress
import org.clockout.presentation.path.PathScreen
import org.clockout.presentation.rememberRitualRepository
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
                    val repository = rememberRitualRepository()
                    val language by repository.getLanguage()
                        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
                    val progress by repository.getPathProgress()
                        .collectAsStateWithLifecycle(initialValue = PathProgress.EMPTY)

                    AppLocalization(language) {
                        PathScreen(progress = progress, onBack = { finish() })
                    }
                }
            }
        }
    }
}
