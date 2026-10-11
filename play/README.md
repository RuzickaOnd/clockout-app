# Play Store assets

Everything Google Play shows next to the app, plus the scripts that rebuild it.
All rendering goes through headless Chrome (`_render.py`), so Chrome and Pillow
are the only prerequisites.

| Asset | Files | Size |
| --- | --- | --- |
| Launcher / store icon | `ic_launcher-playstore.{svg,png}` | 512 × 512 |
| Feature graphic | `feature-graphic-{cs,en}.{html,png}` | 1024 × 500 |
| Phone screenshots | `screenshots/{cs,en}/01–08.png` | 1242 × 2208 (9:16) |
| Store description | `listing/description-{cs,en}.txt` | ≤ 4000 characters |
| Release notes | `release-notes/<version>.txt` | ≤ 500 characters per language |

```bash
python3 play/render-feature-graphic.py        # both languages, or pass cs / en
python3 play/render-screenshots.py
```

The feature graphic's copy lives in its HTML; the screenshot captions live in
the `SCREENS` table inside `render-screenshots.py`.

## Recapturing the raw screenshots

`screenshots/raw/<lang>/` holds untouched device captures. The renderer only
frames them, so anything wrong in a capture has to be fixed on the device:

1. Use an AVD built on a **`google_apis`** image, not `google_apis_playstore` —
   only the former grants `adb root`, which is needed to set the clock.
2. `adb root`, then `settings put global auto_time 0` and `date MMDDhhmmYYYY` to
   land on a **Thursday between 16:00 and 21:00**. Mid-week means the "This week"
   card has days to fill, and the time slot means the ritual can start.
3. Seed the demo history into `/data/data/org.clockout/databases/clockout.db`
   with `play/seed-demo-history.py` (both `ritual_logs` and `practice_days`, so the
   path card, the path screen and the night sky have something to show). Pull the
   database, apply the SQL with `sqlite3`, push it back while the app is stopped. The
   question and answer text is language-specific, so reseed when you switch languages.
4. In Settings, pick the language and turn on "Ritual available anytime".
5. Put the status bar in demo mode so it reads a clean 18:30:
   `settings put global sysui_demo_allowed 1`, then broadcast
   `com.android.systemui.demo` with `command enter`, `clock -e hhmm 1830`,
   `battery -e level 100`, `network -e mobile hide` and `notifications -e visible false`.
6. Capture with `adb exec-out screencap -p`, one shot per ritual step. On an AVD with
   several displays (the resizable one) `screencap` prepends a warning to its output;
   write to a file on the device with `screencap -d <id> -p /sdcard/cap.png` and pull it.

The 1.3.0 captures (`01-home`, `06-history`, `07-path`, `08-sky`) were taken on the
`Resizable_Experimental` AVD (a `google_apis` image) set to the Pixel 9's 1080 × 2424 with
`wm size 1080x2424` and `wm density 420`, so they match the older ritual-step captures.

Steps 1–3 only matter for the captures that show a date or a history: the home
screen and the week overview. A screen that shows neither — the breathing step,
say — needs nothing but demo mode, so it can be recaptured on any AVD and
without root. Put the device on a 24-hour clock first
(`settings put system time_12_24 24`): the demo clock renders in whatever format
the device is set to, and otherwise reads 6:30 where the other shots read 18:30.
