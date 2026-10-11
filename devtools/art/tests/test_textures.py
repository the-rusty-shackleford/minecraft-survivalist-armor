"""The kits' textures as the mod and the shaders read them (D-0001, D-0002; nfx's §7 and §9).

LabPBR: _n is the normal's x and y in red and green, occlusion in blue, height in alpha (here 0.8 to 1);
_s is smoothness in red, F0 in green (0-229 a dielectric, 230 and up a named metal: 230 iron, 231 gold,
234 copper, 255 the albedo as metal), porosity in blue (0-64), emission in alpha (255 none). Every
albedo sprite is listed once on the kits' own atlas (D-0008), its maps never as albedo. The painter keeps colours
within the value floor and ceiling and under the saturation cap but for hazard tape, lit lenses and
what a kit marks exempt.

Partitions: each kit; garment and each palette's material sheet; albedo, _n and _s; a lit kit or not;
painted face exempt or not.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import importlib
import json
from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from kitlib.paint import Painter
from kitlib.palettes import palettes
from kitlib.style import CEILING, FLOOR, SATURATION_CAP

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets"
KITS = sorted(p.stem for p in (Path(__file__).resolve().parents[1] / "kits").glob("*.py") if p.stem != "__init__")
METALS = {230, 231, 234, 255}


def sheets(kit: str) -> list[str]:
    return ["garment"] + [f"material_{p}" for p in sorted(palettes())]


def load(kit: str, name: str) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    d = ASSETS / "survivalistarmor/textures/kit" / kit
    return tuple(np.asarray(Image.open(d / f"{name}{s}.png").convert("RGBA")).astype(int) for s in ("", "_n", "_s"))


@pytest.mark.parametrize("kit", KITS)
def test_every_sheet_has_both_maps_at_its_size(kit):
    for name in sheets(kit):
        albedo, n, s = load(kit, name)
        assert albedo.shape == n.shape == s.shape, name
        h, w = albedo.shape[:2]
        assert h % 16 == 0 and w % 16 == 0, f"{name} {w}x{h}: not four mipmap levels' worth (D-0004)"


@pytest.mark.parametrize("kit", KITS)
def test_specular_follows_labpbr(kit):
    lit = any(c.material == "lens_lit" for c in importlib.import_module(f"kits.{kit}").build().cubes)
    for name in sheets(kit):
        _, _, s = load(kit, name)
        f0 = s[..., 1]
        assert np.all((f0 <= 229) | np.isin(f0, list(METALS))), f"{name}: an F0 between the dielectrics and the named metals"
        assert s[..., 2].max() <= 64, f"{name}: porosity past 64 is subsurface scattering"
        glowing = s[..., 3] < 255
        if name != "garment" or not lit:
            assert not glowing.any(), f"{name}: emission where nothing is lit"
        else:
            assert glowing.any(), "the lit lenses glow"


@pytest.mark.parametrize("kit", KITS)
def test_normals_follow_labpbr(kit):
    for name in sheets(kit):
        _, n, _ = load(kit, name)
        x, y = n[..., 0] / 127.5 - 1, n[..., 1] / 127.5 - 1
        assert (x * x + y * y).max() <= 1.03, f"{name}: a normal longer than one"
        assert n[..., 3].min() >= int(0.8 * 255) - 1, f"{name}: height below the floor the writer keeps"


def test_the_atlas_lists_every_albedo_once_and_no_map():
    atlas = json.loads((ASSETS / "survivalistarmor/atlases/kits.json").read_text())
    names = [s["resource"] for s in atlas["sources"]]
    assert len(names) == len(set(names))
    assert not [n for n in names if n.endswith("_n") or n.endswith("_s")]
    wanted = {f"survivalistarmor:kit/{k}/{s}" for k in KITS for s in sheets(k)}
    assert wanted <= set(names), sorted(wanted - set(names))[:5]
    for n in names:
        ns, path = n.split(":")
        assert (ASSETS / ns / "textures" / f"{path}.png").is_file(), n


def test_no_kit_is_on_the_block_atlas():
    # D-0008: on the block atlas the kits doubled it beside Pixlli's 128x (8192x8192 to 16384x8192).
    blocks = ASSETS / "minecraft/atlases/blocks.json"
    listed = [s.get("resource", s.get("source", "")) for s in json.loads(blocks.read_text())["sources"]] if blocks.exists() else []
    assert not [n for n in listed if "kit/" in n], listed[:5]


@pytest.mark.parametrize("kit", KITS)
def test_painted_colours_keep_to_the_style(kit):
    # Every eighth face, painted afresh: the cap and the value band hold wherever a face is not exempt.
    module = importlib.import_module(f"kits.{kit}")
    k = module.build()
    painter = Painter(k, module.LOOKS, dust=module.DUST)
    for f in k.faces()[::8]:
        p = painter.paint(f)
        if p.colour is None:
            continue
        c = p.colour[p.alpha > 0.5] if p.alpha is not None else p.colour.reshape(-1, 3)
        free = p.exempt[p.alpha > 0.5] if p.alpha is not None else p.exempt.reshape(-1)
        c = c[~free]
        if not len(c):
            continue
        mx, mn = c.max(-1), c.min(-1)
        sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
        assert sat.max() <= SATURATION_CAP + 1e-6, f"{f.cube.name} {f.direction}"
        assert np.all(c >= FLOOR - 1e-6) and np.all(c <= CEILING + 1e-6), f"{f.cube.name} {f.direction}"
