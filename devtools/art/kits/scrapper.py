"""Scrapper: chain-link fence cut into panels and wired onto a work jacket; a half-face twin-cartridge
respirator (nfx's §6). The weave is diamonds, never rings: that is what keeps it from reading medieval.

A knit cap over a balaclava, a cage of chain-link hung from a bar under its brim before the
goggles, and the respirator's two cartridges angled out from the jaw. The dark jacket carries a cut
panel on the chest and one on the back, standing off it and wired on at the corners, so the jacket
shows through the holes. Heavy left: scrap plates hazard-striped, chain-link round the upper arm; the
right a leather cap and a chain-link bracer bound with wire. A canister on the back, its hose over the
right shoulder. The panels are the base material: steel fence on iron, brass on gold, teal composite
on diamond (D-0001), and so are the scrap plates.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, cloth, glass, hose, leather, painted, rubber, steel
from kitlib.vocab import ARM_INNER, ARM_OUT, BOOT_SIDE, SLEEVE, along, band, cases, cylinder, pouch, shell

LOOKS = {
    "coat": canvas("#3c3a33", variation=0.05),
    "cloth": cloth("#36342f", variation=0.04),
    "knit": cloth("#4a4238", variation=0.05),
    "leather": leather("#4a3828"),
    "strap": leather("#3a2b1f"),
    "rubber": rubber("#262422"),
    "painted": painted("#4b4a3c"),
    "glass": glass("#121413"),
    "wire": steel("#5f6264", rim=0.3),
    "steel": steel(),
    "hose": hose("#2a2826"),
    "pouch": canvas("#4e4a3c"),
    "case": canvas("#55533f", variation=0.08),
}
PALETTE = "chainmail"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 1.0


def build() -> Kit:
    k = Kit("scrapper")

    # --- helmet: cap, balaclava, a chain-link cage, the twin cartridges ----------------------------------
    shell(k, "helmet", "head", "cloth.balaclava", 0.45, drop=("down",))
    k.add("helmet", "head", "knit.beanie", (-4.7, 30.2, -4.7), (4.7, 33.0, 4.7))
    k.add("helmet", "head", "knit.beanie_fold", (-4.9, 30.2, -4.9), (4.9, 31.2, 4.9))
    k.add("helmet", "head", "steel.guard_hinge", (-4.6, 29.4, 4.75), (4.6, 30.2, 5.9))
    k.add("helmet", "head", "mesh.faceguard", (-4.3, 26.6, 5.6), (4.3, 29.4, 5.8), base=True, drop=("north",))
    k.add("helmet", "head", "rubber.goggle_frame", (-4.1, 26.9, 4.45), (4.1, 28.6, 4.9))
    k.add("helmet", "head", "glass.eye_r", (-3.5, 27.1, 4.9), (-0.9, 28.4, 5.15))
    k.add("helmet", "head", "glass.eye_l", (0.9, 27.1, 4.9), (3.5, 28.4, 5.15))
    k.add("helmet", "head", "rubber.halfmask", (-3.0, 24.0, 4.45), (3.0, 26.5, 5.7))
    k.add("helmet", "head", "rubber.valve", (-1.2, 24.3, 5.7), (1.2, 25.9, 6.3))
    for side, cx, turn in (("r", -3.3, -25.0), ("l", 3.3, 25.0)):
        k.add("helmet", "head", f"painted.cartridge_{side}", (cx - 1.0, 24.0, 4.9), (cx + 1.0, 26.4, 6.4), rotation=(0.0, turn, 0.0),
              origin=(cx, 25.2, 5.4))
    band(k, "helmet", "head", "strap.mask", 0.8, 25.0, 25.7)

    # --- chestplate: the jacket, its panels, the scrap on the left ----------------------------------------
    shell(k, "chestplate", "body", "coat.jacket", 0.6, 12.5, 24.6)
    k.add("chestplate", "body", "coat.collar", (-4.3, 24.6, -3.0), (4.3, 25.6, 3.0))
    k.add("chestplate", "body", "mesh.front", (-4.3, 14.6, 2.75), (4.3, 23.6, 2.95), base=True, drop=("north",))
    k.add("chestplate", "body", "mesh.back", (-4.3, 14.0, -2.95), (4.3, 22.6, -2.75), base=True, drop=("south",))
    for i, (x, y) in enumerate(((-4.0, 23.2), (3.7, 23.2), (-4.0, 15.0), (3.7, 15.0))):
        k.add("chestplate", "body", f"wire.tie{i}", (x, y - 0.3, 2.65), (x + 0.3, y + 0.3, 3.1))
    k.add("chestplate", "left_arm", "plate.pauldron", (3.5, 22.3, -3.2), (9.5, 24.75, 3.2), base=True,
          paint={"hazard": "south", "stencil": {"text": "3", "scale": 3, "face": "east", "at": (0.5, 0.5)}})
    k.add("chestplate", "left_arm", "plate.pauldron_low", (3.7, 20.7, -2.95), (9.25, 22.5, 2.95), base=True, rotation=(0.0, 0.0, -6.0),
          origin=(4.0, 21.6, 0.0))
    shell(k, "chestplate", "right_arm", "coat.sleeve", SLEEVE, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "coat.sleeve", SLEEVE, 12.0, 24.4)
    band(k, "chestplate", "left_arm", "mesh.sleeve", 0.8, 16.4, 20.6, inner=0.55, base=True)
    k.add("chestplate", "right_arm", "leather.shoulder", (-9.0, 23.1, -3.0), (-3.7, 24.5, 3.0), paint={"stitch": True})
    band(k, "chestplate", "right_arm", "mesh.bracer", 0.9, 12.6, 16.6, inner=0.55, base=True)
    band(k, "chestplate", "right_arm", "wire.bracer_tie", ARM_OUT, 14.4, 14.7, inner=ARM_INNER)
    band(k, "chestplate", "left_arm", "leather.bracer", ARM_OUT, 12.4, 15.6, inner=ARM_INNER, paint={"stitch": True})
    start, end = (-4.2, 23.8), (3.9, 13.6)
    along(k, "chestplate", "body", "strap.bandolier", start, end, 3.1, 3.6, 1.2)
    cases(k, "chestplate", "body", "case.cell", start, end, 3.6, 1.1, [0.44, 0.53, 0.61, 0.69, 0.87], (1.3, 1.8))
    pouch(k, "chestplate", "body", "pouch.chest", (-4.1, 16.6, 2.95), (-1.8, 19.0, 4.1))
    pouch(k, "chestplate", "body", "pouch.belly", (-4.0, 12.8, 2.95), (-1.4, 15.0, 4.1))
    pouch(k, "chestplate", "body", "pouch.back", (0.6, 14.4, -4.3), (3.8, 18.4, -2.95), face="north")
    cylinder(k, "chestplate", "body", "painted.canister", (-2.6, 18.0, -3.95), 1.0, 4.0, "y")
    k.add("chestplate", "body", "hose.run", (-2.0, 20.0, -3.75), (-1.2, 25.3, -2.95))
    k.add("chestplate", "body", "hose.run_front", (-2.0, 22.4, 2.95), (-1.2, 25.0, 3.75))

    # --- leggings: work trousers, a panel wired to the left thigh -----------------------------------------
    k.add("leggings", "body", "cloth.waist", (-4.3, 11.0, -2.4), (4.3, 13.9, 2.4))
    k.add("leggings", "body", "strap.belt", (-4.55, 11.2, -2.65), (4.55, 12.4, 2.65))
    k.add("leggings", "body", "plate.buckle", (-1.0, 11.25, 2.65), (1.0, 12.35, 3.0), base=True)
    shell(k, "leggings", "right_leg", "cloth.leg", 0.5, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "cloth.leg", 0.5, 1.0, 12.25)
    k.add("leggings", "left_leg", "mesh.thigh", (0.3, 9.6, 2.6), (3.9, 11.9, 2.8), base=True, drop=("north",))
    band(k, "leggings", "left_leg", "wire.thigh", 0.85, 10.4, 10.7)
    k.add("leggings", "left_leg", "plate.knee", (0.2, 6.6, 2.55), (3.7, 9.2, 3.0), base=True, paint={"hazard": "south"})
    k.add("leggings", "right_leg", "leather.knee", (-3.6, 6.8, 2.5), (-0.2, 9.2, 3.0), paint={"stitch": True})
    pouch(k, "leggings", "right_leg", "pouch.leg", (-4.75, 7.0, -1.3), (-4.4, 10.0, 1.3), face="west", flap=0.15)

    # --- boots: leather, chain-link round the left shin -----------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"leather.boot_{side}", 0.75, -0.75, 5.6)
        x0, x1 = (-4.65, 0.85) if side == "r" else (-0.85, 4.65)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -2.9), (x1 + 0.15, -0.3, 3.5))
    band(k, "boots", "left_leg", "mesh.gaiter", 0.9, 2.6, 6.4, base=True)
    band(k, "boots", "left_leg", "wire.gaiter_tie", BOOT_SIDE, 4.2, 4.5)
    k.add("boots", "right_leg", "plate.toe", (-4.45, -0.75, 2.75), (0.65, 1.6, 3.45), base=True)
    band(k, "boots", "right_leg", "strap.lace", 0.9, 1.6, 2.3)
    band(k, "boots", "right_leg", "leather.cuff", BOOT_SIDE, 4.6, 6.2)
    return k
