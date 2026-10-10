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
from kitlib.vocab import ARM_INNER, ARM_OUT, BOOT_SIDE, SLEEVE, along, band, cases, cylinder, pouch, ring, shell

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
    cylinder(k, "helmet", "head", "filter.side", (5.6, 27.4, 1.2), 1.8, 1.3, "x", base=True)
    cylinder(k, "helmet", "head", "rubber.filter_cap", (6.375, 27.4, 1.2), 1.15, 0.25, "x")
    cylinder(k, "helmet", "head", "steel.valve", (0.0, 24.5, 5.35), 1.0, 0.9, "z")
    k.add("helmet", "head", "rubber.neck_seal", (-4.7, 22.6, -4.7), (4.7, 23.4, 4.7))

    # --- chestplate: the suit, its wrapped forearms, the load on the back ----------------------------------
    shell(k, "chestplate", "body", "suit.torso", 0.6, 12.5, 24.6)
    shell(k, "chestplate", "right_arm", "suit.sleeve", SLEEVE, 12.4, 24.4)
    shell(k, "chestplate", "left_arm", "suit.sleeve", SLEEVE, 12.4, 24.4)
    k.add("chestplate", "left_arm", "suit.pad", (3.5, 21.4, -3.2), (9.5, 24.75, 3.2), paint={"stitch": True})
    k.add("chestplate", "right_arm", "suit.pad", (-9.0, 22.6, -3.0), (-3.8, 24.5, 3.0), paint={"stitch": True})
    for side, part in (("r", "right_arm"), ("l", "left_arm")):
        band(k, "chestplate", part, f"rubber.glove_{side}", ARM_OUT, 11.6, 13.6, inner=ARM_INNER)
        lean = 1.0 if side == "r" else -1.0
        band(k, "chestplate", part, f"wrap.low_{side}", 0.85, 13.3, 14.7, inner=0.55, rotation=(5.0 * lean, 0.0, 0.0),
             origin=(-6.0 if side == "r" else 6.0, 14.0, 0.0))
        band(k, "chestplate", part, f"wrap.high_{side}", 0.8, 14.5, 16.0, inner=0.55, rotation=(-4.0 * lean, 0.0, 0.0),
             origin=(-6.0 if side == "r" else 6.0, 15.2, 0.0))
    # The bedroll across the shoulder blades; the canister under it; two coils hung flat on the left.
    roll = (-0.1, 22.2, -3.8)
    cylinder(k, "chestplate", "body", "roll.bedroll", roll, 1.15, 9.4, "x", paint={"spiral": {"axis": "x", "centre": roll}})
    k.add("chestplate", "body", "strap.roll", (-2.1, 20.8, -5.0), (-1.3, 23.6, -2.6))
    cylinder(k, "chestplate", "body", "tank.canister", (-2.0, 16.6, -3.75), 1.1, 5.0, "y", base=True, paint={"hazard": "north"})
    k.add("chestplate", "body", "steel.tank_valve", (-2.6, 19.1, -4.35), (-1.4, 19.8, -3.15))
    k.add("chestplate", "body", "strap.tank", (-3.4, 15.4, -5.0), (-0.6, 16.2, -2.6))
    ring(k, "chestplate", "body", "red.coil", (2.2, 16.2, -2.9), 2.0, 0.55, "z")
    ring(k, "chestplate", "body", "blue.coil", (2.6, 15.6, -3.45), 1.8, 0.5, "z")
    # The red hose up out of its coil, over the left shoulder on the harness, and into the hood.
    k.add("chestplate", "body", "red.hose_back", (2.6, 18.2, -3.4), (3.4, 24.6, -2.6))
    k.add("chestplate", "body", "red.hose_top", (2.6, 24.6, -3.4), (3.4, 25.4, 3.85))
    k.add("chestplate", "body", "red.hose_front", (2.6, 20.4, 3.05), (3.4, 24.6, 3.85))
    for side, x0, x1 in (("r", -3.9, -2.9), ("l", 2.4, 3.4)):
        k.add("chestplate", "body", f"webbing.harness_{side}", (x0, 13.2, 2.6), (x1, 24.6, 3.05))
    start, end = (-4.2, 23.8), (3.9, 13.6)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.05, 3.55, 1.2)
    cases(k, "chestplate", "body", "case.vial", start, end, 3.55, 1.1, [0.32, 0.40, 0.47, 0.78], (1.0, 1.9))
    pouch(k, "chestplate", "body", "pouch.dosimeter", (-0.1, 19.4, 2.6), (2.2, 21.8, 3.7))
    pouch(k, "chestplate", "body", "pouch.belly", (-2.7, 12.9, 2.6), (-0.2, 15.4, 3.9))

    # --- leggings -----------------------------------------------------------------------------------------
    k.add("leggings", "body", "suit.waist", (-4.3, 11.0, -2.4), (4.3, 13.9, 2.4))
    k.add("leggings", "body", "strap.belt", (-4.55, 11.2, -2.65), (4.55, 12.4, 2.65))
    k.add("leggings", "body", "frame.buckle", (-1.0, 11.25, 2.65), (1.0, 12.35, 3.0), base=True)
    shell(k, "leggings", "right_leg", "suit.leg", 0.55, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "suit.leg", 0.55, 1.0, 12.25)
    k.add("leggings", "right_leg", "suit.knee", (-3.6, 6.6, 2.55), (-0.2, 9.4, 3.0))
    k.add("leggings", "left_leg", "rubber.knee", (0.2, 6.8, 2.55), (3.6, 9.2, 2.95))
    band(k, "leggings", "left_leg", "strap.thigh", 0.7, 10.0, 10.8)
    pouch(k, "leggings", "left_leg", "pouch.leg", (4.45, 7.0, -1.3), (4.75, 10.0, 1.3), face="east", flap=0.15)

    # --- boots: rubber, the right taped at the top --------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"rubber.boot_{side}", 0.75, -0.75, 6.0)
        x0, x1 = (-4.65, 0.85) if side == "r" else (-0.85, 4.65)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -2.9), (x1 + 0.15, -0.3, 3.5))
    band(k, "boots", "right_leg", "tape.ankle", BOOT_SIDE, 5.0, 6.0)
    k.add("boots", "left_leg", "plate.toe", (-0.65, -0.75, 2.75), (4.45, 1.4, 3.4), base=True)
    k.add("boots", "right_leg", "rubber.toe", (-4.45, -0.75, 2.75), (0.65, 1.2, 3.25))
    band(k, "boots", "left_leg", "strap.boot", 0.9, 2.4, 3.1)
    return k
