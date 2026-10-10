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
from kitlib.vocab import along, band, cases, pouch, shell

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
    k.add("helmet", "head", "rubber.mask", (-3.1, 24.4, 4.6), (3.1, 28.0, 6.2))
    k.add("helmet", "head", "rubber.valve", (-1.4, 24.9, 6.2), (1.4, 26.9, 6.9))
    k.add("helmet", "head", "painted.cartridge_r", (-4.75, 24.6, 5.0), (-2.6, 27.1, 7.3))
    k.add("helmet", "head", "painted.cartridge_l", (2.6, 24.6, 5.0), (4.75, 27.1, 7.3))
    band(k, "helmet", "head", "strap.mask", 1.0, 26.6, 27.4)

    # --- chestplate ----------------------------------------------------------------------------
    shell(k, "chestplate", "body", "padding.undersuit", 0.9, 12.5, 24.9)
    k.add("chestplate", "body", "padding.collar", (-4.4, 24.9, -3.25), (4.4, 25.9, 3.25))
    k.add("chestplate", "body", "plate.breast", (-4.6, 16.0, 2.9), (4.6, 23.6, 3.65), base=True,
          paint={"rivets": "south", "sign": {"face": "south", "field": "#d9d3c3", "border": "#8f3a2c", "border_width": 4, "text": "ROAD WORK",
                                             "scale": 3, "ink": "#24201c", "offset": (-14, -4), "wear": 0.12}})
    k.add("chestplate", "body", "plate.belly", (-3.6, 12.75, 2.9), (3.1, 15.85, 3.45), base=True,
          paint={"rivets": "south", "hazard": "south"})
    # Harness straps over both shoulders, crossing the breastplate's seams and standing off it.
    for side, x0, x1 in (("r", -3.8, -2.7), ("l", 2.7, 3.8)):
        k.add("chestplate", "body", f"webbing.harness_{side}", (x0, 13.0, 3.65), (x1, 24.9, 4.2))
        k.add("chestplate", "body", f"webbing.harness_top_{side}", (x0, 24.9, -3.45), (x1, 25.45, 4.2))
        k.add("chestplate", "body", f"webbing.harness_back_{side}", (x0, 15.0, -3.45), (x1, 24.9, -2.9))
    # The bandolier, right shoulder to left hip, its cases clustered low.
    start, end = (-4.6, 24.0), (4.4, 13.2)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 4.2, 4.85, 1.4)
    cases(k, "chestplate", "body", "case.cell", start, end, 4.85, 1.35, [0.52, 0.62, 0.70, 0.79, 0.88], (1.35, 1.9))
    # A canister clipped to the right harness, its hose up toward the mask.
    k.add("chestplate", "body", "painted.canister", (-4.6, 19.0, 4.2), (-2.4, 22.6, 6.1))
    k.add("chestplate", "body", "hose.lead", (-4.15, 22.6, 4.6), (-3.2, 25.4, 5.5))
    k.add("chestplate", "body", "hose.lead_top", (-4.15, 25.4, 3.9), (-3.2, 26.1, 5.5))
    pouch(k, "chestplate", "body", "pouch.belly", (1.2, 12.9, 3.45), (4.1, 15.4, 5.05))
    pouch(k, "chestplate", "body", "pouch.back", (-3.6, 14.2, -4.75), (0.6, 19.2, -2.9), face="north")
    # Shoulders: heavy left, two stacked plates; light right, a leather cap.
    k.add("chestplate", "left_arm", "plate.pauldron", (3.3, 22.6, -3.7), (9.7, 26.3, 3.7), base=True, paint={"rivets": "up"})
    k.add("chestplate", "left_arm", "plate.pauldron_lame", (3.8, 20.9, -3.45), (10.0, 22.8, 3.45), base=True)
    k.add("chestplate", "right_arm", "leather.pauldron", (-9.45, 23.1, -3.3), (-3.55, 25.8, 3.3), paint={"stitch": True})
    shell(k, "chestplate", "right_arm", "padding.sleeve", 0.9, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "padding.sleeve", 0.9, 12.0, 24.4)
    # Bracers that don't match: a leather wrap right, a plate left; tape round the right sleeve.
    band(k, "chestplate", "right_arm", "leather.bracer", 1.15, 12.2, 16.2, paint={"stitch": True})
    band(k, "chestplate", "left_arm", "plate.bracer", 1.2, 12.6, 15.7, base=True)
    band(k, "chestplate", "right_arm", "cloth.tape", 1.0, 18.0, 18.8)

    # --- leggings ------------------------------------------------------------------------------
    k.add("leggings", "body", "padding.waist", (-4.5, 11.0, -2.6), (4.5, 13.75, 2.6))
    k.add("leggings", "body", "leather.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "steel.buckle", (-1.0, 11.25, 2.85), (1.0, 12.35, 3.3))
    pouch(k, "leggings", "body", "pouch.belt", (-4.4, 9.2, 2.85), (-1.8, 12.0, 4.25))
    shell(k, "leggings", "right_leg", "padding.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "padding.trouser", 0.5, 1.0, 12.25)
    k.add("leggings", "left_leg", "plate.thigh", (0.6, 9.3, 2.5), (4.6, 11.9, 3.1), base=True, paint={"rivets": "south"})
    k.add("leggings", "right_leg", "plate.knee", (-3.65, 6.6, 2.5), (-0.15, 9.4, 3.4), base=True)
    k.add("leggings", "left_leg", "rubber.knee", (0.15, 6.8, 2.5), (3.65, 9.2, 3.25))
    band(k, "leggings", "right_leg", "webbing.thigh_strap", 0.65, 10.0, 10.8)
    pouch(k, "leggings", "right_leg", "pouch.thigh", (-4.95, 6.6, -1.5), (-4.4, 9.8, 1.5), face="west")
    band(k, "leggings", "left_leg", "cloth.tape", 0.55, 9.4, 10.2)

    # --- boots ---------------------------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.95, -0.75, 5.75)
        band(k, "boots", part, f"leather.cuff_{side}", 1.25, 4.75, 6.4)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    k.add("boots", "right_leg", "plate.toe", (-4.55, -0.75, 2.95), (0.75, 1.6, 3.75), base=True)
    k.add("boots", "left_leg", "rubber.toe", (-0.75, -0.75, 2.95), (4.55, 1.3, 3.5))
    k.add("boots", "right_leg", "plate.shin", (-4.3, 1.6, 2.95), (0.5, 4.75, 3.45), base=True)
    band(k, "boots", "right_leg", "webbing.shin_strap", 1.0, 2.8, 3.55)
    band(k, "boots", "left_leg", "webbing.boot_strap", 1.0, 2.0, 2.75)
    return k
