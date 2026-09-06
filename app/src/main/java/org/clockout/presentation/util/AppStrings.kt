package org.clockout.presentation.util

import org.clockout.domain.model.AppLanguage

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
    val weekendSlotBadge: String = when (language) {
        AppLanguage.CZECH -> "VÍKEND"
        AppLanguage.ENGLISH -> "WEEKEND"
    }
    val weekendRestTitle: String = when (language) {
        AppLanguage.CZECH -> "Dnes máš volno"
        AppLanguage.ENGLISH -> "Today is yours"
    }
    val weekendRestDesc: String = when (language) {
        AppLanguage.CZECH -> "Pracovní týden je za tebou. Rituál se vrátí v pondělí."
        AppLanguage.ENGLISH -> "The work week is behind you. The ritual returns on Monday."
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
    fun daysClosedSummary(count: Int, target: Int): String = when (language) {
        AppLanguage.CZECH -> "$count z $target dní uzavřeno"
        AppLanguage.ENGLISH -> "$count of $target days closed"
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
    val includeWeekendsTitle: String = when (language) {
        AppLanguage.CZECH -> "Zahrnout víkendy"
        AppLanguage.ENGLISH -> "Include weekends"
    }
    val includeWeekendsDesc: String = when (language) {
        AppLanguage.CZECH -> "Zapnuto: rituál i připomínka běží 7 dní v týdnu. Vypnuto: jen pondělí až pátek."
        AppLanguage.ENGLISH -> "On: the ritual and its reminder run all 7 days. Off: Monday to Friday only."
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
        AppLanguage.CZECH -> "Zvuk a doprovod při dýchání"
        AppLanguage.ENGLISH -> "Sound & guidance during breathing"
    }
    val ambientSoundDesc: String = when (language) {
        AppLanguage.CZECH -> "Uvolňující zvukový doprovod sladěný s dechovým cvičením pro snazší zklidnění."
        AppLanguage.ENGLISH -> "Relaxing audio accompaniment synchronized with breathing for effortless grounding."
    }
    val soundStyleTitle: String = when (language) {
        AppLanguage.CZECH -> "Styl meditačního zvuku"
        AppLanguage.ENGLISH -> "Meditation sound style"
    }
    val soundStyleDesc: String = when (language) {
        AppLanguage.CZECH -> "Vyber si styl podkresu, který ti nejlépe vyhovuje pro večerní zklidnění."
        AppLanguage.ENGLISH -> "Choose the audio style that best supports your evening wind-down."
    }
    val soundPreviewButton: String = when (language) {
        AppLanguage.CZECH -> "Vyzkoušet"
        AppLanguage.ENGLISH -> "Preview"
    }
    val soundPreviewPlaying: String = when (language) {
        AppLanguage.CZECH -> "Hraje ukázka..."
        AppLanguage.ENGLISH -> "Playing sample..."
    }
    val customSoundSectionTitle: String = when (language) {
        AppLanguage.CZECH -> "Vlastní zvuky"
        AppLanguage.ENGLISH -> "Your own sounds"
    }
    val customSoundGroupLabel: String = when (language) {
        AppLanguage.CZECH -> "VLASTNÍ ZVUKY"
        AppLanguage.ENGLISH -> "YOUR OWN SOUNDS"
    }
    val builtInSoundGroupLabel: String = when (language) {
        AppLanguage.CZECH -> "PŘEDPŘIPRAVENÉ"
        AppLanguage.ENGLISH -> "BUILT-IN"
    }
    val uploadCustomSoundButton: String = when (language) {
        AppLanguage.CZECH -> "Nahrát vlastní zvuk"
        AppLanguage.ENGLISH -> "Upload your own sound"
    }
    val uploadingCustomSound: String = when (language) {
        AppLanguage.CZECH -> "Nahrávám soubor…"
        AppLanguage.ENGLISH -> "Importing file…"
    }
    val customSoundHint: String = when (language) {
        AppLanguage.CZECH -> "Podporované formáty: MP3, WAV, OGG, M4A, FLAC (max. 25 MB, až 10 souborů). Soubor se uloží do aplikace a při dýchání se přehrává ve smyčce."
        AppLanguage.ENGLISH -> "Supported formats: MP3, WAV, OGG, M4A, FLAC (max 25 MB, up to 10 files). The file is stored inside the app and loops during the breathing exercise."
    }
    val deleteCustomSoundTitle: String = when (language) {
        AppLanguage.CZECH -> "Smazat vlastní zvuk?"
        AppLanguage.ENGLISH -> "Delete your own sound?"
    }
    fun deleteCustomSoundText(name: String): String = when (language) {
        AppLanguage.CZECH -> "Soubor „$name“ bude z aplikace odstraněn."
        AppLanguage.ENGLISH -> "The file \"$name\" will be removed from the app."
    }
    val customSoundErrorUnsupported: String = when (language) {
        AppLanguage.CZECH -> "Tento soubor se nepodařilo přehrát. Vyber prosím platný zvukový soubor."
        AppLanguage.ENGLISH -> "This file could not be played. Please choose a valid audio file."
    }
    val customSoundErrorTooLarge: String = when (language) {
        AppLanguage.CZECH -> "Soubor je příliš velký. Maximální velikost je 25 MB."
        AppLanguage.ENGLISH -> "The file is too large. The maximum size is 25 MB."
    }
    val customSoundErrorLimit: String = when (language) {
        AppLanguage.CZECH -> "Můžeš mít uložených nejvýše 10 vlastních zvuků. Nejprve nějaký smaž."
        AppLanguage.ENGLISH -> "You can keep at most 10 custom sounds. Delete one first."
    }
    val customSoundErrorRead: String = when (language) {
        AppLanguage.CZECH -> "Soubor se nepodařilo načíst. Zkus to prosím znovu."
        AppLanguage.ENGLISH -> "The file could not be read. Please try again."
    }
    val aboutTitle: String = when (language) {
        AppLanguage.CZECH -> "O aplikaci Padla"
        AppLanguage.ENGLISH -> "About ClockOut"
    }
    val aboutDesc: String = when (language) {
        AppLanguage.CZECH -> "Padla ti pomáhá vědomě uzavřít pracovní den a přepnout se z práce do osobního času. Nejvíc se hodí na home office, kde mezi kanceláří a domovem chybí přirozená hranice.\n\nStačí tři minuty denního rituálu: zapíšeš, co se dnes povedlo, odložíš nedodělané úkoly na zítra, uklidíš si pracovní místo, zklidníš dech a symbolicky zhasneš pracovní den.\n\nPráce se ti tak nepřelévá do večera, snáz vypneš hlavu a druhý den začínáš odpočatý. Všechno zůstává jen v telefonu — žádné účty, žádný cloud."
        AppLanguage.ENGLISH -> "ClockOut helps you consciously close your workday and switch from work into your own time. It matters most when you work from home, where the boundary between the office and the living room is missing.\n\nAll it takes is a three-minute daily ritual: note what went well today, park unfinished tasks for tomorrow, reset your workspace, calm your breath, and symbolically switch the workday off.\n\nWork stops spilling into your evening, you unwind more easily, and you start the next day rested. Everything stays on your phone — no accounts, no cloud."
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
