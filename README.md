# Padla / ClockOut 🔔✨

> **Mindful Workday Shutdown & Transition Ritual for Android**  
> Aplikace pro vědomé uzavření pracovního dne a přepnutí do večerního volna na home office.

---

## 🇨🇿 O aplikaci (CZ)

**Padla** je minimalistická Android aplikace navržená pro lidi pracující na home office nebo hybridně, kteří mají problém „vypnout hlavu“ po skončení pracovní doby. 

Místo desítek meditačních nahrávek a rozhodovací paralýzy nabízí **jeden konkrétní 3minutový rituál**, který pomáhá udělat jasnou dělicí čáru mezi prací a osobním životem:

1. **Sumarizace dne (Parkoviště myšlenek)** – zapsání toho, co se dnes povedlo, a odložení nedořešených úkolů na zítra.
2. **Fyzické uzavření** – rituální zaklapnutí notebooku, uklizení pracovního stolu nebo vypnutí notifikací.
3. **Dechové zklidnění (4-7-8)** – vedené dechové cvičení s vizuálním pulzem a volitelnými jemnými zvukovými signály pro zklidnění nervové soustavy.
4. **Závěrečné odbití („Padla! / Clock out!“)** – oficiální přepnutí do volného času a zaznamenání denního úspěchu.

### Klíčové vlastnosti
* **Denní připomenutí (Notifikace):** Možnost nastavit čas konce pracovní doby s rychlými předvolbami a spolehlivým budením přes `AlarmManager`.
* **Sledování návyku (Streaks):** Přehled splněných rituálů, aktuální a nejdelší série bez nátlaku či stresu.
* **100% lokální & offline:** Veškerá data (myšlenky, historie) se ukládají výhradně lokálně v databázi Room na zařízení. Žádné účty, žádný cloud, absolutní soukromí.
* **Dvojjazyčnost:** Plná podpora češtiny (*Padla*) a angličtiny (*ClockOut*) s možností přepnutí přímo v nastavení nebo dle systému.
* **Moderní design:** Material Design 3, plynulé animace a tmavý uklidňující režim.

---

## 🇬🇧 About ClockOut (EN)

**ClockOut** is a lightweight Android application designed to help remote and hybrid professionals mindfully end their workday and transition into their personal time.

Instead of overwhelming audio catalogs, ClockOut offers a single guided **3-minute shutdown ritual**:
1. **Mental Dump** – Note down daily wins and park unfinished tasks for tomorrow.
2. **Physical Workspace Reset** – Close your laptop, clear your desk, and step away.
3. **Breathing Exercise (4-7-8)** – Calming animated breath pacer to reset your nervous system.
4. **Official Clock Out** – Mark the moment work ends and your evening begins.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose & Material 3
* **Architecture:** Clean MVVM (Model-View-ViewModel) + Kotlin Coroutines & Flow
* **Local Persistence:** Android Jetpack Room
* **Scheduling:** AlarmManager + BroadcastReceiver
* **Minimum SDK:** Android 8.0 (API 26) / Target SDK 34
