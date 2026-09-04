package com.example.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CZECH("cs", "Čeština");

    companion object {
        val DEFAULT = ENGLISH

        fun fromCode(code: String?): AppLanguage = when (code?.lowercase()) {
            "cs" -> CZECH
            else -> ENGLISH
        }

        fun fromSystemLocale(): AppLanguage {
            val systemCode = java.util.Locale.getDefault().language
            return when {
                systemCode.equals("cs", ignoreCase = true) -> CZECH
                else -> DEFAULT
            }
        }
    }
}

enum class LanguageOption(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    CZECH("cs");

    fun label(language: AppLanguage): String = when (this) {
        SYSTEM -> when (language) {
            AppLanguage.CZECH -> "Dle systému"
            AppLanguage.ENGLISH -> "System default"
        }
        ENGLISH -> "English"
        CZECH -> "Čeština"
    }

    fun resolveEffectiveLanguage(): AppLanguage = when (this) {
        SYSTEM -> AppLanguage.fromSystemLocale()
        ENGLISH -> AppLanguage.ENGLISH
        CZECH -> AppLanguage.CZECH
    }

    companion object {
        val DEFAULT = SYSTEM

        fun fromCode(code: String?): LanguageOption = when (code?.lowercase()) {
            "system", "default", null, "" -> SYSTEM
            "cs" -> CZECH
            "en" -> ENGLISH
            else -> SYSTEM
        }
    }
}

