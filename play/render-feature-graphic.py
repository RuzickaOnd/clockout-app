#!/usr/bin/env python3
"""Render the Play Store feature graphics from their HTML sources.

Chrome draws each `feature-graphic-*.html` at 2x into an offscreen shot, then
Pillow downsamples it to the 1024x500 Play requires -- rendering at 1x leaves
the thin ring strokes and the letter-spaced eyebrow visibly aliased.

    python3 play/render-feature-graphic.py            # all languages
    python3 play/render-feature-graphic.py cs         # just one
"""
import os
import shutil
import subprocess
import sys
import tempfile
import time

from PIL import Image

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
HERE = os.path.dirname(os.path.abspath(__file__))
WIDTH, HEIGHT, SCALE = 1024, 500, 2


def render(html_path, png_path):
    shot = tempfile.mktemp(suffix=".png")
    profile = tempfile.mkdtemp()
    chrome = subprocess.Popen(
        [
            CHROME, "--headless", "--disable-gpu", "--no-sandbox", "--no-first-run",
            "--disable-extensions", "--hide-scrollbars",
            "--user-data-dir=" + profile,
            "--force-device-scale-factor=%d" % SCALE,
            "--virtual-time-budget=1500",
            "--window-size=%d,%d" % (WIDTH, HEIGHT),
            "--screenshot=" + shot,
            "file://" + os.path.abspath(html_path),
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    # Headless Chrome writes the screenshot but then lingers, so wait for the
    # file to stop growing rather than for the process to exit.
    deadline, previous = time.time() + 40, -1
    while time.time() < deadline:
        time.sleep(0.4)
        if os.path.exists(shot):
            current = os.path.getsize(shot)
            if current > 0 and current == previous:
                break
            previous = current
        if chrome.poll() is not None:
            break
    chrome.kill()
    chrome.wait()
    shutil.rmtree(profile, ignore_errors=True)

    if not os.path.exists(shot) or os.path.getsize(shot) == 0:
        raise SystemExit("Chrome produced no screenshot for %s" % html_path)

    # Play rejects alpha, so flatten to RGB on the way out.
    image = Image.open(shot).convert("RGB")
    if image.size != (WIDTH, HEIGHT):
        image = image.resize((WIDTH, HEIGHT), Image.LANCZOS)
    image.save(png_path, "PNG", optimize=True)
    os.remove(shot)
    print("%s  %dx%d" % (png_path, WIDTH, HEIGHT))


def main(languages):
    if not languages:
        languages = sorted(
            name[len("feature-graphic-"):-len(".html")]
            for name in os.listdir(HERE)
            if name.startswith("feature-graphic-") and name.endswith(".html")
        )
    for language in languages:
        render(
            os.path.join(HERE, "feature-graphic-%s.html" % language),
            os.path.join(HERE, "feature-graphic-%s.png" % language),
        )


if __name__ == "__main__":
    main(sys.argv[1:])
