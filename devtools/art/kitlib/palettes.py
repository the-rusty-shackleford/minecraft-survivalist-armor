"""palettes: how a kit's material parts look on each armour material (D-0001: the base armour's own
material colours them, nfx's §1.2 and §4.2).

Each palette is a ramp of five colours from dark to light that a material part's grey key is
mapped through, its LabPBR response, and how it weathers. Each is the game's own armour material as
Pixlli draws it (its measured hue and value, devtools/art/pixlli-style.json) turned into a salvaged
part: duller, and under the saturation cap but for none (nfx's §7: "Progression is hue and value,
never saturation"). Gold becomes brass hardware that greens with verdigris; diamond a faded teal
composite scuffed toward grey; iron cut steel that rusts at its edges.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from .style import CLASSES, rgb, tinted


@dataclass(frozen=True)
class Palette:
    """A material's look: its ramp (5 x 3), smoothness (0-1), LabPBR F0 byte (0-229 dielectric, 230+ metals),
    porosity (0-64), the colour it weathers to and how much, and whether dust kills its metalness."""
    name: str
    ramp: np.ndarray
    smooth: float
    f0: int
    porosity: int
    weather: np.ndarray
    weathering: float


def _ramp(cls: str, saturation: float, dim: float, hue: float | None = None) -> np.ndarray:
    v = CLASSES[cls]["value"]
    h = CLASSES[cls]["hue"] if hue is None else hue
    return np.array([tinted(h, saturation, max(0.05, x * dim)) for x in v])


def palettes() -> dict[str, Palette]:
    """effects: returns every palette by name, as the mod's palette table names them."""
    return {
        # Cut steel: Pixlli's iron armour, a shade duller, cool, rust at the edges.
        "iron": Palette("iron", _ramp("armor_iron", 0.04, 0.74, 210.0), 0.62, 230, 0, rgb("#6b4a33"), 0.62),
        # Brass hardware, the least saturated gold in the lineup (nfx's §6), greening with verdigris.
        "gold": Palette("gold", _ramp("armor_gold", 0.34, 0.60), 0.58, 231, 0, rgb("#56705c"), 0.55),
        # A faded institutional teal composite, scuffed toward grey: not a metal.
        "diamond": Palette("diamond", _ramp("armor_diamond", 0.18, 0.80), 0.56, 14, 4, rgb("#8c938f"), 0.30),
        # Dark alloy with a warm cast.
        "netherite": Palette("netherite", _ramp("armor_netherite", 0.10, 1.0), 0.46, 255, 0, rgb("#3a2c26"), 0.20),
        # Dark wire steel, rusting.
        "chainmail": Palette("chainmail", _ramp("armor_chainmail", 0.05, 0.80, 215.0), 0.55, 230, 0, rgb("#6b4a33"), 0.6),
        # Hide, painted light and neutral so a dye tints it as the game tints leather.
        "leather": Palette("leather", _ramp("armor_leather", 0.02, 0.92), 0.26, 6, 42, rgb("#4a3f36"), 0.20),
        # Scute, dark sea green, salt-stained.
        "turtle": Palette("turtle", np.array([tinted(130, 0.28, x) for x in (0.10, 0.17, 0.26, 0.36, 0.48)]), 0.42, 10, 6,
                          rgb("#9a9a86"), 0.25),
        # nfx's galestone: wind-cut pale stone, blue-white and matte (the hue and value of its own textures, greyed to
        # the cap).
        "galestone": Palette("galestone", np.array([tinted(203, 0.10, x) for x in (0.16, 0.28, 0.42, 0.57, 0.72)]), 0.44, 18, 12,
                             rgb("#7a7466"), 0.30),
        # nfx's voidsteel: dark blue-violet steel, its albedo its own metal.
        "voidsteel": Palette("voidsteel", np.array([tinted(226, 0.26, x) for x in (0.04, 0.08, 0.13, 0.20, 0.30)]), 0.64, 255, 0,
                             rgb("#2e2a33"), 0.25),
        # Whatever armour no entry names: scrap, warm grey-brown, rusting.
        "neutral": Palette("neutral", np.array([tinted(28, 0.16, x) for x in (0.12, 0.21, 0.31, 0.42, 0.55)]), 0.40, 230, 0,
                           rgb("#6b4a33"), 0.6),
    }
