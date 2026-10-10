"""style: Pixlli's measured numbers (devtools/art/pixlli-style.json), and colour helpers.

The one thing the painter knows of Pixlli is this file: scalars and palettes of at most eight
colours per material class (D-0002). It never opens the pack.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import colorsys
import json
from pathlib import Path

import numpy as np

STYLE_FILE = Path(__file__).resolve().parents[1] / "pixlli-style.json"
CLASSES: dict = json.loads(STYLE_FILE.read_text())["classes"]

# nfx's §7: nothing darker than this, nothing lighter.
FLOOR = np.array([0x14, 0x12, 0x0E]) / 255.0
CEILING = np.array([0xE2, 0xDA, 0xC8]) / 255.0
# The one colour constant across the kits (§5), over its near-black.
HAZARD = np.array([0xC6, 0xA4, 0x2E]) / 255.0
HAZARD_DARK = np.array([0x1E, 0x1B, 0x16]) / 255.0
# The dust of the reference: warm tan, paler where it settles thick.
DUST = np.array([0xA8, 0x94, 0x6C]) / 255.0
DUST_LIGHT = np.array([0xC4, 0xB2, 0x8C]) / 255.0
SATURATION_CAP = 0.35


def rgb(hexcode: str) -> np.ndarray:
    h = hexcode.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)]) / 255.0


def luma(c: np.ndarray) -> np.ndarray:
    return c[..., 0] * 0.2126 + c[..., 1] * 0.7152 + c[..., 2] * 0.0722


def environment_value() -> tuple[float, float]:
    """effects: returns the median and the 90% range of the world's own blocks' value: what the kits must sit among."""
    names = ("stone", "oak_planks", "cobblestone", "dirt", "coarse_dirt", "smooth_stone")
    meds = [CLASSES[n]["value"][2] for n in names if n in CLASSES]
    return float(np.mean(meds)), float(np.mean([CLASSES[n]["value"][4] - CLASSES[n]["value"][0] for n in names if n in CLASSES]))


def tinted(hue: float, saturation: float, value: float) -> np.ndarray:
    """effects: returns the colour of that hue (degrees) and HSV saturation whose luma is `value`."""
    lo, hi = 0.0, 1.0
    for _ in range(30):
        v = (lo + hi) / 2
        c = np.array(colorsys.hsv_to_rgb((hue % 360) / 360, saturation, v))
        if luma(c) < value:
            lo = v
        else:
            hi = v
    return np.array(colorsys.hsv_to_rgb((hue % 360) / 360, saturation, (lo + hi) / 2))


def cap_saturation(c: np.ndarray, cap: float = SATURATION_CAP) -> np.ndarray:
    """effects: returns colours c (..., 3) with HSV saturation no more than cap, value and hue kept."""
    mx = c.max(-1, keepdims=True)
    mn = c.min(-1, keepdims=True)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    over = sat > cap
    k = np.where(over, cap / np.maximum(sat, 1e-6), 1.0)
    return np.where(over, mx - (mx - c) * k, c)


def clamp_value(c: np.ndarray) -> np.ndarray:
    """effects: returns colours c held between the floor and the ceiling."""
    return np.clip(c, FLOOR, CEILING)
