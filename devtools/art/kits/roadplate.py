"""Roadplate: a breastplate cut out of a road sign, bolted over a padded undersuit (nfx's §6; the grey
figure of the reference, §3).

Bolted sheet-metal plate over padding. A boxy welded helmet with a welding visor flipped up on its
brow, a twin-cartridge respirator and goggles under it. The breastplate is the sign: faded white with
a red border, "ROAD WORK" running off its edges, worn through to the cut metal. A bandolier right
shoulder to left hip, five cases clustered low on it; harness straps over both shoulders crossing the
plate. Mismatched shoulders -- two stacked plates on the left, a leather cap on the right -- and
mismatched bracers and knees. The material shows as the plates: cut steel on iron, teal composite on
diamond (D-0001).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, cloth, glass, leather, padding, painted, rubber, steel, webbing
from kitlib.vocab import ARM_INNER, ARM_OUT, BOOT_SIDE, SLEEVE, along, band, cases, pouch, shell

LOOKS = {
    "padding": padding("#8a8475", variation=0.05),
    "webbing": webbing("#3c3b33"),
    "leather": leather("#5b4431"),
    "strap": leather("#3e2e20"),
    "case": canvas("#55553f", variation=0.08),
    "pouch": canvas("#69654f", variation=0.07),
    "rubber": rubber("#2b2927"),
    "hose": rubber("#2f2c29"),
    "painted": painted("#4b4f3a"),
    "glass": glass(),
    "steel": steel(),
    "cloth": cloth("#85847d", variation=0.03),
}
PALETTE = "iron"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 1.0


def build() -> Kit:
    k = Kit("roadplate")

    # --- helmet -------------------------------------------------------------------------------
    shell(k, "helmet", "head", "padding.hood", 0.75, drop=("down",))
    k.add("helmet", "head", "plate.helm", (-5.25, 28.25, -5.25), (5.25, 33.4, 4.6), base=True, drop=("down",),
          paint={"rivets": "east", "stencil": {"text": "R7", "scale": 2, "face": "east", "at": (0.45, 0.55)}})
    k.add("helmet", "head", "plate.brow", (-5.5, 30.25, 4.6), (5.5, 31.75, 5.6), base=True)
    k.add("helmet", "head", "plate.patch", (5.25, 29.0, -2.5), (5.85, 31.5, 1.75), base=True, paint={"rivets": "east"})
    # The welding visor, hinged at the brow and flipped up over the crown.
    k.add("helmet", "head", "painted.visor", (-4.5, 31.5, 5.1), (4.5, 35.25, 5.85), rotation=(-62.0, 0.0, 0.0), origin=(0.0, 31.6, 5.3))
    k.add("helmet", "head", "glass.visor_lens", (-2.75, 32.6, 5.85), (2.75, 34.1, 6.1), rotation=(-62.0, 0.0, 0.0), origin=(0.0, 31.6, 5.3))
    # Goggles and the twin-cartridge respirator over the face: nobody shows a face (§3.5).
    k.add("helmet", "head", "rubber.goggle_frame", (-4.25, 28.0, 4.6), (4.25, 30.4, 5.05))
    k.add("helmet", "head", "glass.eye_r", (-3.6, 28.4, 5.05), (-0.9, 30.0, 5.4))
    k.add("helmet", "head", "glass.eye_l", (0.9, 28.4, 5.05), (3.6, 30.0, 5.4))
    k.add("helmet", "head", "rubber.mask", (-3.1, 24.4, 4.6), (3.1, 28.0, 6.0))
    k.add("helmet", "head", "rubber.valve", (-1.4, 24.9, 6.0), (1.4, 26.9, 6.6))
    k.add("helmet", "head", "painted.cartridge_r", (-4.6, 24.6, 4.9), (-2.6, 27.1, 6.7))
    k.add("helmet", "head", "painted.cartridge_l", (2.6, 24.6, 4.9), (4.6, 27.1, 6.7))
    band(k, "helmet", "head", "strap.mask", 1.0, 26.6, 27.4)

    # --- chestplate ----------------------------------------------------------------------------
    shell(k, "chestplate", "body", "padding.undersuit", 0.6, 12.5, 24.6)
    k.add("chestplate", "body", "padding.collar", (-4.3, 24.6, -2.95), (4.3, 25.6, 2.95))
    k.add("chestplate", "body", "plate.breast", (-4.3, 16.0, 2.6), (4.3, 23.6, 3.1), base=True,
          paint={"rivets": "south", "sign": {"face": "south", "field": "#d9d3c3", "border": "#8f3a2c", "border_width": 4, "text": "ROAD WORK",
                                             "scale": 3, "ink": "#24201c", "offset": (-14, -4), "wear": 0.12}})
    k.add("chestplate", "body", "plate.belly", (-3.6, 12.75, 2.6), (3.1, 15.85, 3.05), base=True,
          paint={"rivets": "south", "hazard": "south"})
    # Harness straps over both shoulders, crossing the breastplate's seams and standing off it.
    for side, x0, x1 in (("r", -3.8, -2.7), ("l", 2.7, 3.8)):
        k.add("chestplate", "body", f"webbing.harness_{side}", (x0, 13.0, 3.1), (x1, 24.6, 3.55))
        k.add("chestplate", "body", f"webbing.harness_top_{side}", (x0, 24.6, -3.05), (x1, 25.1, 3.55))
        k.add("chestplate", "body", f"webbing.harness_back_{side}", (x0, 15.0, -3.05), (x1, 24.6, -2.6))
    # The bandolier, right shoulder to left hip, its cases clustered low.
    start, end = (-4.2, 23.8), (3.9, 13.6)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.55, 4.05, 1.3)
    cases(k, "chestplate", "body", "case.cell", start, end, 4.05, 0.95, [0.52, 0.62, 0.70, 0.79, 0.88], (1.3, 1.8))
    # A canister clipped to the right harness, its hose up toward the mask.
    k.add("chestplate", "body", "painted.canister", (-4.4, 19.0, 3.55), (-2.4, 22.4, 5.0))
    k.add("chestplate", "body", "hose.lead", (-4.0, 22.4, 3.8), (-3.1, 25.1, 4.6))
    k.add("chestplate", "body", "hose.lead_top", (-4.0, 25.1, 3.1), (-3.1, 25.8, 4.6))
    pouch(k, "chestplate", "body", "pouch.belly", (1.0, 12.9, 3.05), (3.6, 15.2, 4.65))
    pouch(k, "chestplate", "body", "pouch.back", (-3.6, 14.2, -4.6), (0.6, 19.2, -2.6), face="north")
    # Shoulders: heavy left, a plate over a lame; light right, a leather cap.
    k.add("chestplate", "left_arm", "plate.pauldron", (3.5, 22.4, -3.2), (9.5, 24.75, 3.2), base=True, paint={"rivets": "up"})
    k.add("chestplate", "left_arm", "plate.pauldron_lame", (3.7, 20.5, -2.95), (9.2, 22.5, 2.95), base=True)
    k.add("chestplate", "right_arm", "leather.pauldron", (-9.0, 22.9, -3.0), (-3.6, 24.5, 3.0), paint={"stitch": True})
    shell(k, "chestplate", "right_arm", "padding.sleeve", SLEEVE, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "padding.sleeve", SLEEVE, 12.0, 24.4)
    # Bracers that don't match: a leather wrap right, a plate left; tape round the right sleeve.
    band(k, "chestplate", "right_arm", "leather.bracer", ARM_OUT, 12.2, 16.2, inner=ARM_INNER, paint={"stitch": True})
    band(k, "chestplate", "left_arm", "plate.bracer", ARM_OUT, 12.6, 15.7, inner=ARM_INNER, base=True)
    band(k, "chestplate", "right_arm", "cloth.tape", 0.8, 18.0, 18.8, inner=0.55)

    # --- leggings ------------------------------------------------------------------------------
    k.add("leggings", "body", "padding.waist", (-4.3, 11.0, -2.4), (4.3, 13.75, 2.4))
    k.add("leggings", "body", "leather.belt", (-4.55, 11.2, -2.65), (4.55, 12.4, 2.65))
    k.add("leggings", "body", "steel.buckle", (-1.0, 11.25, 2.65), (1.0, 12.35, 3.0))
    pouch(k, "leggings", "body", "pouch.belt", (-4.3, 9.2, 2.65), (-1.8, 12.0, 3.85))
    shell(k, "leggings", "right_leg", "padding.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "padding.trouser", 0.5, 1.0, 12.25)
    k.add("leggings", "left_leg", "plate.thigh", (0.6, 9.3, 2.5), (4.3, 11.9, 3.0), base=True, paint={"rivets": "south"})
    k.add("leggings", "right_leg", "plate.knee", (-3.65, 6.6, 2.5), (-0.15, 9.4, 3.0), base=True)
    k.add("leggings", "left_leg", "rubber.knee", (0.15, 6.8, 2.5), (3.65, 9.2, 2.95))
    band(k, "leggings", "right_leg", "webbing.thigh_strap", 0.65, 10.0, 10.8)
    pouch(k, "leggings", "right_leg", "pouch.thigh", (-4.75, 6.6, -1.5), (-4.4, 9.8, 1.5), face="west", flap=0.15)
    band(k, "leggings", "left_leg", "cloth.tape", 0.55, 9.4, 10.2)

    # --- boots ---------------------------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.75, -0.75, 5.75)
        band(k, "boots", part, f"leather.cuff_{side}", BOOT_SIDE, 4.75, 6.4)
        x0, x1 = (-4.65, 0.85) if side == "r" else (-0.85, 4.65)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -2.9), (x1 + 0.15, -0.3, 3.5))
    k.add("boots", "right_leg", "plate.toe", (-4.45, -0.75, 2.75), (0.65, 1.6, 3.45), base=True)
    k.add("boots", "left_leg", "rubber.toe", (-0.65, -0.75, 2.75), (4.45, 1.3, 3.3))
    k.add("boots", "right_leg", "plate.shin", (-4.2, 1.6, 2.75), (0.4, 4.75, 3.2), base=True)
    band(k, "boots", "right_leg", "webbing.shin_strap", 0.9, 2.8, 3.55)
    band(k, "boots", "left_leg", "webbing.boot_strap", 0.9, 2.0, 2.75)
    return k
