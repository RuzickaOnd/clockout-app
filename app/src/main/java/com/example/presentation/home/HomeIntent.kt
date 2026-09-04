package com.example.presentation.home

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class ToggleTestMode(val enabled: Boolean) : HomeIntent
}
