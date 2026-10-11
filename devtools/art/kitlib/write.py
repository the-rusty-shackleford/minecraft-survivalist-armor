"""write: a painted kit as the mod reads it -- its Blockbench project, its textures and their LabPBR maps.

    assets/survivalistarmor/survivalistarmor/kits/<kit>.bbmodel
    assets/survivalistarmor/textures/kit/<kit>/garment.png, garment_n.png, garment_s.png
    assets/survivalistarmor/textures/kit/<kit>/material_<palette>.png, _n.png, _s.png  (one per palette)

LabPBR (nfx's §9): _n is the tangent-space normal in red and green (OpenGL's: green up the texture),
ambient occlusion in blue, height in alpha; _s is perceptual smoothness in red, F0 in green (0-229 a
dielectric's reflectance, 230 and up a named metal), porosity in blue (0-64), emission in alpha
(0-254; 255 is none). Between faces, each face's colours are spread into its padding so filtering
never brings in black.

The project names the garment and the iron sheet by their paths beside it, so Blockbench opened on
it from the resources shows the kit as painted; the mod reads only the PNGs (its per-texture UV sizes
are the PNGs' sizes). Embedding them doubled each kit's weight in the jar.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import json
import uuid as uuidlib
from pathlib import Path

import numpy as np
from PIL import Image

from .geom import DIRECTIONS, Face, Kit
from .pack import Spot
from .paint import Painted
from .palettes import Palette
from .style import cap_saturation, clamp_value

NORMAL_TILT = 2.2  # height-to-normal strength: Pixlli's own normals tilt 5-13 degrees on average


class Sheets:
    """The kit's two canvases, filled face by face."""

    def __init__(self, sizes: dict[str, tuple[int, int]]):
        gw, gh = sizes["garment"]
        mw, mh = sizes["material"]
        self.g_colour = np.zeros((gh, gw, 3))
        self.g_alpha = np.zeros((gh, gw))
        self.g_height = np.ones((gh, gw))
        self.g_ao = np.ones((gh, gw))
        self.g_smooth = np.zeros((gh, gw))
        self.g_f0 = np.zeros((gh, gw), dtype=int)
        self.g_porosity = np.zeros((gh, gw), dtype=int)
        self.g_emission = np.zeros((gh, gw), dtype=int)
        self.g_filled = np.zeros((gh, gw), bool)
        self.m_key = np.full((mh, mw), 0.5)
        self.m_alpha = np.zeros((mh, mw))
        self.m_height = np.ones((mh, mw))
        self.m_ao = np.ones((mh, mw))
        self.m_weather = np.zeros((mh, mw))
        self.m_wear = np.zeros((mh, mw))
        self.m_paint = np.zeros((mh, mw, 4))
        self.m_dust = np.zeros((mh, mw))
        self.m_filled = np.zeros((mh, mw), bool)
        self.normal_g = None
        self.normal_m = None

    def put(self, spot: Spot, p: Painted) -> None:
        sl = (slice(spot.y, spot.y + p.h), slice(spot.x, spot.x + p.w))
        nx, ny = normals(p.height)
        if spot.sheet == "garment":
            self.g_colour[sl] = p.colour
            self.g_alpha[sl] = p.alpha
            self.g_height[sl] = p.height
            self.g_ao[sl] = p.occlusion
            self.g_smooth[sl] = p.smooth
            self.g_f0[sl] = p.f0
            self.g_porosity[sl] = p.porosity
            self.g_emission[sl] = p.emission
            self.g_filled[sl] = True
            self._normal("g", sl, nx, ny)
        else:
            self.m_key[sl] = p.key
            self.m_alpha[sl] = p.alpha
            self.m_height[sl] = p.height
            self.m_ao[sl] = p.occlusion
            self.m_weather[sl] = p.weather
            self.m_wear[sl] = p.wear
            self.m_paint[sl] = p.paint
            self.m_dust[sl] = p.dust
            self.m_filled[sl] = True
            self._normal("m", sl, nx, ny)

    def _normal(self, which: str, sl, nx: np.ndarray, ny: np.ndarray) -> None:
        name = "normal_" + which
        if getattr(self, name) is None:
            shape = (self.g_alpha if which == "g" else self.m_alpha).shape
            setattr(self, name, np.zeros(shape + (2,)))
        getattr(self, name)[sl] = np.stack([nx, ny], axis=-1)


def normals(height: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """effects: returns the tangent-space normal's x and y from a face's height (x right, y up the texture)."""
    h = np.pad(height, 1, mode="edge")
    dx = (h[1:-1, 2:] - h[1:-1, :-2]) / 2
    dy = (h[2:, 1:-1] - h[:-2, 1:-1]) / 2
    nx, ny, nz = -dx * NORMAL_TILT, dy * NORMAL_TILT, np.ones_like(dx)
    length = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    return nx / length, ny / length


def spread(values: np.ndarray, filled: np.ndarray, steps: int = 3) -> np.ndarray:
    """effects: returns values with each unfilled pixel near a face given its neighbour's value, `steps` pixels out."""
    out = values.copy()
    have = filled.copy()
    for _ in range(steps):
        grown = have.copy()
        for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            src = np.roll(have, (dy, dx), axis=(0, 1))
            take = src & ~grown
            out[take] = np.roll(out, (dy, dx), axis=(0, 1))[take]
            grown |= take
        have = grown
    return out


def ramp(palette: Palette, key: np.ndarray) -> np.ndarray:
    """effects: returns the colour at each key position on the palette's ramp (0 its darkest stop, 1 its lightest)."""
    stops = palette.ramp
    k = np.clip(key, 0, 1) * (len(stops) - 1)
    i = np.clip(np.floor(k).astype(int), 0, len(stops) - 2)
    f = (k - i)[..., None]
    return stops[i] * (1 - f) + stops[i + 1] * f


def material_sheet(s: Sheets, palette: Palette, dust_colour: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """effects: returns the material sheet's colour (h, w, 4), _s and _n in this palette."""
    colour = ramp(palette, s.m_key)
    colour = colour + (palette.weather - colour) * (s.m_weather * palette.weathering)[..., None]
    colour = colour + (ramp(palette, np.ones_like(s.m_key)) - colour) * (s.m_wear * 0.55)[..., None]
    pa = s.m_paint[..., 3:4]
    colour = colour * (1 - pa) + s.m_paint[..., :3] * pa
    colour = colour * (0.62 + 0.38 * s.m_ao[..., None])
    colour = colour + (dust_colour - colour) * (s.m_dust * 0.75)[..., None]
    colour = clamp_value(cap_saturation(colour))
    painted = s.m_paint[..., 3] > 0.5
    smooth = np.full(s.m_key.shape, palette.smooth)
    smooth = np.where(painted, 0.36, smooth)
    smooth = smooth - s.m_weather * palette.weathering * 0.25
    smooth = smooth + (0.12 - smooth) * np.clip(s.m_dust * 1.3, 0, 1)
    f0 = np.full(s.m_key.shape, palette.f0, dtype=int)
    f0 = np.where(painted, 12, f0)
    f0 = np.where((s.m_dust > 0.45) & (f0 >= 230), 10, f0)
    f0 = np.where((s.m_weather * palette.weathering > 0.3) & (f0 >= 230), 20, f0)
    porosity = np.clip(np.full(s.m_key.shape, palette.porosity) + s.m_dust * 30, 0, 64).astype(int)
    filled = s.m_filled
    rgba = np.dstack([spread(colour, filled), np.where(filled, s.m_alpha, 1.0)])
    spec = labpbr_s(spread(smooth, filled), spread(f0, filled), spread(porosity, filled), np.zeros_like(f0))
    return rgba, spec, labpbr_n(s.normal_m, spread(s.m_ao, filled), spread(s.m_height, filled), filled)


def garment_sheet(s: Sheets) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    filled = s.g_filled
    rgba = np.dstack([spread(s.g_colour, filled), np.where(filled, s.g_alpha, 1.0)])
    spec = labpbr_s(spread(s.g_smooth, filled), spread(s.g_f0, filled), spread(s.g_porosity, filled), spread(s.g_emission, filled))
    return rgba, spec, labpbr_n(s.normal_g, spread(s.g_ao, filled), spread(s.g_height, filled), filled)


def labpbr_s(smooth, f0, porosity, emission) -> np.ndarray:
    out = np.zeros(smooth.shape + (4,), dtype=np.uint8)
    out[..., 0] = np.clip(smooth * 255, 0, 255).astype(np.uint8)
    out[..., 1] = np.clip(f0, 0, 255).astype(np.uint8)
    out[..., 2] = np.clip(porosity, 0, 64).astype(np.uint8)
    out[..., 3] = np.where(emission > 0, np.clip(emission, 1, 254), 255).astype(np.uint8)
    return out


def labpbr_n(nxy, ao, height, filled) -> np.ndarray:
    if nxy is None:
        nxy = np.zeros(ao.shape + (2,))
    nxy = np.where(filled[..., None], nxy, 0.0)
    h = height - height[filled].min() if filled.any() else height
    top = h[filled].max() if filled.any() and h[filled].max() > 0 else 1.0
    out = np.zeros(ao.shape + (4,), dtype=np.uint8)
    out[..., 0] = np.clip((nxy[..., 0] * 0.5 + 0.5) * 255, 0, 255).astype(np.uint8)
    out[..., 1] = np.clip((nxy[..., 1] * 0.5 + 0.5) * 255, 0, 255).astype(np.uint8)
    out[..., 2] = np.clip(ao * 255, 0, 255).astype(np.uint8)
    out[..., 3] = np.clip((0.80 + 0.20 * np.clip(h / top, 0, 1)) * 255, 1, 255).astype(np.uint8)
    return out


def png(array: np.ndarray, path: Path) -> None:
    a = array
    if a.dtype != np.uint8:
        a = np.clip(a * 255 + 0.5, 0, 255).astype(np.uint8)
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(a).save(path, optimize=True)


def project(kit: Kit, faces: list[Face], spots: dict[int, Spot], sizes: dict[str, tuple[int, int]]) -> dict:
    """effects: returns the kit's Blockbench project: its cubes in piece and part folders, each face's
    texture rectangle (a culled face none), both textures by their paths from the project."""
    gw, gh = sizes["garment"]
    mw, mh = sizes["material"]
    by_cube: dict[int, dict[str, tuple[Face, Spot]]] = {}
    for i, f in enumerate(faces):
        by_cube.setdefault(f.index, {})[f.direction] = (f, spots[i])
    elements, folders = [], {}
    for index, cube in enumerate(kit.cubes):
        uuid = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, f"{kit.name}/cube/{index}"))
        faces_json = {}
        for d in DIRECTIONS:
            if d in by_cube.get(index, {}):
                _, spot = by_cube[index][d]
                faces_json[d] = {"uv": [spot.x, spot.y, spot.x + spot.w, spot.y + spot.h], "texture": 1 if spot.sheet == "material" else 0}
            else:
                faces_json[d] = {"uv": [0, 0, 0, 0], "texture": None}
        el = {"name": cube.name, "type": "cube", "uuid": uuid, "box_uv": False, "from": list(cube.lo), "to": list(cube.hi),
              "origin": list(map(float, cube.pivot())), "rotation": list(cube.rotation), "faces": faces_json}
        elements.append(el)
        folders.setdefault(cube.piece, {}).setdefault(cube.part, []).append(uuid)
    groups, outliner = [], []
    for piece, parts in folders.items():
        pu = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, f"{kit.name}/{piece}"))
        groups.append({"name": piece, "uuid": pu, "origin": [0, 0, 0], "rotation": [0, 0, 0]})
        children = []
        for part, ids in parts.items():
            qu = str(uuidlib.uuid5(uuidlib.NAMESPACE_URL, f"{kit.name}/{piece}/{part}"))
            groups.append({"name": part, "uuid": qu, "origin": [0, 0, 0], "rotation": [0, 0, 0]})
            children.append({"uuid": qu, "isOpen": False, "children": ids})
        outliner.append({"uuid": pu, "isOpen": True, "children": children})
    textures = [
        {"name": "garment", "width": gw, "height": gh, "uv_width": gw, "uv_height": gh,
         "relative_path": f"../../textures/kit/{kit.name}/garment.png"},
        {"name": "material", "width": mw, "height": mh, "uv_width": mw, "uv_height": mh,
         "relative_path": f"../../textures/kit/{kit.name}/material_iron.png"},
    ]
    return {"meta": {"format_version": "5.0", "model_format": "free", "box_uv": False}, "name": kit.name,
            "resolution": {"width": gw, "height": gh}, "elements": elements, "groups": groups, "outliner": outliner,
            "textures": textures}


def write_kit(root: Path, kit: Kit, faces: list[Face], spots: dict[int, Spot], sizes: dict[str, tuple[int, int]], sheets: Sheets,
              palettes: dict[str, Palette], dust_colour: np.ndarray) -> list[str]:
    """effects: writes the kit's project, textures and maps under root (src/main/resources); returns its sprite names."""
    assets = root / "assets/survivalistarmor"
    tex = assets / "textures/kit" / kit.name
    garment_rgba, garment_s, garment_n = garment_sheet(sheets)
    png(garment_rgba, tex / "garment.png")
    png(garment_s, tex / "garment_s.png")
    png(garment_n, tex / "garment_n.png")
    sprites = [f"survivalistarmor:kit/{kit.name}/garment"]
    for name, palette in palettes.items():
        rgba, spec, nrm = material_sheet(sheets, palette, dust_colour)
        png(rgba, tex / f"material_{name}.png")
        png(spec, tex / f"material_{name}_s.png")
        png(nrm, tex / f"material_{name}_n.png")
        sprites.append(f"survivalistarmor:kit/{kit.name}/material_{name}")
    proj = project(kit, faces, spots, sizes)
    out = assets / "survivalistarmor/kits" / f"{kit.name}.bbmodel"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(proj, separators=(",", ":")) + "\n")
    return sprites


def write_atlas(root: Path, sprites: list[str]) -> None:
    """effects: lists every kit sprite on the kits' own atlas (D-0008), singly, so no _n or _s map is stitched as a colour."""
    out = root / "assets/survivalistarmor/atlases/kits.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"sources": [{"type": "single", "resource": s} for s in sorted(sprites)]}, indent=2) + "\n")
