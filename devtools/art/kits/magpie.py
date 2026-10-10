"""Magpie: brass busbar, copper pipe and contacts stripped out of a substation, mounted on a worker's
coat (nfx's §6). Gold's answer: gold becomes brass hardware on a work coat, the least saturated thing
in the lineup at a glance, its material showing only when you look (§6).

A lineman's blue-grey coat. Four bent busbar strips across the chest as lamellar, held on by webbing;
soft metal, so they bend and dent rather than chip. The heavy left shoulder is two busbars bent over
it with a ceramic insulator standing on top; the right a leather patch. A hard hat with a hazard band
and a busbar crest, over a balaclava; round goggles in copper rims and a half mask with one brass
filter. A copper fuse cartridge on the hip and an insulated cable up the chest. Black electrical
tape on the right arm only. "11KV" sprayed on the back. Brass on gold, cut steel on iron, teal
composite on diamond: the busbar is the base material wherever it goes (D-0001).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, ceramic, cloth, copper, glass, hose, leather, painted, rubber, steel, webbing
from kitlib.vocab import along, band, cases, cylinder, pouch, shell

LOOKS = {
    "coat": canvas("#3d454a", variation=0.05),
    "canvas": canvas("#5a5443"),
    "cloth": cloth("#38352f"),
    "balaclava": cloth("#4a463f"),
    "leather": leather("#4f3a28"),
    "strap": leather("#3f2d20"),
    "webbing": webbing("#33322c"),
    "rubber": rubber("#272523"),
    "hose": hose("#2b2927"),
    "tape": rubber("#1f1e1d", rim=0.3),
    "paint": painted("#9e9a8a", variation=0.04),
    "ceramic": ceramic(),
    "pipe": copper(),
    "case": painted("#3e2b1f"),
    "pouch": canvas("#56513f"),
    "glass": glass(),
    "steel": steel(),
}
PALETTE = "gold"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 1.0


def build() -> Kit:
    k = Kit("magpie")

    # --- helmet: a lineman's hard hat over a balaclava -----------------------------------------------------
    shell(k, "helmet", "head", "balaclava.hood", 0.6, drop=("down",))
    k.add("helmet", "head", "paint.hat_brim", (-5.55, 29.8, -5.55), (5.55, 30.4, 6.3), paint={"hazard": "south"})
    k.add("helmet", "head", "paint.hat_dome", (-4.95, 30.4, -4.95), (4.95, 32.9, 4.95))
    k.add("helmet", "head", "paint.hat_crown", (-3.6, 32.9, -3.6), (3.6, 33.6, 3.6))
    k.add("helmet", "head", "busbar.crest", (-0.7, 32.8, -5.2), (0.7, 34.3, 5.3), base=True, paint={"rivets": "east"})
    k.add("helmet", "head", "rubber.halfmask", (-3.0, 24.3, 4.6), (3.0, 27.6, 6.0))
    cylinder(k, "helmet", "head", "contact.filter", (0.0, 25.9, 6.85), 1.55, 1.7, "z", base=True)
    for side, x0, x1 in (("r", -3.9, -0.8), ("l", 0.8, 3.9)):
        k.add("helmet", "head", f"pipe.goggle_rim_{side}", (x0, 27.8, 4.6), (x1, 30.2, 5.3))
        k.add("helmet", "head", f"glass.lens_{side}", (x0 + 0.4, 28.2, 5.3), (x1 - 0.4, 29.8, 5.5))
    band(k, "helmet", "head", "leather.goggle_strap", 0.85, 28.6, 29.4)

    # --- chestplate: the coat and its busbar ----------------------------------------------------------------
    shell(k, "chestplate", "body", "coat.body", 0.9, 12.5, 24.9,
          paint={"stencil": {"text": "11KV", "scale": 3, "face": "north", "at": (0.5, 0.35), "colour": "#cfc6ad"}})
    k.add("chestplate", "body", "coat.collar", (-4.6, 24.4, -3.4), (4.6, 26.0, 3.4))
    for i, (y, turn) in enumerate(((22.4, 3.0), (20.5, -2.0), (18.6, 4.5), (16.8, -3.5))):
        k.add("chestplate", "body", f"busbar.lam{i}", (-4.4 + (0.3 if i == 2 else 0.0), y - 0.8, 2.9), (4.4, y + 0.8, 3.55), base=True,
              rotation=(0.0, 0.0, turn), origin=(0.0, y, 3.2), paint={"rivets": "south", "pitch": 9})
    for side, x0, x1 in (("r", -3.1, -2.2), ("l", 2.2, 3.1)):
        k.add("chestplate", "body", f"webbing.hold_{side}", (x0, 15.4, 3.55), (x1, 24.9, 4.05))
    k.add("chestplate", "left_arm", "busbar.pauldron", (3.2, 23.9, -3.6), (9.6, 25.2, 3.6), base=True, rotation=(0.0, 0.0, -8.0),
          origin=(4.0, 24.5, 0.0))
    k.add("chestplate", "left_arm", "busbar.pauldron_low", (3.6, 22.4, -3.4), (9.9, 23.7, 3.4), base=True, rotation=(0.0, 0.0, -12.0),
          origin=(4.0, 23.0, 0.0))
    cylinder(k, "chestplate", "left_arm", "ceramic.insulator", (6.4, 25.9, 0.0), 1.0, 1.9, "y")
    k.add("chestplate", "right_arm", "leather.shoulder", (-9.2, 23.6, -3.1), (-3.8, 25.3, 3.1), paint={"stitch": True})
    shell(k, "chestplate", "right_arm", "coat.sleeve", 0.9, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "coat.sleeve", 0.9, 12.0, 24.4)
    band(k, "chestplate", "left_arm", "contact.bracer", 1.2, 12.6, 15.4, base=True, paint={"rivets": "east"})
    band(k, "chestplate", "right_arm", "leather.glove", 1.25, 11.8, 14.8, paint={"stitch": True})
    band(k, "chestplate", "right_arm", "tape.wrap", 1.0, 16.5, 17.2)
    band(k, "chestplate", "right_arm", "tape.wrap_high", 1.0, 19.3, 19.9)
    # The fuse cartridge on the hip and its cable up the chest, over the shoulder.
    cylinder(k, "chestplate", "body", "pipe.fuse", (-3.4, 15.3, 4.9), 1.1, 3.4, "y")
    k.add("chestplate", "body", "hose.cable", (-4.25, 17.0, 3.7), (-3.25, 25.2, 4.6))
    k.add("chestplate", "body", "hose.cable_top", (-4.25, 25.2, -0.5), (-3.25, 26.1, 4.6))
    start, end = (-4.6, 24.2), (4.5, 13.0)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 4.05, 4.65, 1.3)
    cases(k, "chestplate", "body", "case.fuse", start, end, 4.65, 1.3, [0.36, 0.47, 0.57, 0.84], (1.2, 2.1))
    pouch(k, "chestplate", "body", "pouch.pocket_r", (-4.0, 12.8, 2.9), (-1.4, 15.0, 3.9))
    pouch(k, "chestplate", "body", "pouch.pocket_l", (1.4, 12.8, 2.9), (4.0, 15.0, 3.9))

    # --- leggings: work trousers ----------------------------------------------------------------------------
    k.add("leggings", "body", "cloth.waist", (-4.5, 11.0, -2.6), (4.5, 13.9, 2.6))
    k.add("leggings", "body", "leather.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "steel.buckle", (-1.0, 11.25, 2.85), (1.0, 12.35, 3.3))
    pouch(k, "leggings", "body", "pouch.tools", (-5.4, 8.8, -1.3), (-4.6, 12.0, 1.3), face="west")
    shell(k, "leggings", "right_leg", "cloth.leg", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "cloth.leg", 0.5, 1.0, 12.25)
    k.add("leggings", "left_leg", "contact.knee", (0.15, 6.6, 2.5), (3.65, 9.4, 3.4), base=True, paint={"rivets": "south"})
    k.add("leggings", "right_leg", "canvas.knee_patch", (-3.6, 6.8, 2.5), (-0.2, 9.2, 2.9), paint={"stitch": True})
    band(k, "leggings", "right_leg", "pipe.wire_coil", 0.7, 10.0, 10.8)
    pouch(k, "leggings", "left_leg", "pouch.cargo", (4.4, 6.6, -1.4), (4.95, 9.8, 1.4), face="east")

    # --- boots ----------------------------------------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.95, -0.75, 5.5)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    k.add("boots", "left_leg", "contact.toe", (-0.75, -0.75, 2.95), (4.55, 1.5, 3.7), base=True)
    band(k, "boots", "right_leg", "pipe.ankle_wire", 1.05, 2.4, 3.0)
    band(k, "boots", "left_leg", "canvas.gaiter", 1.1, 3.6, 6.6, paint={"stitch": True})
    band(k, "boots", "right_leg", "leather.cuff", 1.2, 4.6, 6.0)
    band(k, "boots", "right_leg", "webbing.lace", 1.0, 1.5, 2.1)
    return k
