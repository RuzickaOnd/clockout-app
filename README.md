# Padla / ClockOut 🔔✨

> **Mindful Workday Shutdown & Transition Ritual for Android**  
> Aplikace pro vědomé uzavření pracovního dne a přepnutí do večerního volna na home office.

**Ke stažení na Google Play / Available on Google Play:** <https://play.google.com/store/apps/details?id=org.clockout>

---

## 🇨🇿 O aplikaci (CZ)

**Padla** je minimalistická Android aplikace navržená pro lidi pracující na home office nebo hybridně, kteří mají problém „vypnout hlavu“ po skončení pracovní doby.

Nabízí **jeden konkrétní 3minutový rituál o čtyřech krocích**, který pomáhá udělat jasnou dělicí čáru mezi prací a osobním životem:

1. **Dnešní otázka (parkoviště myšlenek)** – každý den se střídá jiná otázka (co se povedlo, co necháváš za zavřenými dveřmi…). Vybereš si z nabízených odpovědí, nebo napíšeš vlastní myšlenku a necháš ji v aplikaci.
2. **Dechové zklidnění (4 s / 4 s)** – vedené dýchání s vizuálním pulzem, volitelným zvukovým podkresem a jemnou haptikou. Krok jde přeskočit.
3. **Vizuální uzavření** – klepnutím zhasneš na displeji pracovní lampu a symbolicky ukončíš den.
4. **Záznam nálady** – jedním klepnutím zaznamenáš, s jakým rozpoložením den opouštíš. Krok jde přeskočit.

Práce se ti tak nepřelévá do večera, snáz vypneš hlavu a druhý den začínáš odpočatý.

### Klíčové vlastnosti
* **Denní připomenutí (notifikace):** Nastavitelný čas konce pracovní doby s rychlými předvolbami; alarm plánuje `AlarmManager` (`setAndAllowWhileIdle`, tedy nepřesně, ale i v režimu Doze) a přežije restart zařízení.
* **Víkendy podle tebe:** Rituál i připomínka běží buď jen v pracovní dny, nebo všech 7 dní v týdnu.
* **Flexibilní pracovní doba:** Volitelně lze rituál spustit kdykoliv, ne jen v hlavním okně 16:00–21:00.
* **Přehled týdne:** Vidíš, které dny aktuálního týdne už máš uzavřené, včetně zaznamenané nálady, a v historii celkový počet uzavřených dní. Žádný nátlak, žádné trestání za vynechaný den.
* **Zvukový doprovod:** Čtyři předpřipravené styly (tibetská mísa, jemný déšť, dechové vlny, ticho s haptikou) plus **vlastní zvuky** – import MP3 / WAV / OGG / M4A / FLAC (max. 25 MB, až 10 souborů), které se při dýchání přehrávají ve smyčce.
* **Lokální data:** Myšlenky i historie se ukládají výhradně lokálně (Room + DataStore), nastavení a záznamy se nikam neodesílají. Žádné účty, žádná synchronizace do cloudu, žádné reklamy.
* **Dvojjazyčnost:** Plná podpora češtiny (*Padla*) a angličtiny (*ClockOut*) s přepnutím přímo v nastavení.
* **Moderní design:** Material Design 3, plynulé animace, tmavý uklidňující režim a displej, který během rituálu nezhasne.

---

## 🇬🇧 About ClockOut (EN)

**ClockOut** is a lightweight Android application designed to help remote and hybrid professionals mindfully end their workday and transition into their personal time.

It guides you through a single **3-minute, four-step shutdown ritual** that draws a clear line between work and personal life:

1. **Today's question (thought parking)** – A different reflective question each day. Pick one of the suggested answers or write your own thought and leave it in the app.
2. **Breathing (4s in / 4s out)** – A guided breath pacer with a visual pulse, optional ambient sound and gentle haptics. Skippable.
3. **Visual closure** – Tap to turn off the desk lamp on screen and symbolically end the day.
4. **Mood log** – One tap to capture the mood you are leaving the day with. Skippable.

Work stops spilling into your evening, you unwind more easily, and you start the next day rested.

### Key features
* **Daily reminder** at your chosen workday end time, with quick presets; scheduled via `AlarmManager` (`setAndAllowWhileIdle` — inexact but Doze-tolerant) and restored after reboot.
* **Weekdays or all seven days** — your choice, for both the ritual and its reminder.
* **Flexible schedule** — optionally allow the ritual at any hour, not just the 16:00–21:00 window.
* **Weekly overview** — which days of the current week you have already closed, with the mood you logged, plus the total number of closed days in History. No pressure, no penalty for a missed day.
* **Sound** — four built-in ambient styles (Tibetan singing bowl, gentle rain, breathing waves, silence with haptics) plus **your own sounds**: import MP3 / WAV / OGG / M4A / FLAC (max 25 MB, up to 10 files) and loop them while you breathe.
* **Local data** — thoughts and history live only on the device (Room + DataStore). No accounts, no cloud sync, no ads; nothing you write is uploaded.
* **Bilingual** — full English and Czech, switchable in Settings.
* **Modern design** — Material 3, smooth animations, a calm dark theme, and a screen that stays awake for the whole ritual.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose & Material 3
* **Architecture:** Clean MVVM (Model-View-ViewModel) + Kotlin Coroutines & Flow
* **Local Persistence:** Jetpack Room (ritual history) + DataStore Preferences (settings)
* **Scheduling:** AlarmManager + BroadcastReceiver
* **Distribution:** Play In-App Updates (flexible)
* **SDK:** Minimum Android 7.0 (API 24), target/compile SDK 37
