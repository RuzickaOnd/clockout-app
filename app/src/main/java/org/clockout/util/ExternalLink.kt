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
