"""Blacklung: black lames and scrap, lit orange lenses, a full gas mask, hazard shoulders, an ammo
bandolier (nfx's §6; the dark figure of the reference, §3: near-black that still has readable form).

A dark hood pulled over a full gas mask, two orange lenses lit in it, a snout filter, its hose
hanging. A dark coat under overlapping lames, the upper lame over the lower as plates are hung,
each a touch out of true; the lames are the base material, dark alloy on netherite. Both shoulders
plated and hazard-striped, the left heavier by a second lame. Brass cartridges clustered on the
bandolier. Lames over the left forearm, a leather wrap on the right. A canister at the hip, its hose
up the chest.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, cloth, glow, hose, leather, painted, rubber, steel, webbing
from kitlib.paint import look
from kitlib.vocab import along, band, cases, cylinder, pouch, shell

LOOKS = {
    "cloth": cloth("#2c2926", variation=0.04),
    "rubber": rubber("#22201f"),
    "scrap": painted("#3a3531"),
    "lens_lit": glow("#f08a2a"),
    "casing": look("metal", "#7c6940", 0.70, 231, 0, rim=0.2, wear=0.6),
    "strap": leather("#33261b"),
    "webbing": webbing("#2e2c28"),
    "canister": painted("#3d3a2f"),
    "hose": hose("#262422"),
    "pouch": canvas("#38352c"),
    "steel": steel("#5d5f61"),
}
PALETTE = "netherite"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 0.85


def build() -> Kit:
    k = Kit("blacklung")

    # --- helmet: hood over a full gas mask ------------------------------------------------------------------
    shell(k, "helmet", "head", "cloth.hood", 0.75, drop=("down",))
    k.add("helmet", "head", "cloth.hood_peak", (-4.4, 31.6, 3.9), (4.4, 33.2, 5.6))
    k.add("helmet", "head", "cloth.hood_drape", (-4.6, 22.4, -5.2), (4.6, 25.0, -3.8))
    k.add("helmet", "head", "lames.crown", (-4.0, 32.6, -4.2), (4.0, 33.6, 3.0), base=True)
    k.add("helmet", "head", "rubber.mask", (-3.8, 24.2, 4.75), (3.8, 30.2, 5.6))
    for side, x0, x1 in (("r", -3.4, -0.8), ("l", 0.8, 3.4)):
        k.add("helmet", "head", f"scrap.rim_{side}", (x0 - 0.3, 26.9, 5.6), (x1 + 0.3, 29.9, 5.8))
        k.add("helmet", "head", f"lens_lit.eye_{side}", (x0, 27.2, 5.6), (x1, 29.6, 6.0))
    cylinder(k, "helmet", "head", "canister.snout", (0.0, 25.4, 6.6), 1.5, 2.0, "z")
    k.add("helmet", "head", "canister.snout_cap", (-1.0, 24.4, 7.6), (1.0, 26.4, 8.0))
    k.add("helmet", "head", "hose.mask", (0.6, 22.8, 6.0), (1.4, 24.6, 6.8))

    # --- chestplate: coat under lames, hazard shoulders, the ammo bandolier ---------------------------------
    shell(k, "chestplate", "body", "cloth.coat", 0.9, 12.5, 24.9,
          paint={"stencil": {"text": "13", "scale": 3, "face": "north", "at": (0.5, 0.4), "colour": "#bfb39a"}})
    for i in range(5):
        top = 24.0 - i * 2.05
        turn = (2.5, -1.5, 3.0, -2.5, 1.5)[i]
        k.add("chestplate", "body", f"lames.torso{i}", (-4.6 + 0.2 * (i % 2), top - 2.45, 2.9), (4.6 - 0.25 * (i % 3), top, 3.75 - i * 0.12),
              base=True, rotation=(0.0, 0.0, turn), origin=(0.0, top - 1.2, 3.3), paint={"rivets": "south", "pitch": 8})
    k.add("chestplate", "left_arm", "lames.pauldron", (3.2, 22.4, -3.9), (9.9, 26.4, 3.9), base=True, paint={"hazard": "south"})
    k.add("chestplate", "left_arm", "lames.pauldron_low", (3.6, 20.6, -3.6), (10.1, 22.6, 3.6), base=True)
    k.add("chestplate", "right_arm", "lames.pauldron", (-9.5, 23.2, -3.3), (-3.5, 25.6, 3.3), base=True, paint={"hazard": "south"})
    shell(k, "chestplate", "right_arm", "cloth.sleeve", 0.9, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "cloth.sleeve", 0.9, 12.0, 24.4)
    band(k, "chestplate", "left_arm", "lames.forearm", 1.2, 13.0, 15.0, base=True)
    band(k, "chestplate", "left_arm", "lames.forearm_high", 1.25, 15.3, 17.2, base=True)
    band(k, "chestplate", "right_arm", "strap.wrap", 1.15, 12.6, 16.0, paint={"stitch": True})
    start, end = (-4.6, 24.2), (4.4, 13.4)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.8, 4.4, 1.5)
    cases(k, "chestplate", "body", "casing.round", start, end, 4.4, 0.8, [0.27, 0.33, 0.40, 0.47, 0.54, 0.61], (0.7, 1.8))
    cylinder(k, "chestplate", "body", "canister.hip", (-3.6, 14.4, 4.4), 1.0, 3.0, "y")
    k.add("chestplate", "body", "hose.chest", (-3.0, 15.9, 3.9), (-2.2, 24.8, 4.7))
    pouch(k, "chestplate", "body", "pouch.mag", (0.6, 12.9, 3.6), (3.2, 15.5, 5.0))
    pouch(k, "chestplate", "body", "pouch.mag_small", (3.3, 13.1, 3.4), (4.8, 15.3, 4.6))

    # --- leggings -------------------------------------------------------------------------------------------
    k.add("leggings", "body", "cloth.waist", (-4.5, 11.0, -2.6), (4.5, 13.9, 2.6))
    k.add("leggings", "body", "strap.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "steel.buckle", (-1.0, 11.25, 2.85), (1.0, 12.35, 3.3))
    shell(k, "leggings", "right_leg", "cloth.leg", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "cloth.leg", 0.5, 1.0, 12.25)
    for i in range(3):
        top = 11.9 - i * 1.4
        k.add("leggings", "left_leg", f"lames.thigh{i}", (0.5, top - 1.7, 2.5), (4.5, top, 3.05 - i * 0.1), base=True)
    k.add("leggings", "right_leg", "scrap.thigh", (-4.4, 9.2, 2.5), (-0.5, 11.8, 2.95))
    k.add("leggings", "left_leg", "lames.knee", (0.2, 6.6, 2.5), (3.7, 9.0, 3.4), base=True)
    band(k, "leggings", "right_leg", "strap.knee", 0.65, 7.4, 8.2)
    pouch(k, "leggings", "right_leg", "pouch.leg", (-4.95, 9.4, -1.4), (-4.4, 11.9, 1.2), face="west")

    # --- boots ----------------------------------------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"rubber.boot_{side}", 0.95, -0.75, 5.8)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    k.add("boots", "left_leg", "lames.shin", (-0.55, 1.4, 2.95), (4.35, 5.2, 3.5), base=True)
    k.add("boots", "right_leg", "scrap.toe", (-4.55, -0.75, 2.95), (0.75, 1.5, 3.6))
    band(k, "boots", "right_leg", "webbing.strap", 1.0, 2.6, 3.3)
    band(k, "boots", "left_leg", "webbing.strap_top", 1.05, 5.2, 5.9)
    return k
