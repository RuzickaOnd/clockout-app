package org.clockout.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

/**
 * Today's date, emitted again whenever the system says it may have changed: at midnight, after the
 * clock is set by hand, after a change of time zone. A timer counting down to midnight cannot do
 * this - its clock stops while the phone sleeps, so a screen left open overnight would wake up on
 * yesterday - and it would miss a new zone altogether.
 */
fun Context.todayFlow(now: () -> LocalDate = LocalDate::now): Flow<LocalDate> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(now())
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    // System broadcasts reach a receiver that is not exported; nothing else needs to.
    ContextCompat.registerReceiver(this@todayFlow, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    trySend(now())
    awaitClose { unregisterReceiver(receiver) }
}.distinctUntilChanged()
