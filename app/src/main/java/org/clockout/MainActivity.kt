package org.clockout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.review.ReviewManagerFactory
import org.clockout.presentation.PadlaApp
import org.clockout.ui.theme.FrostedCanvas
import org.clockout.ui.theme.MyApplicationTheme
import org.clockout.review.InAppReviewController
import org.clockout.update.AppUpdateBanner
import org.clockout.update.AppUpdateController
import org.clockout.util.applyFrostedWindowChrome

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateController: AppUpdateController
    private lateinit var inAppReviewController: InAppReviewController

    private val updateFlowLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // Declining the update is fine; the check simply runs again next launch.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateController = AppUpdateController(AppUpdateManagerFactory.create(this))
        appUpdateController.checkForUpdate(updateFlowLauncher)
        inAppReviewController = InAppReviewController(ReviewManagerFactory.create(this))
        
        // Transparent behind both system bars; shared with BreathingStyleActivity so the two
        // cannot drift apart.
        applyFrostedWindowChrome()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FrostedCanvas
                ) {
                    val updateState by appUpdateController.state.collectAsStateWithLifecycle()

                    PadlaApp(
                        onRequestReview = { inAppReviewController.requestReview(this@MainActivity) },
                        overlay = {
                            AppUpdateBanner(
                                state = updateState,
                                onInstall = { appUpdateController.completeUpdate() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The download may have finished while the app was in the background.
        appUpdateController.resumeIfDownloaded()
    }

    override fun onDestroy() {
        appUpdateController.unregister()
        super.onDestroy()
    }
}

@Preview(showBackground = true)
@Composable
fun PadlaAppPreview() {
    MyApplicationTheme {
        PadlaApp()
    }
}
