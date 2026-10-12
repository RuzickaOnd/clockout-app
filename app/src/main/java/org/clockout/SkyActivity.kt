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
import org.clockout.domain.model.SkyData
import org.clockout.presentation.appContainer
import org.clockout.presentation.sky.SkyScreen
import org.clockout.presentation.util.AppLocalization
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.util.applyFrostedWindowChrome

/**
 * The night sky, reached from the preview card in History. An Activity of its own for the same
 * reasons as [PathActivity].
 */
class SkyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFrostedWindowChrome()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FrostedCanvas) {
                    val repository = appContainer().ritualRepository
                    val language by repository.getLanguage()
                        .collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
                    // Null until Room answers: drawing an empty sky first would flash the newcomer state.
                    // Remembered, so a change of language does not restart the collection.
                    val skyFlow = remember(repository) { repository.getSky() }
                    val sky by skyFlow.collectAsStateWithLifecycle(initialValue = null as SkyData?)

                    AppLocalization(language) {
                        sky?.let { SkyScreen(data = it, language = language, onBack = { finish() }) }
                    }
                }
            }
        }
    }
}
