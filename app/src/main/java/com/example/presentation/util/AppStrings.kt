package com.example.presentation.util

import com.example.domain.model.AppLanguage

class AppStrings(val language: AppLanguage) {

    // Common
    val appName: String = when (language) {
        AppLanguage.CZECH -> "Padla"
        AppLanguage.ENGLISH -> "ClockOut"
    }
    val back: String = when (language) {
        AppLanguage.CZECH -> "Zpět"
        AppLanguage.ENGLISH -> "Back"
    }
    val cancel: String = when (language) {
        AppLanguage.CZECH -> "Zrušit"
        AppLanguage.ENGLISH -> "Cancel"
    }
    val delete: String = when (language) {
        AppLanguage.CZECH -> "Smazat"
        AppLanguage.ENGLISH -> "Delete"
    }

    // HomeScreen
    val appSubtitle: String = when (language) {
        AppLanguage.CZECH -> "Přechod práce → volno"
        AppLanguage.ENGLISH -> "Work → Leisure transition"
    }
    val navHistory: String = when (language) {
        AppLanguage.CZECH -> "Historie"
        AppLanguage.ENGLISH -> "History"
    }
    val navSettings: String = when (language) {
        AppLanguage.CZECH -> "Nastavení"
        AppLanguage.ENGLISH -> "Settings"
    }
    val slotWorkdayEnd: String = when (language) {
        AppLanguage.CZECH -> "SLOT: KONEC PRÁCE (16–21h)"
        AppLanguage.ENGLISH -> "SLOT: WORKDAY END (16–21h)"
    }
    val todayClosedBadge: String = when (language) {
        AppLanguage.CZECH -> "DNES UZAVŘENO ✓"
        AppLanguage.ENGLISH -> "TODAY CLOSED ✓"
    }
    val workdayDoneTitle: String = when (language) {
        AppLanguage.CZECH -> "Pracovní den je hotov"
        AppLanguage.ENGLISH -> "Workday is completed"
    }
    fun workdayDoneDesc(moodLabel: String): String = when (language) {
        AppLanguage.CZECH -> "Nálada: $moodLabel. Užij si volný večer pro sebe a své tělo."
        AppLanguage.ENGLISH -> "Mood: $moodLabel. Enjoy your free evening for yourself and your mind."
    }
    val repeatRitual: String = when (language) {
        AppLanguage.CZECH -> "Zopakovat rituál"
        AppLanguage.ENGLISH -> "Repeat ritual"
    }
    val closeDayTitle: String = when (language) {
        AppLanguage.CZECH -> "Uzavři vědomě dnešní den"
        AppLanguage.ENGLISH -> "Close today's workday mindfully"
    }
    val closeDayDesc: String = when (language) {
        AppLanguage.CZECH -> "Krátký 3minutový rituál ti pomůže odložit pracovní myšlenky a přepnout hlavu do volna."
        AppLanguage.ENGLISH -> "A short 3-minute ritual helps you leave work thoughts behind and transition your mind into leisure."
    }
    val todayQuestionLabel: String = when (language) {
        AppLanguage.CZECH -> "DNEŠNÍ OTÁZKA"
        AppLanguage.ENGLISH -> "TODAY'S QUESTION"
    }
    val startRitualButton: String = when (language) {
        AppLanguage.CZECH -> "Zahájit rituál (2–3 min)"
        AppLanguage.ENGLISH -> "Start ritual (2–3 min)"
    }
    val thisWeekTitle: String = when (language) {
        AppLanguage.CZECH -> "Tento týden"
        AppLanguage.ENGLISH -> "This week"
    }
    fun daysClosedSummary(count: Int): String = when (language) {
        AppLanguage.CZECH -> "$count z 5 pracovních dní uzavřeno"
        AppLanguage.ENGLISH -> "$count of 5 workdays closed"
    }
    val whyRitualTitle: String = when (language) {
        AppLanguage.CZECH -> "Proč tento rituál?"
        AppLanguage.ENGLISH -> "Why this ritual?"
    }
    val whyRitualDesc: String = when (language) {
        AppLanguage.CZECH -> "Při práci z domova chybí fyzická cesta z kanceláře, která dříve přirozeně oddělovala den. Tyto 3 minuty ti dají jasný signál, že práce skončila a začíná tvůj vlastní čas."
        AppLanguage.ENGLISH -> "When working from home, the physical commute that used to separate the day is missing. These 3 minutes give your brain a clear signal that work is over and your own time begins."
    }

    // RitualScreen
    val ritualHeaderTitle: String = when (language) {
        AppLanguage.CZECH -> "RITUÁL UZAVŘENÍ"
        AppLanguage.ENGLISH -> "CLOSING RITUAL"
    }
    fun stepIndicator(step: Int, total: Int = 4): String = when (language) {
        AppLanguage.CZECH -> "Krok $step ze $total"
        AppLanguage.ENGLISH -> "Step $step of $total"
    }
    val closeRitualDialogTitle: String = when (language) {
        AppLanguage.CZECH -> "Přerušit rituál?"
        AppLanguage.ENGLISH -> "Interrupt ritual?"
    }
    val closeRitualDialogText: String = when (language) {
        AppLanguage.CZECH -> "Rituál trvá jen 2–3 minuty a pomůže ti přepnout hlavu do večerního klidu."
        AppLanguage.ENGLISH -> "The ritual takes only 2–3 minutes and helps transition your mind into evening rest."
    }
    val closeRitualConfirm: String = when (language) {
        AppLanguage.CZECH -> "Ukončit"
        AppLanguage.ENGLISH -> "Exit"
    }
    val closeRitualContinue: String = when (language) {
        AppLanguage.CZECH -> "Pokračovat"
        AppLanguage.ENGLISH -> "Continue"
    }

    // Step 1: Question
    val questionCategory: String = when (language) {
        AppLanguage.CZECH -> "VĚDOMÉ UZAVŘENÍ DNE"
        AppLanguage.ENGLISH -> "MINDFUL CLOSURE"
    }
    val questionInstruction: String = when (language) {
        AppLanguage.CZECH -> "Pojmenuj jednu věc, kterou už dnes nemusíš řešit. Nech ji tady."
        AppLanguage.ENGLISH -> "Name one thing you don't need to carry anymore today. Leave it right here."
    }
    val customAnswerPlaceholder: String = when (language) {
        AppLanguage.CZECH -> "Nebo napiš vlastní myšlenku..."
        AppLanguage.ENGLISH -> "Or write your own thought..."
    }
    val buttonContinueToBreathing: String = when (language) {
        AppLanguage.CZECH -> "Pokračovat na dýchání"
        AppLanguage.ENGLISH -> "Continue to breathing"
    }

    // Step 2: Breathing
    val breathingCategory: String = when (language) {
        AppLanguage.CZECH -> "DECHOVÁ SEKVENCE"
        AppLanguage.ENGLISH -> "BREATHING SEQUENCE"
    }
    val breathingTitle: String = when (language) {
        AppLanguage.CZECH -> "Vědomý dech 4s / 4s"
        AppLanguage.ENGLISH -> "Mindful breath 4s / 4s"
    }
    val breathingInhale: String = when (language) {
        AppLanguage.CZECH -> "Nádech..."
        AppLanguage.ENGLISH -> "Inhale..."
    }
    val breathingExhale: String = when (language) {
        AppLanguage.CZECH -> "Výdech..."
        AppLanguage.ENGLISH -> "Exhale..."
    }
    val breathingInhaleDesc: String = when (language) {
        AppLanguage.CZECH -> "Naplň plíce klidem a čerstvým vzduchem"
        AppLanguage.ENGLISH -> "Fill your lungs with calm and fresh air"
    }
    val breathingExhaleDesc: String = when (language) {
        AppLanguage.CZECH -> "S výdechem vypusť veškeré pracovní napětí"
        AppLanguage.ENGLISH -> "Release all work tension as you exhale"
    }
    val buttonFinishBreathing: String = when (language) {
        AppLanguage.CZECH -> "Pokračovat na uzavření"
        AppLanguage.ENGLISH -> "Continue to closure"
    }
    val buttonSkipBreathing: String = when (language) {
        AppLanguage.CZECH -> "Přeskočit dýchání"
        AppLanguage.ENGLISH -> "Skip breathing"
    }

    // Step 3: Visual Closure
    val closureCategory: String = when (language) {
        AppLanguage.CZECH -> "VIZUÁLNÍ UZAVŘENÍ"
        AppLanguage.ENGLISH -> "VISUAL CLOSURE"
    }
    val closureTapPrompt: String = when (language) {
        AppLanguage.CZECH -> "Klepnutím uzavři den"
        AppLanguage.ENGLISH -> "Tap to close the day"
    }
    val closureDoneTitle: String = when (language) {
        AppLanguage.CZECH -> "Pracovní den je uzavřen"
        AppLanguage.ENGLISH -> "Workday is closed"
    }
    val closureLampTurnOffPrompt: String = when (language) {
        AppLanguage.CZECH -> "Zhasni pracovní lampu a symbolicky zavři dnešní den."
        AppLanguage.ENGLISH -> "Turn off the desk lamp and symbolically close the day."
    }
    val closureFinishedPrompt: String = when (language) {
        AppLanguage.CZECH -> "Práce skončila. Zbytek dne patří jen tobě."
        AppLanguage.ENGLISH -> "Work is done. The rest of the day is yours."
    }
    val closureDoneMessage: String = when (language) {
        AppLanguage.CZECH -> "Všechny zprávy a úkoly počkají do zítra. Pro dnešek máš padla."
        AppLanguage.ENGLISH -> "All messages and tasks will wait until tomorrow. You've clocked out today."
    }
    val buttonRecordMood: String = when (language) {
        AppLanguage.CZECH -> "Zaznamenat náladu"
        AppLanguage.ENGLISH -> "Record mood"
    }

    // Step 4: Mood Log
    val moodCategory: String = when (language) {
        AppLanguage.CZECH -> "RYCHLÝ ZÁZNAM"
        AppLanguage.ENGLISH -> "QUICK LOG"
    }
    val moodTitle: String = when (language) {
        AppLanguage.CZECH -> "Jak se na konci dne cítíš?"
        AppLanguage.ENGLISH -> "How do you feel at the end of the day?"
    }
    val moodSubtitle: String = when (language) {
        AppLanguage.CZECH -> "Jedno klepnutí pro zaznamenání tvého rozpoložení."
        AppLanguage.ENGLISH -> "One tap to capture your evening mindset."
    }
    val buttonCompleteRitual: String = when (language) {
        AppLanguage.CZECH -> "Dokončit rituál"
        AppLanguage.ENGLISH -> "Complete ritual"
    }
    val buttonSkipMood: String = when (language) {
        AppLanguage.CZECH -> "Přeskočit a zavřít"
        AppLanguage.ENGLISH -> "Skip and close"
    }
    val ritualCompletedNotice: String = when (language) {
        AppLanguage.CZECH -> "Rituál dokončen!"
        AppLanguage.ENGLISH -> "Ritual completed!"
    }
    val returnHome: String = when (language) {
        AppLanguage.CZECH -> "Návrat na hlavní stránku"
        AppLanguage.ENGLISH -> "Return to home page"
    }

    // HistoryScreen
    val historyTitle: String = when (language) {
        AppLanguage.CZECH -> "Historie rituálů"
        AppLanguage.ENGLISH -> "Ritual History"
    }
    val clearHistoryTooltip: String = when (language) {
        AppLanguage.CZECH -> "Smazat historii"
        AppLanguage.ENGLISH -> "Clear history"
    }
    val clearHistoryDialogTitle: String = when (language) {
        AppLanguage.CZECH -> "Smazat historii rituálů?"
        AppLanguage.ENGLISH -> "Delete ritual history?"
    }
    val clearHistoryDialogText: String = when (language) {
        AppLanguage.CZECH -> "Tato akce vymaže všechny dosud zaznamenané ukončené dny."
        AppLanguage.ENGLISH -> "This action will delete all recorded completed workdays."
    }
    val emptyHistoryTitle: String = when (language) {
        AppLanguage.CZECH -> "Zatím žádné záznamy"
        AppLanguage.ENGLISH -> "No records yet"
    }
    val emptyHistoryDesc: String = when (language) {
        AppLanguage.CZECH -> "Až dnes po práci projdeš svým prvním 3minutovým rituálem, objeví se zde tvá nálada i myšlenka."
        AppLanguage.ENGLISH -> "Once you complete your first 3-minute ritual after work today, your mood and reflection will appear here."
    }
    fun totalClosedDays(count: Int): String = when (language) {
        AppLanguage.CZECH -> "Celkem uzavřeno: $count dnů"
        AppLanguage.ENGLISH -> "Total closed: $count days"
    }
    val padlaBadge: String = when (language) {
        AppLanguage.CZECH -> "Padla ✓"
        AppLanguage.ENGLISH -> "Done ✓"
    }

    // SettingsScreen
    val settingsTitle: String = when (language) {
        AppLanguage.CZECH -> "Nastavení"
        AppLanguage.ENGLISH -> "Settings"
    }
    val languageSectionTitle: String = "App Language"
    val languageSectionDesc: String = ""
    val workdayEndTimeTitle: String = when (language) {
        AppLanguage.CZECH -> "Čas konce pracovního dne"
        AppLanguage.ENGLISH -> "Workday end time"
    }
    val notificationsDisabled: String = when (language) {
        AppLanguage.CZECH -> "Vypnuto"
        AppLanguage.ENGLISH -> "Disabled"
    }
    val notificationsEnabled: String = when (language) {
        AppLanguage.CZECH -> "Zapnuto"
        AppLanguage.ENGLISH -> "Enabled"
    }
    val permissionDialogTitle: String = when (language) {
        AppLanguage.CZECH -> "Povolení notifikací"
        AppLanguage.ENGLISH -> "Notification Permission"
    }
    val permissionDeniedMessage: String = when (language) {
        AppLanguage.CZECH -> "Pro zasílání pravidelných připomenutí konce práce je potřeba povolit notifikace v systému."
        AppLanguage.ENGLISH -> "Notification permission is required to send daily reminders when your workday ends."
    }
    fun currentlySetTo(time: String): String = when (language) {
        AppLanguage.CZECH -> "Aktuálně nastaveno: $time"
        AppLanguage.ENGLISH -> "Currently set to: $time"
    }
    val quickPresetsLabel: String = when (language) {
        AppLanguage.CZECH -> "Rychlá volba obvyklého času padla:"
        AppLanguage.ENGLISH -> "Quick presets for usual end of work:"
    }
    val testModeTitle: String = when (language) {
        AppLanguage.CZECH -> "Rituál dostupný kdykoliv"
        AppLanguage.ENGLISH -> "Ritual available anytime"
    }
    val testModeDesc: String = when (language) {
        AppLanguage.CZECH -> "Umožní spustit rituál i mimo hlavní okno (16:00–21:00) pro flexibilní pracovní dobu a testování."
        AppLanguage.ENGLISH -> "Allows starting the ritual outside the main 16:00–21:00 window for flexible schedules and testing."
    }
    val ambientSoundTitle: String = when (language) {
        AppLanguage.CZECH -> "Ambientní zvuk při dýchání"
        AppLanguage.ENGLISH -> "Ambient breathing sound"
    }
    val ambientSoundDesc: String = when (language) {
        AppLanguage.CZECH -> "Jemný uklidňující harmonický tón s plynulým nádechem a výdechem pro hlubší uvolnění."
        AppLanguage.ENGLISH -> "A gentle soothing harmonic tone for smooth inhaling and exhaling to deepen relaxation."
    }
    val aboutTitle: String = when (language) {
        AppLanguage.CZECH -> "O aplikaci Padla"
        AppLanguage.ENGLISH -> "About ClockOut"
    }
    val aboutDesc: String = when (language) {
        AppLanguage.CZECH -> "Appka, co ti pomůže vědomě uzavřít pracovní den a přepnout do večera na home office.\n\nNa rozdíl od běžných meditačních appek nenabízí stovky nahrávek k vybírání, ale jeden konkrétní 3minutový denní rituál bez rozhodovací paralýzy."
        AppLanguage.ENGLISH -> "An app designed to help you mindfully close your workday and transition into your evening when working from home.\n\nUnlike traditional meditation apps with endless catalogs to choose from, ClockOut provides one singular 3-minute daily ritual with zero decision fatigue."
    }
    val clearDataButton: String = when (language) {
        AppLanguage.CZECH -> "Vymazat data a historii"
        AppLanguage.ENGLISH -> "Clear data and history"
    }
    val clearDataDialogTitle: String = when (language) {
        AppLanguage.CZECH -> "Vymazat data aplikace?"
        AppLanguage.ENGLISH -> "Clear app data?"
    }
    val clearDataDialogText: String = when (language) {
        AppLanguage.CZECH -> "Tato akce smaže historii zaznamenaných rituálů."
        AppLanguage.ENGLISH -> "This action will delete all recorded ritual history."
    }
    val clearDataConfirm: String = when (language) {
        AppLanguage.CZECH -> "Vymazat"
        AppLanguage.ENGLISH -> "Clear"
    }
}
