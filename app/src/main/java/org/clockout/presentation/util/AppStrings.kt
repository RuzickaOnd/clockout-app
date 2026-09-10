package org.clockout.presentation.util

import org.clockout.domain.model.AppLanguage

class AppStrings(val language: AppLanguage) {

    // Common
    /** Still used by SettingsScreen, which Task 6 migrates to resources; that task removes this. */
    val back: String = when (language) {
        AppLanguage.CZECH -> "Zpět"
        AppLanguage.ENGLISH -> "Back"
    }
    /** Still used by SettingsScreen, which Task 6 migrates to resources; that task removes this. */
    val cancel: String = when (language) {
        AppLanguage.CZECH -> "Zrušit"
        AppLanguage.ENGLISH -> "Cancel"
    }
    /** Still used by SettingsScreen, which Task 6 migrates to resources; that task removes this. */
    val delete: String = when (language) {
        AppLanguage.CZECH -> "Smazat"
        AppLanguage.ENGLISH -> "Delete"
    }

    // HomeScreen
    val navHome: String = when (language) {
        AppLanguage.CZECH -> "Dnešek"
        AppLanguage.ENGLISH -> "Today"
    }
    val navHistory: String = when (language) {
        AppLanguage.CZECH -> "Historie"
        AppLanguage.ENGLISH -> "History"
    }
    val navSettings: String = when (language) {
        AppLanguage.CZECH -> "Nastavení"
        AppLanguage.ENGLISH -> "Settings"
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
        AppLanguage.CZECH -> "Padla ti pomáhá vědomě uzavřít pracovní den a přepnout se z práce do osobního času. Nejvíc se hodí na home office, kde mezi kanceláří a domovem chybí přirozená hranice.\n\nStačí tři minuty denního rituálu: odpovíš na dnešní otázku a odložíš myšlenku, kterou už nemusíš nést, zklidníš dech v rytmu 4 s nádech / 4 s výdech, klepnutím zhasneš pracovní lampu a zaznamenáš, s jakou náladou den opouštíš.\n\nPráce se ti tak nepřelévá do večera, snáz vypneš hlavu a druhý den začínáš odpočatý. Všechno zůstává jen v telefonu — žádné účty, žádný cloud."
        AppLanguage.ENGLISH -> "ClockOut helps you consciously close your workday and switch from work into your own time. It matters most when you work from home, where the boundary between the office and the living room is missing.\n\nAll it takes is a three-minute daily ritual: answer today's question and park a thought you no longer need to carry, calm your breath at 4s in / 4s out, tap to turn off the desk lamp, and note the mood you are leaving the day with.\n\nWork stops spilling into your evening, you unwind more easily, and you start the next day rested. Everything stays on your phone — no accounts, no cloud."
    }
    val appVersionLabel: String = when (language) {
        AppLanguage.CZECH -> "Verze"
        AppLanguage.ENGLISH -> "Version"
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
