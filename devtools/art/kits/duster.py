"""Duster: a canvas work coat over cut hide, a big collar, a face rag and welding goggles (nfx's §6; the
brown figure of the reference, §3). The crudest kit: its lenses are dead flat, its respirator a rag.

A deep hood whose drape lies over the shoulders: that drape is the big collar. It belongs to the hood,
not the coat, so it turns with the head; a collar on the coat would be cut by the hood's corners
every time the wearer looked round. Under the hood a rag tied over the face, its ends hanging, and
welding goggles in a leather band. The coat hangs open over a hide vest, lapels turned back; a pack
on the back, its straps joined across the chest, a shotgun-shell bandolier over them. Heavy left: a
hide mantle with a cut sheet riveted on top, and a sheet bracer; the right arm only rag-wrapped. The
coat's skirts hang from the hips to the knee on the leggings. Tall boots, a spur on the right heel.
The material shows as the cut sheet: the pauldron, the bracer, a knee, a toe cap and the buckle (D-0001).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, cloth, glass, hazard, hose, leather, painted, rubber, steel, webbing
from kitlib.vocab import ARM_INNER, ARM_OUT, BOOT_SIDE, SLEEVE, along, band, cases, cylinder, pouch, shell

LOOKS = {
    "coat": canvas("#4e3f30", variation=0.06),
    "hide": leather("#856a4e", variation=0.07),
    "rag": cloth("#9b8e76", variation=0.06),
    "leather": leather("#3d3026"),
    "strap": leather("#2f241b"),
    "hazard": hazard(),
    "webbing": webbing("#3d3a31"),
    "glass": glass("#18221c"),
    "painted": painted("#4a4a3c"),
    "hose": hose("#2d2a27"),
    "cartridge": painted("#6e4a3a", variation=0.06),
    "pouch": canvas("#5e5442"),
    "canvas": canvas("#5a5240", variation=0.07),
    "rubber": rubber("#2a2724"),
    "steel": steel(),
}
PALETTE = "leather"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 1.15


def build() -> Kit:
    k = Kit("duster")

    # --- helmet: a deep hood and its drape, a face rag, welding goggles -----------------------------------
    k.add("helmet", "head", "coat.hood", (-4.85, 23.6, -5.0), (4.85, 33.0, 4.6), drop=("down",),
          paint={"stencil": {"text": "9", "scale": 3, "face": "north", "at": (0.5, 0.25), "colour": "#cbbfa4"}})
    # The hood's opening stands out round the face, so the face sits back in its shadow (the reference's brown figure).
    k.add("helmet", "head", "coat.hood_brow", (-4.85, 30.6, 4.6), (4.85, 33.0, 6.3))
    k.add("helmet", "head", "coat.hood_cheek_r", (-4.85, 23.6, 4.6), (-3.75, 30.6, 6.3))
    k.add("helmet", "head", "coat.hood_cheek_l", (3.75, 23.6, 4.6), (4.85, 30.6, 6.3))
    k.add("helmet", "head", "coat.drape", (-5.5, 22.2, -5.5), (5.5, 24.6, 4.4), drop=("down",))
    k.add("helmet", "head", "rag.face", (-3.75, 23.4, 4.6), (3.75, 27.3, 5.35))
    k.add("helmet", "head", "rag.tail", (-1.8, 21.6, 4.85), (1.6, 23.4, 5.3), rotation=(0.0, 0.0, 6.0), origin=(0.0, 23.4, 5.1))
    k.add("helmet", "head", "strap.goggle_band", (-5.05, 28.2, -5.15), (5.05, 29.2, 4.75))
    for side, cx in (("r", -2.0), ("l", 2.0)):
        cylinder(k, "helmet", "head", f"steel.goggle_cup_{side}", (cx, 28.7, 5.25), 1.45, 1.3, "z")
        k.add("helmet", "head", f"glass.goggle_lens_{side}", (cx - 0.95, 27.75, 5.9), (cx + 0.95, 29.65, 6.1))
    k.add("helmet", "head", "steel.goggle_bridge", (-0.6, 28.4, 5.0), (0.6, 29.0, 5.5))

    # --- chestplate: the coat open over the vest, the pack, the bandolier ---------------------------------
    shell(k, "chestplate", "body", "hide.vest", 0.6, 12.5, 24.6)
    k.add("chestplate", "body", "coat.back", (-4.8, 12.5, -3.0), (4.8, 24.9, -2.6))
    k.add("chestplate", "body", "coat.front_r", (-4.8, 12.5, 2.6), (-1.3, 24.9, 3.0))
    k.add("chestplate", "body", "coat.front_l", (1.3, 12.5, 2.6), (4.8, 24.9, 3.0))
    k.add("chestplate", "body", "coat.lapel_r", (-3.7, 20.4, 3.0), (-1.1, 24.9, 3.35))
    k.add("chestplate", "body", "coat.lapel_l", (1.1, 20.4, 3.0), (3.7, 24.9, 3.35))
    shell(k, "chestplate", "right_arm", "coat.sleeve", SLEEVE, 12.6, 24.4)
    shell(k, "chestplate", "left_arm", "coat.sleeve", SLEEVE, 12.6, 24.4)
    k.add("chestplate", "left_arm", "hide.mantle", (3.5, 21.6, -3.2), (9.5, 24.5, 3.2), paint={"stitch": True})
    k.add("chestplate", "left_arm", "sheet.pauldron", (3.6, 24.5, -2.95), (9.3, 24.75, 2.95), base=True, paint={"rivets": "up"})
    band(k, "chestplate", "left_arm", "sheet.bracer", ARM_OUT, 12.8, 15.8, inner=ARM_INNER, base=True, paint={"rivets": "east"})
    band(k, "chestplate", "right_arm", "leather.cuff", ARM_OUT, 12.2, 14.6, inner=ARM_INNER, paint={"stitch": True})
    band(k, "chestplate", "right_arm", "rag.wrap", 0.8, 15.0, 15.7, inner=0.55)
    # Rag strips wound round the coat, over it and under the straps.
    k.add("chestplate", "body", "rag.strip_high", (-4.9, 16.4, -3.2), (4.9, 17.2, 3.2))
    k.add("chestplate", "body", "rag.strip_low", (-4.9, 13.6, -3.2), (4.9, 14.3, 3.2))
    # The pack and its straps, joined across the open coat.
    for side, x0, x1 in (("r", -3.9, -2.9), ("l", 2.9, 3.9)):
        k.add("chestplate", "body", f"strap.pack_{side}", (x0, 16.0, 3.35), (x1, 24.9, 3.75))
        k.add("chestplate", "body", f"strap.pack_top_{side}", (x0, 24.9, -3.6), (x1, 25.4, 3.75))
        k.add("chestplate", "body", f"strap.pack_back_{side}", (x0, 15.0, -3.6), (x1, 24.9, -3.2))
    k.add("chestplate", "body", "webbing.sternum", (-3.9, 19.2, 3.4), (3.9, 19.9, 3.85))
    pouch(k, "chestplate", "body", "canvas.pack", (-3.4, 14.4, -4.75), (3.4, 21.6, -3.6), face="north", flap=0.25)
    k.add("chestplate", "body", "hazard.tape", (-3.45, 16.8, -4.95), (3.45, 18.0, -4.75))
    # A filter canister on the left strap, its hose up under the rag.
    cylinder(k, "chestplate", "body", "painted.canister", (3.4, 21.6, 4.35), 0.65, 2.6, "y")
    k.add("chestplate", "body", "hose.lead", (2.9, 22.9, 3.75), (3.7, 25.2, 4.55))
    start, end = (-4.2, 23.8), (3.9, 13.6)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.85, 4.3, 1.3)
    cases(k, "chestplate", "body", "cartridge.shell", start, end, 4.3, 0.7, [0.30, 0.36, 0.42, 0.49, 0.56, 0.62], (0.7, 1.7))
    pouch(k, "chestplate", "body", "pouch.hip", (-4.6, 12.7, 3.0), (-2.3, 15.2, 4.2))
    pouch(k, "chestplate", "body", "pouch.back", (-3.2, 12.6, -4.6), (0.2, 14.3, -3.0), face="north")

    # --- leggings: hide trousers under the coat's skirts --------------------------------------------------
    k.add("leggings", "body", "hide.waist", (-4.3, 11.0, -2.4), (4.3, 13.9, 2.4))
    k.add("leggings", "body", "strap.belt", (-4.55, 11.2, -2.65), (4.55, 12.4, 2.65))
    k.add("leggings", "body", "sheet.buckle", (-1.1, 11.2, 2.65), (1.1, 12.4, 3.0), base=True)
    shell(k, "leggings", "right_leg", "hide.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "hide.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "right_leg", "coat.skirt", 0.65, 6.8, 12.25)
    shell(k, "leggings", "left_leg", "coat.skirt", 0.65, 6.8, 12.25)
    k.add("leggings", "right_leg", "sheet.knee", (-3.7, 6.6, 2.65), (-0.1, 9.2, 3.0), base=True, paint={"rivets": "south"})
    k.add("leggings", "left_leg", "hide.knee", (0.2, 6.8, 2.65), (3.6, 9.0, 2.95), paint={"stitch": True})
    band(k, "leggings", "left_leg", "strap.thigh", 0.9, 10.2, 10.9)
    pouch(k, "leggings", "right_leg", "pouch.leg", (-4.8, 7.4, -1.3), (-4.55, 10.4, 1.3), face="west", flap=0.1)

    # --- boots: tall, a spur on the right heel ------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.75, -0.75, 5.2)
        band(k, "boots", part, f"leather.top_{side}", BOOT_SIDE, 5.0, 6.6)
        x0, x1 = (-4.65, 0.85) if side == "r" else (-0.85, 4.65)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -2.9), (x1 + 0.15, -0.3, 3.5))
    k.add("boots", "right_leg", "steel.spur", (-2.3, 0.4, -3.2), (-1.5, 1.0, -2.75))
    k.add("boots", "right_leg", "steel.rowel", (-2.5, 0.0, -3.5), (-1.3, 1.4, -3.2))
    k.add("boots", "left_leg", "sheet.toe", (-0.65, -0.75, 2.75), (4.45, 1.5, 3.4), base=True)
    band(k, "boots", "left_leg", "strap.boot", 0.85, 2.4, 3.1)
    k.add("boots", "left_leg", "steel.boot_buckle", (4.75, 2.3, -0.5), (4.9, 3.2, 0.5))
    return k
