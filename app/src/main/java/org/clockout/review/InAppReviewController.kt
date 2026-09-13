package org.clockout.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManager

/**
 * Drives the Play in-app review flow.
 *
 * Every Play call is best effort, exactly as in the update flow: a build that did not come from
 * the Play Store, or a user who is over Play's own request quota, simply sees nothing. The app
 * therefore never treats a missing dialog as an error, and never falls back to a prompt of its own.
 */
class InAppReviewController(private val reviewManager: ReviewManager) {

    fun requestReview(activity: Activity) {
        reviewManager.requestReviewFlow()
            .addOnSuccessListener { info ->
                reviewManager.launchReviewFlow(activity, info)
            }
            .addOnFailureListener {
                // Nothing to do: asking for a review is never worth bothering the user about.
            }
    }
}
