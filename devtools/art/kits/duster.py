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
from kitlib.vocab import along, band, cases, cylinder, pouch, shell

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
    shell(k, "chestplate", "body", "hide.vest", 0.9, 12.5, 24.9)
    k.add("chestplate", "body", "coat.back", (-5.2, 12.5, -3.35), (5.2, 25.0, -2.9))
    k.add("chestplate", "body", "coat.front_r", (-5.2, 12.5, 2.9), (-1.3, 25.0, 3.4))
    k.add("chestplate", "body", "coat.front_l", (1.3, 12.5, 2.9), (5.2, 25.0, 3.4))
    k.add("chestplate", "body", "coat.lapel_r", (-3.7, 20.4, 3.4), (-1.1, 25.0, 3.8))
    k.add("chestplate", "body", "coat.lapel_l", (1.1, 20.4, 3.4), (3.7, 25.0, 3.8))
    shell(k, "chestplate", "right_arm", "coat.sleeve", 1.0, 12.6, 24.5)
    shell(k, "chestplate", "left_arm", "coat.sleeve", 1.0, 12.6, 24.5)
    k.add("chestplate", "left_arm", "hide.mantle", (3.4, 21.6, -3.6), (9.6, 25.6, 3.6), paint={"stitch": True})
    k.add("chestplate", "left_arm", "sheet.pauldron", (3.6, 25.6, -3.1), (9.4, 26.4, 3.1), base=True, paint={"rivets": "up"})
    band(k, "chestplate", "left_arm", "sheet.bracer", 1.2, 12.8, 15.8, base=True, paint={"rivets": "east"})
    band(k, "chestplate", "right_arm", "leather.cuff", 1.15, 12.2, 14.6, paint={"stitch": True})
    band(k, "chestplate", "right_arm", "rag.wrap", 1.05, 15.0, 15.7)
    # Rag strips wound round the coat, over it and under the straps.
    band(k, "chestplate", "body", "rag.strip_high", 1.5, 16.4, 17.2)
    band(k, "chestplate", "body", "rag.strip_low", 1.5, 13.6, 14.3)
    # The pack and its straps, joined across the open coat.
    for side, x0, x1 in (("r", -3.9, -2.9), ("l", 2.9, 3.9)):
        k.add("chestplate", "body", f"strap.pack_{side}", (x0, 16.0, 3.8), (x1, 25.0, 4.3))
        k.add("chestplate", "body", f"strap.pack_top_{side}", (x0, 25.0, -3.8), (x1, 25.5, 4.3))
        k.add("chestplate", "body", f"strap.pack_back_{side}", (x0, 15.0, -3.8), (x1, 25.0, -3.35))
    k.add("chestplate", "body", "webbing.sternum", (-3.9, 19.2, 4.3), (3.9, 19.9, 4.65))
    pouch(k, "chestplate", "body", "canvas.pack", (-3.4, 14.4, -6.4), (3.4, 21.6, -3.8), face="north")
    k.add("chestplate", "body", "hazard.tape", (-3.45, 16.8, -6.6), (3.45, 18.0, -6.35))
    # A filter canister on the left strap, its hose up under the rag.
    cylinder(k, "chestplate", "body", "painted.canister", (3.4, 21.6, 5.0), 0.9, 2.6, "y")
    k.add("chestplate", "body", "hose.lead", (2.9, 22.9, 4.6), (3.7, 25.2, 5.4))
    start, end = (-4.7, 24.2), (4.6, 13.0)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 4.65, 5.2, 1.5)
    cases(k, "chestplate", "body", "cartridge.shell", start, end, 5.2, 0.9, [0.30, 0.36, 0.42, 0.49, 0.56, 0.62], (0.7, 1.7))
    pouch(k, "chestplate", "body", "pouch.hip", (-4.9, 12.7, 3.4), (-2.3, 15.2, 4.5))
    pouch(k, "chestplate", "body", "pouch.back", (-3.2, 12.6, -5.0), (0.2, 14.3, -3.35), face="north")

    # --- leggings: hide trousers under the coat's skirts --------------------------------------------------
    k.add("leggings", "body", "hide.waist", (-4.5, 11.0, -2.6), (4.5, 13.9, 2.6))
    k.add("leggings", "body", "strap.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "sheet.buckle", (-1.1, 11.2, 2.85), (1.1, 12.4, 3.35), base=True)
    shell(k, "leggings", "right_leg", "hide.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "hide.trouser", 0.5, 1.0, 12.25)
    shell(k, "leggings", "right_leg", "coat.skirt", 0.75, 6.8, 12.25)
    shell(k, "leggings", "left_leg", "coat.skirt", 0.75, 6.8, 12.25)
    k.add("leggings", "right_leg", "sheet.knee", (-3.7, 6.6, 2.75), (-0.1, 9.2, 3.45), base=True, paint={"rivets": "south"})
    k.add("leggings", "left_leg", "hide.knee", (0.2, 6.8, 2.75), (3.6, 9.0, 3.2), paint={"stitch": True})
    band(k, "leggings", "left_leg", "strap.thigh", 0.9, 10.2, 10.9)
    pouch(k, "leggings", "right_leg", "pouch.leg", (-5.0, 7.4, -1.3), (-4.65, 10.4, 1.3), face="west")

    # --- boots: tall, a spur on the right heel ------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.95, -0.75, 5.2)
        band(k, "boots", part, f"leather.top_{side}", 1.2, 5.0, 6.6)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    k.add("boots", "right_leg", "steel.spur", (-2.3, 0.4, -3.55), (-1.5, 1.0, -2.95))
    k.add("boots", "right_leg", "steel.rowel", (-2.5, 0.0, -4.25), (-1.3, 1.4, -3.55))
    k.add("boots", "left_leg", "sheet.toe", (-0.75, -0.75, 2.95), (4.55, 1.5, 3.7), base=True)
    band(k, "boots", "left_leg", "strap.boot", 1.05, 2.4, 3.1)
    k.add("boots", "left_leg", "steel.boot_buckle", (4.95, 2.3, -0.5), (5.3, 3.2, 0.5))
    return k
