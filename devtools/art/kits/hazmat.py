"""Hazmat: a sealed, quilted chemical suit, a boxy hood with a round side filter, a bedroll and a canister
on the back, coiled hoses (nfx's §6; the yellow figure of the reference, §3).

The suit is the one garment free of the saturation cap: a hazmat suit is hazard yellow, the same
exception §7 makes for hazard tape. The hood is a box, sealed at the neck, its window framed; the
filter stands off its left side. Padded shoulders, the left the bigger; forearms wrapped in strips of
cloth over the sleeves, rubber gloves at the hands, the right boot taped at the top. On the back, a
bedroll across the shoulders (its spiral ends, as the reference has them, sticking out past the right
shoulder), a canister under it on the right, and two coils of hose hung flat on the left; one of them
runs up over the left shoulder on the harness to the hood. The material shows as the hardware: the
window's frame, the filter, the canister, the buckle and a toe cap (D-0001).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, cloth, glass, hose, leather, quilt, rubber, steel, webbing
from kitlib.vocab import along, band, cases, cylinder, pouch, ring, shell

LOOKS = {
    "suit": quilt("#bf9f3a", variation=0.05, exempt=True),
    "rubber": rubber("#2a2826"),
    "tape": rubber("#77756e", rim=0.3),
    "wrap": cloth("#a99d82", variation=0.08),
    "glass": glass("#1b201f"),
    "strap": leather("#4a3626"),
    "webbing": webbing("#3d3c34"),
    "roll": canvas("#77705a", variation=0.06),
    "red": hose("#9c6158"),
    "blue": hose("#5a7090"),
    "pouch": canvas("#6a6450"),
    "case": canvas("#55553f", variation=0.08),
    "steel": steel(),
}
PALETTE = "iron"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 1.0


def build() -> Kit:
    k = Kit("hazmat")

    # --- helmet: a boxy hood, its window framed, the filter off its left side ------------------------------
    k.add("helmet", "head", "suit.hood", (-4.95, 23.2, -4.95), (4.95, 33.4, 4.95), drop=("down",),
          paint={"stencil": {"text": "04", "scale": 3, "face": "north", "at": (0.5, 0.45), "colour": "#2b2722"}})
    k.add("helmet", "head", "frame.window", (-4.3, 25.6, 4.95), (4.3, 31.4, 5.5), base=True)
    k.add("helmet", "head", "glass.window", (-3.7, 26.2, 5.5), (3.7, 30.8, 5.75))
    cylinder(k, "helmet", "head", "filter.side", (6.0, 27.4, 1.2), 1.8, 2.1, "x", base=True)
    cylinder(k, "helmet", "head", "rubber.filter_cap", (7.35, 27.4, 1.2), 1.15, 0.6, "x")
    cylinder(k, "helmet", "head", "steel.valve", (0.0, 24.5, 5.35), 1.0, 0.9, "z")
    k.add("helmet", "head", "rubber.neck_seal", (-4.7, 22.6, -4.7), (4.7, 23.4, 4.7))

    # --- chestplate: the suit, its wrapped forearms, the load on the back ----------------------------------
    shell(k, "chestplate", "body", "suit.torso", 1.0, 12.5, 25.0)
    shell(k, "chestplate", "right_arm", "suit.sleeve", 1.0, 12.4, 24.5)
    shell(k, "chestplate", "left_arm", "suit.sleeve", 1.0, 12.4, 24.5)
    k.add("chestplate", "left_arm", "suit.pad", (3.3, 21.4, -3.7), (9.8, 25.9, 3.7), paint={"stitch": True})
    k.add("chestplate", "right_arm", "suit.pad", (-9.4, 22.6, -3.3), (-3.8, 25.3, 3.3), paint={"stitch": True})
    for side, part in (("r", "right_arm"), ("l", "left_arm")):
        band(k, "chestplate", part, f"rubber.glove_{side}", 1.25, 11.6, 13.6)
        lean = 1.0 if side == "r" else -1.0
        band(k, "chestplate", part, f"wrap.low_{side}", 1.15, 13.3, 14.7, rotation=(5.0 * lean, 0.0, 0.0),
             origin=(-6.0 if side == "r" else 6.0, 14.0, 0.0))
        band(k, "chestplate", part, f"wrap.high_{side}", 1.12, 14.5, 16.0, rotation=(-4.0 * lean, 0.0, 0.0),
             origin=(-6.0 if side == "r" else 6.0, 15.2, 0.0))
    # The bedroll across the shoulders, past the right one; the canister under it; two coils hung flat on the left.
    roll = (-1.2, 21.2, -5.0)
    cylinder(k, "chestplate", "body", "roll.bedroll", roll, 1.6, 10.4, "x", paint={"spiral": {"axis": "x", "centre": roll}})
    k.add("chestplate", "body", "strap.roll", (-2.1, 19.4, -6.85), (-1.3, 23.0, -3.0))
    cylinder(k, "chestplate", "body", "tank.canister", (-2.0, 16.0, -4.9), 1.5, 5.0, "y", base=True, paint={"hazard": "north"})
    k.add("chestplate", "body", "steel.tank_valve", (-2.6, 18.5, -5.5), (-1.4, 19.3, -4.3))
    k.add("chestplate", "body", "strap.tank", (-3.8, 15.4, -6.65), (-0.2, 16.2, -3.0))
    ring(k, "chestplate", "body", "red.coil", (2.2, 16.2, -3.85), 2.0, 0.55, "z")
    ring(k, "chestplate", "body", "blue.coil", (2.6, 15.6, -4.4), 1.8, 0.5, "z")
    # The red hose up out of its coil, over the left shoulder on the harness, and into the hood.
    k.add("chestplate", "body", "red.hose_back", (2.6, 18.2, -4.2), (3.4, 25.0, -3.4))
    k.add("chestplate", "body", "red.hose_top", (2.6, 25.0, -4.2), (3.4, 25.8, 4.3))
    k.add("chestplate", "body", "red.hose_front", (2.6, 20.4, 3.5), (3.4, 25.0, 4.3))
    for side, x0, x1 in (("r", -3.9, -2.9), ("l", 2.4, 3.4)):
        k.add("chestplate", "body", f"webbing.harness_{side}", (x0, 13.2, 3.0), (x1, 25.0, 3.5))
    start, end = (-4.6, 24.2), (4.4, 13.0)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.5, 4.1, 1.3)
    cases(k, "chestplate", "body", "case.vial", start, end, 4.1, 1.1, [0.32, 0.40, 0.47, 0.78], (1.0, 1.9))
    pouch(k, "chestplate", "body", "pouch.dosimeter", (-0.1, 19.4, 3.0), (2.2, 21.8, 4.2))
    pouch(k, "chestplate", "body", "pouch.belly", (-2.7, 12.9, 3.0), (-0.2, 15.4, 4.4))

    # --- leggings -----------------------------------------------------------------------------------------
    k.add("leggings", "body", "suit.waist", (-4.5, 11.0, -2.6), (4.5, 13.9, 2.6))
    k.add("leggings", "body", "strap.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "frame.buckle", (-1.0, 11.25, 2.85), (1.0, 12.35, 3.3), base=True)
    shell(k, "leggings", "right_leg", "suit.leg", 0.55, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "suit.leg", 0.55, 1.0, 12.25)
    k.add("leggings", "right_leg", "suit.knee", (-3.6, 6.6, 2.55), (-0.2, 9.4, 3.3))
    k.add("leggings", "left_leg", "rubber.knee", (0.2, 6.8, 2.55), (3.6, 9.2, 3.15))
    band(k, "leggings", "left_leg", "strap.thigh", 0.7, 10.0, 10.8)
    pouch(k, "leggings", "left_leg", "pouch.leg", (4.45, 7.0, -1.3), (5.0, 10.0, 1.3), face="east")

    # --- boots: rubber, the right taped at the top --------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"rubber.boot_{side}", 0.95, -0.75, 6.0)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    band(k, "boots", "right_leg", "tape.ankle", 1.1, 5.0, 6.0)
    k.add("boots", "left_leg", "plate.toe", (-0.75, -0.75, 2.95), (4.55, 1.4, 3.6), base=True)
    k.add("boots", "right_leg", "rubber.toe", (-4.55, -0.75, 2.95), (0.75, 1.2, 3.45))
    band(k, "boots", "left_leg", "strap.boot", 1.05, 2.4, 3.1)
    return k
