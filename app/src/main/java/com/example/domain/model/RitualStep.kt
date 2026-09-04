package com.example.domain.model

sealed interface RitualStep {
    data object Question : RitualStep
    data object Breathing : RitualStep
    data object Closing : RitualStep
    data object Log : RitualStep
}
