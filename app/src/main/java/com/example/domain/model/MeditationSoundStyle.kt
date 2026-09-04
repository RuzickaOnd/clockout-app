package com.example.domain.model

enum class MeditationSoundStyle(val id: String) {
    TIBETAN_BOWL("tibetan_bowl"),
    RAIN_PINK_NOISE("rain_pink_noise"),
    BREATHING_WAVES("breathing_waves"),
    SILENT_HAPTIC("silent_haptic");

    fun label(language: AppLanguage): String = when (this) {
        TIBETAN_BOWL -> when (language) {
            AppLanguage.CZECH -> "Tibetská mísa a gong"
            AppLanguage.ENGLISH -> "Tibetan Singing Bowl & Gong"
        }
        RAIN_PINK_NOISE -> when (language) {
            AppLanguage.CZECH -> "Jemný déšť a růžový šum"
            AppLanguage.ENGLISH -> "Gentle Rain & Pink Noise"
        }
        BREATHING_WAVES -> when (language) {
            AppLanguage.CZECH -> "Dechové vlny (příliv a odliv)"
            AppLanguage.ENGLISH -> "Breathing Ocean Waves"
        }
        SILENT_HAPTIC -> when (language) {
            AppLanguage.CZECH -> "Pouze ticho a haptika"
            AppLanguage.ENGLISH -> "Silent & Haptics Only"
        }
    }

    fun description(language: AppLanguage): String = when (this) {
        TIBETAN_BOWL -> when (language) {
            AppLanguage.CZECH -> "Harmonický rezonující úder meditační mísy při začátku a změnách fází dechu"
            AppLanguage.ENGLISH -> "Warm harmonic resonance of a meditation bowl at start and breath shifts"
        }
        RAIN_PINK_NOISE -> when (language) {
            AppLanguage.CZECH -> "Klidný měkký šum evokující kapky letního deště bez ostrých tónů"
            AppLanguage.ENGLISH -> "Soothing soft ambient noise evoking summer raindrops without harsh frequencies"
        }
        BREATHING_WAVES -> when (language) {
            AppLanguage.CZECH -> "Příliv a odliv šumu plynule synchronizovaný s fázemi nádechu a výdechu"
            AppLanguage.ENGLISH -> "Ocean wave swells smoothly synchronized with your inhale and exhale rhythm"
        }
        SILENT_HAPTIC -> when (language) {
            AppLanguage.CZECH -> "Úplné ticho bez zvuku, pouze jemné uklidňující vibrace telefonu"
            AppLanguage.ENGLISH -> "Complete silence with gentle tactile phone vibration cues"
        }
    }

    companion object {
        fun fromId(id: String?): MeditationSoundStyle {
            return entries.firstOrNull { it.id == id } ?: TIBETAN_BOWL
        }
    }
}
