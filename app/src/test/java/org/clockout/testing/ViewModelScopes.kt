package org.clockout.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel

/**
 * The view models a test created, so its tearDown can cancel their scopes first. Nothing clears a
 * view model a test builds by hand, so its collectors outlive the test: resumed later on a DataStore
 * or Room thread, they touch Dispatchers.Main while this test resets it or the next one sets it,
 * and kotlinx-coroutines-test fails one of them with "Dispatchers.Main is used concurrently with
 * setting it". Cancel before resetting Main and before closing the database.
 */
class ViewModelScopes {
    private val created = mutableListOf<ViewModel>()

    fun <T : ViewModel> track(viewModel: T): T = viewModel.also { created += it }

    fun cancelAll() {
        created.forEach { it.viewModelScope.cancel() }
        created.clear()
    }
}
