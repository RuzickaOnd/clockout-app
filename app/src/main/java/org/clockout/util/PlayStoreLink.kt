package org.clockout.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens this app's Play Store listing, where the rating stars sit right under the install buttons.
 *
 * Play exposes no public deep link straight to the review form - the closest thing to that is the
 * in-app review flow ([org.clockout.review.InAppReviewController]) - so the listing is as far as a
 * button can take the user. The `market://` scheme hands the job to the Play app; a device without
 * one falls back to the browser, which is why neither path needs the INTERNET permission the app
 * deliberately ships without.
 */
fun Context.openPlayStoreListing() {
    val playAppIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(playAppIntent)
    } catch (_: ActivityNotFoundException) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(browserIntent)
        } catch (_: ActivityNotFoundException) {
            // No Play app and no browser: nothing sensible left to do.
        }
    }
}
