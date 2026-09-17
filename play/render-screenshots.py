#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Compose the Play Store phone screenshots.

Each listing image is a raw device capture from `screenshots/raw/<lang>/` sitting
in a phone frame on the same night sky as the feature graphic, with a caption
that names the ritual step. Output is 1242x2208 (9:16), which Play accepts.

Recapture the raw shots on an emulator whose date falls mid-week and whose
history already holds a few closed days, otherwise the week card reads 0 of 5.

    python3 play/render-screenshots.py            # all languages
    python3 play/render-screenshots.py cs         # just one
"""
import os
import random
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _render import shoot  # noqa: E402

HERE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
WIDTH, HEIGHT = 1242, 2208

# raw capture -> caption above the phone
SCREENS = {
    "cs": [
        ("01-home.png",      "DENNÍ RITUÁL",    "Uzavři den vědomě",     "Jeden rituál, čtyři krátké kroky."),
        ("02-question.png",  "KROK 1 · OTÁZKA", "Zaparkuj myšlenky",     "Nech práci za zavřenými dveřmi."),
        ("03-breathing.png", "KROK 2 · DECH",   "Zklidni dech 4–4",      "Čtyři animace na výběr, zvuk i jemná haptika."),
        ("04-lamp.png",      "KROK 3 · LAMPA",  "Zhasni pracovní lampu", "Symbolická tečka za dnešním dnem."),
        ("05-mood.png",      "KROK 4 · NÁLADA", "Zaznamenej náladu",     "Jedno klepnutí, žádné vyplňování."),
        ("06-history.png",   "PŘEHLED",         "Sleduj svůj týden",     "Bez nátlaku a bez trestů za vynechaný den."),
    ],
    "en": [
        ("01-home.png",      "DAILY RITUAL",      "Close the day on purpose", "One ritual, four short steps."),
        ("02-question.png",  "STEP 1 · QUESTION", "Park your thoughts",       "Leave work behind closed doors."),
        ("03-breathing.png", "STEP 2 · BREATH",   "Breathe 4 in, 4 out",      "Four animations to choose from, sound and haptics."),
        ("04-lamp.png",      "STEP 3 · LAMP",     "Turn off the desk lamp",   "A symbolic full stop to the day."),
        ("05-mood.png",      "STEP 4 · MOOD",     "Log how you feel",         "One tap. Nothing to fill in."),
        ("06-history.png",   "OVERVIEW",          "See your week",            "No pressure, no penalty for a missed day."),
    ],
}

# A fixed seed keeps the star field identical between runs, so re-rendering an
# untouched screenshot produces the same bytes.
random.seed(11)
STARS = "".join(
    '<circle cx="%.1f" cy="%.1f" r="%s" fill="#F8FAFC" opacity="%.2f"/>'
    % (random.uniform(20, WIDTH - 20), random.uniform(15, 620),
       random.choice([1.0, 1.3, 1.6, 2.0]), random.uniform(.18, .7))
    for _ in range(40)
)

SCREEN_WIDTH, BEZEL, PHONE_TOP = 800, 15, 372

CSS = """
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:%(W)dpx;height:%(H)dpx;overflow:hidden;background:#0A0B14}
.canvas{position:relative;width:%(W)dpx;height:%(H)dpx;overflow:hidden;
  background:linear-gradient(160deg,#090A13 0%%,#141329 38%%,#241A3C 70%%,#33214A 100%%);
  font-family:"SF Pro Display",-apple-system,"Helvetica Neue",Arial,sans-serif;
  -webkit-font-smoothing:antialiased;text-align:center}
.glow{position:absolute;border-radius:50%%;pointer-events:none}
.g-purple{width:1000px;height:1000px;right:-280px;top:-460px;
  background:radial-gradient(circle,rgba(147,51,234,.36) 0%%,rgba(147,51,234,0) 68%%)}
.g-indigo{width:900px;height:900px;left:-260px;top:-180px;
  background:radial-gradient(circle,rgba(99,102,241,.30) 0%%,rgba(99,102,241,0) 66%%)}
.g-amber{position:absolute;width:1500px;height:620px;left:50%%;transform:translateX(-50%%);
  bottom:-330px;border-radius:50%%;
  background:radial-gradient(ellipse,rgba(255,203,107,.26) 0%%,rgba(245,158,11,.10) 42%%,rgba(245,158,11,0) 70%%)}
.stars{position:absolute;inset:0}

.head{position:absolute;left:0;right:0;top:104px;padding:0 90px}
.eyebrow{font-size:25px;font-weight:600;letter-spacing:.24em;color:#B4C0FF}
h1{margin-top:26px;font-size:66px;line-height:1.08;font-weight:600;
  letter-spacing:-.022em;color:#F8FAFC}
.sub{margin-top:22px;font-size:31px;line-height:1.4;font-weight:400;color:#A3AEC4}

.phone{position:absolute;left:50%%;transform:translateX(-50%%);top:%(TOP)dpx;
  width:%(OUTER)dpx;padding:%(BEZEL)dpx;border-radius:66px;
  background:linear-gradient(150deg,#33344F 0%%,#1A1B2C 42%%,#2A2B44 100%%);
  box-shadow:0 44px 90px rgba(5,6,14,.62), 0 0 0 1px rgba(255,255,255,.20),
             0 0 130px rgba(99,102,241,.22)}
.phone img{display:block;width:100%%;border-radius:52px;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.07)}
"""

TEMPLATE = u"""<meta charset="utf-8"><style>%(css)s</style>
<div class="canvas">
  <div class="glow g-purple"></div>
  <div class="glow g-indigo"></div>
  <svg class="stars" viewBox="0 0 %(W)d %(H)d" xmlns="http://www.w3.org/2000/svg">%(stars)s</svg>
  <div class="g-amber"></div>
  <div class="head">
    <div class="eyebrow">%(eyebrow)s</div>
    <h1>%(headline)s</h1>
    <div class="sub">%(sub)s</div>
  </div>
  <div class="phone"><img src="%(img)s"></div>
</div>
"""


def main(languages):
    css = CSS % dict(W=WIDTH, H=HEIGHT, TOP=PHONE_TOP,
                     OUTER=SCREEN_WIDTH + 2 * BEZEL, BEZEL=BEZEL)
    build = tempfile.mkdtemp(prefix="clockout-shots-")
    for language in languages or sorted(SCREENS):
        out_dir = os.path.join(HERE, language)
        os.makedirs(out_dir, exist_ok=True)
        for index, (raw, eyebrow, headline, sub) in enumerate(SCREENS[language], start=1):
            page = TEMPLATE % dict(
                css=css, W=WIDTH, H=HEIGHT, stars=STARS, eyebrow=eyebrow,
                headline=headline, sub=sub,
                img=os.path.join(HERE, "raw", language, raw),
            )
            html = os.path.join(build, "%s-%02d.html" % (language, index))
            open(html, "w", encoding="utf-8").write(page)
            shoot(html, os.path.join(out_dir, "%02d.png" % index),
                  WIDTH, HEIGHT, scale=1.5)


if __name__ == "__main__":
    main(sys.argv[1:])
