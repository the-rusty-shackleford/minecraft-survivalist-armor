"""The seven kits as nfx's spec needs them (§5, §8) and as they must fit together (devtools/art/kitlib/vocab.py).

Partitions: each kit; each piece; a cube upright or turned; a cube on the body, an arm or a leg; above or
below each interface line; a garment cube or a material-sheet cube; chain-link or solid.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import importlib
from pathlib import Path

import numpy as np
import pytest

from kitlib.checks import visibility, world_corners
from kitlib.geom import BODY, PIECES
from kitlib.palettes import palettes
from kitlib.vocab import (BOOT_INSIDE, BOOT_TOP, CHEST_HEM, LEGGINGS_DEPTH, LEGGINGS_TOP, LEGGINGS_WIDTH,
                          TROUSER_OUTSIDE)

KITS = sorted(p.stem for p in (Path(__file__).resolve().parents[1] / "kits").glob("*.py") if p.stem != "__init__")
EPS = 1e-6


@pytest.fixture(scope="module", params=KITS)
def made(request):
    module = importlib.import_module(f"kits.{request.param}")
    return module, module.build()


def bounds(cube) -> tuple[np.ndarray, np.ndarray]:
    c = world_corners(cube)
    return c.min(axis=0), c.max(axis=0)


def leg_centre(part: str) -> float:
    (x0, _, _), (x1, _, _) = BODY[part]
    return (x0 + x1) / 2


def stem(name: str) -> str:
    for suffix in ("_x", "_flap"):
        if name.endswith(suffix):
            return name[: -len(suffix)]
    return name


def test_all_seven_kits_are_here():
    assert KITS == ["blacklung", "bluecoat", "duster", "hazmat", "magpie", "roadplate", "scrapper"]


def test_every_garment_cube_has_a_look_and_every_kit_a_palette(made):
    module, kit = made
    missing = sorted({c.material for c in kit.cubes if not c.base and c.material not in module.LOOKS})
    assert not missing, f"{kit.name}: no look for {missing}"
    assert module.PALETTE in palettes()


def test_every_piece_is_dressed_and_within_budget(made):
    # nfx's §8 and the plan's performance line: a fully kitted mob is about 160 cubes.
    _, kit = made
    for piece, parts in PIECES.items():
        n = kit.count(piece)
        assert 6 <= n <= 40, f"{kit.name} {piece}: {n} cubes"
        assert {c.part for c in kit.cubes if c.piece == piece} <= set(parts)
    assert len(kit.cubes) <= 160


def test_no_cube_is_buried(made):
    # A cube whose best face is mostly inside the others of its part is drawn and painted for nothing, or meant
    # something it no longer shows (the hat dome that sat inside the balaclava, the buckle inside the boot).
    _, kit = made
    seen = visibility(kit)
    buried = [f"{kit.cubes[i].piece} {kit.cubes[i].name} {v:.2f}" for i, v in seen.items() if v < 0.25]
    assert not buried, buried


def test_chest_piece_stops_at_its_hem(made):
    _, kit = made
    low = [c.name for c in kit.cubes if c.piece == "chestplate" and c.part == "body" and bounds(c)[0][1] < CHEST_HEM - EPS]
    assert not low, low


def test_leggings_stay_under_any_chest_piece(made):
    _, kit = made
    wrong = []
    for c in kit.cubes:
        if c.piece != "leggings" or c.part != "body":
            continue
        lo, hi = bounds(c)
        if hi[1] > LEGGINGS_TOP + EPS:
            wrong.append(f"{c.name} rises to {hi[1]:.2f}")
        if hi[1] > CHEST_HEM + EPS and (max(-lo[0], hi[0]) > LEGGINGS_WIDTH + EPS or max(-lo[2], hi[2]) > LEGGINGS_DEPTH + EPS):
            wrong.append(f"{c.name} stands out above the hem")
    assert not wrong, wrong


def test_chest_piece_clears_any_leggings(made):
    # Below LEGGINGS_TOP a chest cube either stands clear of the leggings' room or wraps all of it (the shell).
    _, kit = made
    wrong = []
    for c in kit.cubes:
        if c.piece != "chestplate" or c.part != "body":
            continue
        lo, hi = bounds(c)
        if lo[1] >= LEGGINGS_TOP - EPS:
            continue
        clear = lo[2] >= LEGGINGS_DEPTH - EPS or hi[2] <= -LEGGINGS_DEPTH + EPS or lo[0] >= LEGGINGS_WIDTH - EPS or hi[0] <= -LEGGINGS_WIDTH + EPS
        wraps = lo[0] <= -LEGGINGS_WIDTH and hi[0] >= LEGGINGS_WIDTH and lo[2] <= -LEGGINGS_DEPTH and hi[2] >= LEGGINGS_DEPTH
        if not (clear or wraps):
            wrong.append(c.name)
    assert not wrong, wrong


def test_boots_stop_below_the_knee(made):
    _, kit = made
    high = [f"{c.name} {bounds(c)[1][1]:.2f}" for c in kit.cubes if c.piece == "boots" and bounds(c)[1][1] > BOOT_TOP + EPS]
    assert not high, high


def test_trouser_legs_fit_inside_any_boots(made):
    _, kit = made
    wrong = []
    for c in kit.cubes:
        if c.piece != "leggings" or c.part == "body":
            continue
        lo, hi = bounds(c)
        if lo[1] >= BOOT_TOP - EPS:
            continue
        cx = leg_centre(c.part)
        if max(cx - lo[0], hi[0] - cx, -lo[2], hi[2]) > TROUSER_OUTSIDE + EPS:
            wrong.append(c.name)
    assert not wrong, wrong


def test_boots_stand_outside_any_trousers(made):
    _, kit = made
    wrong = []
    for c in kit.cubes:
        if c.piece != "boots":
            continue
        lo, hi = bounds(c)
        cx = leg_centre(c.part)
        clear = lo[2] >= TROUSER_OUTSIDE - EPS or hi[2] <= -TROUSER_OUTSIDE + EPS or lo[0] >= cx + TROUSER_OUTSIDE - EPS or hi[0] <= cx - TROUSER_OUTSIDE + EPS
        wraps = lo[0] <= cx - BOOT_INSIDE + EPS and hi[0] >= cx + BOOT_INSIDE - EPS and lo[2] <= -BOOT_INSIDE + EPS and hi[2] >= BOOT_INSIDE - EPS
        if not (clear or wraps):
            wrong.append(c.name)
    assert not wrong, wrong


def test_the_shared_vocabulary_is_all_there(made):
    # nfx's §5: a respirator or sealed mask, lenses, one or two hazard placements, a bandolier of four to six
    # cases, two pouches or more on the chest and one on the legs, a hose, exactly one stencil.
    module, kit = made
    helmet = [c for c in kit.cubes if c.piece == "helmet"]
    assert any(c.material in ("glass", "lens_lit") for c in helmet), "lenses"
    assert any(w in c.name for c in helmet for w in ("mask", "rag.face", "filter", "valve", "cartridge", "snout", "visor", "window")), \
        "a respirator or sealed mask"
    hazards = {stem(c.name) for c in kit.cubes if c.paint.get("hazard") or c.material == "hazard"}
    assert 1 <= len(hazards) <= 2, hazards
    stencils = [c.name for c in kit.cubes if c.paint.get("stencil")]
    assert len(stencils) == 1, stencils
    straps = [c for c in kit.cubes if c.piece == "chestplate" and c.name.endswith(".bandolier")]
    assert len(straps) == 1
    strap = straps[0]
    cases = [c for c in kit.cubes if c is not strap and c.piece == "chestplate" and c.rotation == strap.rotation and c.name[-1].isdigit()]
    assert 4 <= len(cases) <= 6, [c.name for c in cases]
    lo, hi = bounds(strap)
    assert (lo[0], hi[1]) < (-3.5, 23.0) and (hi[0], lo[1]) > (3.5, 14.5), "right shoulder to left hip"
    pouches = lambda piece: [c for c in kit.cubes if c.piece == piece and c.material == "pouch" and not c.name.endswith("_flap")]
    assert len(pouches("chestplate")) >= 2 and len(pouches("leggings")) >= 1
    hoses = {m for m, look in module.LOOKS.items() if look.kind == "hose"} | {"hose"}
    assert any(c.material in hoses for c in kit.cubes), "a hose"


def test_nothing_mirrors(made):
    # nfx's §5: heavy left shoulder, light right; bracers that don't match. The right arm, mirrored, is not the left.
    _, kit = made
    def boxes(part, flip):
        out = set()
        for c in kit.cubes:
            if c.piece == "chestplate" and c.part == part:
                (x0, y0, z0), (x1, y1, z1) = c.lo, c.hi
                if flip:
                    x0, x1 = -x1, -x0
                out.add((c.material, round(x0, 2), round(y0, 2), round(z0, 2), round(x1, 2), round(y1, 2), round(z1, 2)))
        return out
    assert boxes("right_arm", True) != boxes("left_arm", False)
    def shoulder(part):
        # What stands above the sleeve's top on that arm: the pauldron, the pad, the cap.
        return sum(np.prod(np.array(c.hi) - np.array(c.lo)) for c in kit.cubes
                   if c.piece == "chestplate" and c.part == part and bounds(c)[1][1] > 24.6)
    assert shoulder("left_arm") > shoulder("right_arm"), "the heavy shoulder is the left"


def test_the_checks_bite():
    # The measures above, on cubes made to fail them: a buckle inside its boot, a chest cube below the hem, a
    # strap under chain-link (which hides nothing), and a turned cube judged where it stands.
    from kitlib.geom import Kit
    k = Kit("probe")
    k.add("boots", "left_leg", "leather.boot", (-1.05, -0.75, -2.95), (4.85, 5.6, 2.95))
    k.add("boots", "left_leg", "steel.buckle", (3.95, 2.3, -0.5), (4.35, 3.2, 0.5))
    k.add("chestplate", "body", "mesh.panel", (-4.3, 14.6, 3.05), (4.3, 23.6, 3.25), base=True)
    k.add("chestplate", "body", "strap.under", (-2.0, 18.0, 3.1), (2.0, 19.0, 3.2))
    k.add("chestplate", "body", "case.low", (-1.0, 12.0, 3.0), (1.0, 13.0, 4.0), rotation=(0.0, 0.0, 30.0), origin=(0.0, 12.5, 3.5))
    seen = visibility(k)
    assert seen[1] == 0.0 and seen[0] == 1.0
    assert seen[3] == 1.0
    assert bounds(k.cubes[4])[0][1] < CHEST_HEM
