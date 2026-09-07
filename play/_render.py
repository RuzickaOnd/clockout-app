"""Shared headless-Chrome renderer for the Play Store assets.

Chrome draws the page at a higher device scale factor and Pillow downsamples the
result, which is what keeps hairline strokes and letter-spaced captions from
aliasing at the sizes Play asks for.
"""
import os
import shutil
import subprocess
import tempfile
import time

from PIL import Image

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


def shoot(html_path, png_path, width, height, scale=2):
    """Render an HTML file to an exactly width x height RGB PNG."""
    shot = tempfile.mktemp(suffix=".png")
    profile = tempfile.mkdtemp()
    chrome = subprocess.Popen(
        [
            CHROME, "--headless", "--disable-gpu", "--no-sandbox", "--no-first-run",
            "--disable-extensions", "--hide-scrollbars", "--allow-file-access-from-files",
            "--user-data-dir=" + profile,
            "--force-device-scale-factor=%s" % scale,
            "--virtual-time-budget=2500",
            "--window-size=%d,%d" % (width, height),
            "--screenshot=" + shot,
            "file://" + os.path.abspath(html_path),
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    # Headless Chrome writes the screenshot but then lingers, so wait for the
    # file to stop growing rather than for the process to exit.
    deadline, previous = time.time() + 60, -1
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
    if image.size != (width, height):
        image = image.resize((width, height), Image.LANCZOS)
    image.save(png_path, "PNG", optimize=True)
    os.remove(shot)
    print("%s  %dx%d" % (png_path, width, height))
