#!/usr/bin/env python3
"""Render the 1024x500 Play Store feature graphics from their HTML sources.

    python3 play/render-feature-graphic.py            # all languages
    python3 play/render-feature-graphic.py cs         # just one
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _render import shoot  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
WIDTH, HEIGHT = 1024, 500


def main(languages):
    if not languages:
        languages = sorted(
            name[len("feature-graphic-"):-len(".html")]
            for name in os.listdir(HERE)
            if name.startswith("feature-graphic-") and name.endswith(".html")
        )
    for language in languages:
        shoot(
            os.path.join(HERE, "feature-graphic-%s.html" % language),
            os.path.join(HERE, "feature-graphic-%s.png" % language),
            WIDTH, HEIGHT,
        )


if __name__ == "__main__":
    main(sys.argv[1:])
