# Play Store assets

Everything Google Play shows next to the app, plus the scripts that rebuild it.
All rendering goes through headless Chrome (`_render.py`), so Chrome and Pillow
are the only prerequisites.

| Asset | Files | Size |
| --- | --- | --- |
| Launcher / store icon | `ic_launcher-playstore.{svg,png}` | 512 × 512 |
| Feature graphic | `feature-graphic-{cs,en}.{html,png}` | 1024 × 500 |
| Phone screenshots | `screenshots/{cs,en}/01–06.png` | 1242 × 2208 (9:16) |

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
3. Seed a few closed days into `/data/data/org.clockout/databases/clockout.db`
   (table `ritual_logs`), otherwise the week card reads 0 of 5 and History is
   empty. The question and answer text is language-specific, so reseed when you
   switch languages.
4. In Settings, pick the language and turn on "Ritual available anytime".
5. Put the status bar in demo mode so it reads a clean 18:30:
   `settings put global sysui_demo_allowed 1`, then broadcast
   `com.android.systemui.demo` with `command enter`, `clock -e hhmm 1830`,
   `battery -e level 100`, `network -e mobile hide` and `notifications -e visible false`.
6. Capture with `adb exec-out screencap -p`, one shot per ritual step.
