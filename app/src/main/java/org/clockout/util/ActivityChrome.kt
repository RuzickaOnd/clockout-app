package org.clockout.util

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/**
 * Makes the window transparent behind both system bars and tells the platform not to paint a
 * scrim over either of them.
 *
 * Every Activity in the app has to do this identically: the frosted glass background is a single
 * dark canvas with ambient glows in its corners, and a system bar that draws its own colour cuts
 * a visible band across it. Sharing one function is what stops a second Activity from quietly
 * shipping with a grey status bar.
 */
fun ComponentActivity.applyFrostedWindowChrome() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
    )

    WindowCompat.setDecorFitsSystemWindows(window, false)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.isAppearanceLightStatusBars = false
    insetsController.isAppearanceLightNavigationBars = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
    }

    window.navigationBarColor = AndroidColor.TRANSPARENT
    window.statusBarColor = AndroidColor.TRANSPARENT
}
