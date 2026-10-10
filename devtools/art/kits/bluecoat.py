"""Bluecoat: a pre-collapse hardsuit, pale institutional teal scuffed toward grey, with leather, webbing and
wood slats lashed over it by whoever scavenged it; a tank and hose over the shoulder (nfx's §6).
Diamond's answer: diamond becomes a faded hardsuit under scavenged leather, only reading as its
material when you look (§6).

The suit's shell is the base material: teal composite on diamond, cut steel on iron, brass on gold.
A sealed helmet with a broad visor, a faint lit strip across it (this kit is the high-tech one, §5),
a side lamp and a respirator port. The torso shell with a raised chest panel; the arms dark rubber
undersuit with a shell forearm on the right, the left forearm splinted with three wood slats tied on.
A tank slung slant on the back, its ribbed hose over the right shoulder to the helmet. A leather cap on
the right shoulder, the heavy shell on the left. Webbing bandolier, leather lashing round the chest.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from kitlib.geom import Kit
from kitlib.paint import canvas, glass, glow, hose, leather, painted, rubber, steel, webbing, wood
from kitlib.vocab import along, band, cases, cylinder, pouch, shell

LOOKS = {
    "rubber": rubber("#2c2f30"),
    "leather": leather("#5a3f2a"),
    "strap": leather("#4a3322"),
    "webbing": webbing("#3a3b33"),
    "wood": wood("#6b4c32"),
    "cord": webbing("#77684d", rim=0.18),
    "tank": painted("#5a615c"),
    "hose": hose("#2b2a28"),
    "glass": glass("#18201f"),
    "lens_lit": glow("#7fcfc4"),
    "painted": painted("#4a4d48"),
    "pouch": canvas("#5b5744"),
    "case": canvas("#4e4b3a"),
    "steel": steel(),
}
PALETTE = "diamond"  # the armour it is designed toward first (nfx's §6); the preview is drawn in it
DUST = 0.9


def build() -> Kit:
    k = Kit("bluecoat")

    # --- helmet: sealed, a broad visor ---------------------------------------------------------------------
    k.add("helmet", "head", "shell.helmet", (-5.0, 24.4, -5.0), (5.0, 33.0, 4.4), base=True, drop=("down",),
          paint={"stencil": {"text": "07", "scale": 3, "face": "north", "at": (0.5, 0.45), "colour": "#d4ccb6"}})
    k.add("helmet", "head", "shell.helmet_top", (-4.2, 33.0, -4.2), (4.2, 33.8, 3.6), base=True)
    k.add("helmet", "head", "shell.chin", (-4.6, 23.6, 3.8), (4.6, 26.6, 5.6), base=True)
    k.add("helmet", "head", "rubber.visor_seal", (-4.7, 26.2, 4.4), (4.7, 31.8, 5.05))
    k.add("helmet", "head", "glass.visor", (-4.2, 26.6, 4.6), (4.2, 31.4, 5.3))
    k.add("helmet", "head", "lens_lit.hud", (-3.6, 30.4, 5.3), (-0.4, 30.9, 5.45))
    k.add("helmet", "head", "painted.lamp", (5.0, 29.4, 0.4), (6.1, 31.6, 3.4))
    k.add("helmet", "head", "glass.lamp_lens", (5.05, 29.7, 3.4), (6.05, 31.3, 3.75))
    cylinder(k, "helmet", "head", "painted.port", (-5.55, 26.6, 1.4), 1.2, 1.1, "x")
    k.add("helmet", "head", "rubber.neck_seal", (-4.6, 23.2, -4.6), (4.6, 24.4, 4.6))

    # --- chestplate: the shell, the tank, the lashings -----------------------------------------------------
    shell(k, "chestplate", "body", "shell.torso", 1.0, 12.6, 25.0, base=True)
    k.add("chestplate", "body", "shell.chest_step", (-3.8, 17.2, 3.0), (3.8, 23.8, 3.7), base=True)
    k.add("chestplate", "body", "rubber.collar", (-4.5, 25.0, -3.3), (4.5, 26.0, 3.3))
    k.add("chestplate", "left_arm", "shell.pauldron", (3.2, 22.4, -3.8), (9.8, 26.4, 3.8), base=True)
    k.add("chestplate", "right_arm", "leather.shoulder", (-9.3, 23.6, -3.2), (-3.7, 25.4, 3.2), paint={"stitch": True})
    shell(k, "chestplate", "right_arm", "rubber.sleeve", 0.9, 12.0, 24.4)
    shell(k, "chestplate", "left_arm", "rubber.sleeve", 0.9, 12.0, 24.4)
    band(k, "chestplate", "right_arm", "shell.forearm", 1.15, 12.4, 17.4, base=True, paint={"hazard": "south"})
    band(k, "chestplate", "right_arm", "rubber.bellows", 1.05, 18.4, 19.4)
    for i in range(3):
        z0 = -2.7 + i * 1.9
        k.add("chestplate", "left_arm", f"wood.slat{i}", (9.0, 12.6 + i * 0.2, z0), (9.65, 18.4 - i * 0.3, z0 + 1.4))
    band(k, "chestplate", "left_arm", "cord.tie_low", 1.3, 13.5, 14.1)
    band(k, "chestplate", "left_arm", "cord.tie_high", 1.3, 16.9, 17.5)
    # The tank slung slant on the back, its valve, its hose over the right shoulder.
    tank = dict(rotation=(0.0, 0.0, 18.0), origin=(-1.5, 18.5, -4.6))
    cylinder(k, "chestplate", "body", "tank.body", (-1.5, 18.5, -4.6), 1.9, 8.0, "y", **tank)
    k.add("chestplate", "body", "steel.valve", (-3.35, 22.4, -5.2), (-2.15, 23.6, -4.0), **tank)
    k.add("chestplate", "body", "leather.tank_strap", (-4.4, 16.0, -6.7), (1.4, 16.8, -2.95))
    k.add("chestplate", "body", "leather.tank_strap_high", (-4.4, 20.4, -6.7), (1.4, 21.2, -2.95))
    k.add("chestplate", "body", "hose.over", (-4.2, 23.0, -4.6), (-3.0, 25.8, -3.4))
    k.add("chestplate", "body", "hose.top", (-4.2, 25.0, -3.4), (-3.0, 26.0, 2.4))
    k.add("chestplate", "body", "hose.front", (-4.2, 22.4, 2.4), (-3.0, 25.0, 3.6))
    start, end = (-4.6, 24.2), (4.4, 13.0)
    along(k, "chestplate", "body", "webbing.bandolier", start, end, 3.7, 4.3, 1.3)
    cases(k, "chestplate", "body", "case.cell", start, end, 4.3, 1.4, [0.40, 0.52, 0.62, 0.86], (1.4, 2.0))
    band(k, "chestplate", "body", "leather.lash", 1.2, 14.8, 15.6)
    pouch(k, "chestplate", "body", "pouch.chest", (-4.0, 12.8, 3.0), (-1.6, 14.6, 4.4))
    pouch(k, "chestplate", "body", "pouch.back", (1.8, 12.9, -4.3), (4.4, 15.6, -2.95), face="north")

    # --- leggings: undersuit, shell thighs, lashed wood -----------------------------------------------------
    k.add("leggings", "body", "rubber.waist", (-4.5, 11.0, -2.6), (4.5, 13.9, 2.6))
    k.add("leggings", "body", "webbing.belt", (-4.75, 11.2, -2.85), (4.75, 12.4, 2.85))
    k.add("leggings", "body", "steel.buckle", (-1.0, 11.25, 2.85), (1.0, 12.35, 3.3))
    shell(k, "leggings", "right_leg", "rubber.leg", 0.45, 1.0, 12.25)
    shell(k, "leggings", "left_leg", "rubber.leg", 0.45, 1.0, 12.25)
    shell(k, "leggings", "right_leg", "shell.thigh", 0.55, 9.4, 11.9, base=True)
    k.add("leggings", "left_leg", "shell.knee", (0.1, 6.6, 2.45), (3.7, 9.4, 3.3), base=True)
    band(k, "leggings", "right_leg", "rubber.knee_bellows", 0.55, 7.2, 8.2)
    band(k, "leggings", "left_leg", "leather.thigh_strap", 0.65, 10.2, 11.0)
    pouch(k, "leggings", "left_leg", "pouch.holster", (4.35, 8.6, -1.2), (4.95, 11.6, 1.4), face="east")
    k.add("leggings", "right_leg", "wood.thigh_slat", (-4.75, 6.8, -1.0), (-4.4, 11.2, 0.6))
    band(k, "leggings", "right_leg", "cord.thigh_tie", 0.75, 8.6, 9.1)

    # --- boots: rubber overboots, shell guards ---------------------------------------------------------------
    for side, part in (("r", "right_leg"), ("l", "left_leg")):
        shell(k, "boots", part, f"rubber.boot_{side}", 0.95, -0.75, 5.6)
        x0, x1 = (-4.75, 1.15) if side == "r" else (-1.15, 4.75)
        k.add("boots", part, f"rubber.sole_{side}", (x0 - 0.15, -1.0, -3.1), (x1 + 0.15, -0.3, 4.0))
    k.add("boots", "right_leg", "shell.shin", (-4.3, 1.4, 2.95), (0.5, 5.4, 3.5), base=True)
    k.add("boots", "left_leg", "shell.toe", (-0.75, -0.75, 2.95), (4.55, 1.3, 3.6), base=True)
    band(k, "boots", "left_leg", "leather.gaiter", 1.15, 3.2, 6.2, paint={"stitch": True})
    band(k, "boots", "right_leg", "webbing.boot_strap", 1.0, 0.6, 1.3)
    return k
