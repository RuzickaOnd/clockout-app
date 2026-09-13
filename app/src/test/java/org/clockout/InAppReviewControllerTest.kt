package org.clockout

import android.app.Activity
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import org.clockout.review.InAppReviewController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InAppReviewControllerTest {

    private lateinit var activity: Activity

    /** Wraps Play's own test double so the launch can be observed without faking [ReviewInfo]. */
    private class RecordingReviewManager(
        private val delegate: ReviewManager,
        private val requestFailure: Exception? = null
    ) : ReviewManager {
        var launchedFrom: Activity? = null

        override fun requestReviewFlow(): Task<ReviewInfo> =
            requestFailure?.let { Tasks.forException(it) } ?: delegate.requestReviewFlow()

        override fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Task<Void> {
            launchedFrom = activity
            return delegate.launchReviewFlow(activity, reviewInfo)
        }
    }

    private fun playStore(requestFailure: Exception? = null) = RecordingReviewManager(
        FakeReviewManager(ApplicationProvider.getApplicationContext()),
        requestFailure
    )

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun `a successful request launches the review flow from the activity`() {
        val playStore = playStore()

        InAppReviewController(playStore).requestReview(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(activity, playStore.launchedFrom)
    }

    /** A build that never came from the Play Store must fail silently, not crash the ritual. */
    @Test
    fun `a failed request is swallowed`() {
        val playStore = playStore(requestFailure = IllegalStateException("not installed by Play"))

        InAppReviewController(playStore).requestReview(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertNull(playStore.launchedFrom)
    }
}
