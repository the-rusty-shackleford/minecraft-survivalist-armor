"""placeholder.py: write the booth's placeholder kit, survivalistarmor_gametest:placeholder.

    uv run --no-project --with pillow python devtools/booth/placeholder.py

A kit for testing the drawing, never shipped (it lives in the gametest mod): one cube per part a
piece rides on, sized over the game's own armour box, plus a plate from the material sheet, a lit
lens on the helmet, a strap and a buckle. Every face is painted by the way it faces -- the
figure's front red, its back blue, its right (-x) green, its left (+x) yellow, up white, down
black -- so a photograph shows at once whether anything is turned or mirrored. Each palette's
material sheet is one flat colour.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/gametest/resources/assets"
NS = "survivalistarmor_gametest"
KIT = "placeholder"

FACE_COLOURS = {"south": (200, 40, 40), "north": (40, 60, 200), "west": (40, 170, 60),
                "east": (220, 200, 40), "up": (235, 235, 235), "down": (25, 25, 25)}
PALETTES = {"neutral": (200, 40, 200), "iron": (150, 150, 155), "gold": (200, 170, 60), "diamond": (80, 190, 190),
            "netherite": (60, 55, 60), "leather": (140, 90, 50), "chainmail": (110, 110, 115), "turtle": (70, 140, 60)}

# (piece, part, cube name, from, to, texture: 0 garment, 1 material)
CUBES = [
    ("helmet", "head", "canvas.shell", (-5, 23, -5), (5, 33, 5), 0),
    ("helmet", "head", "plate.crown", (-4, 33, -4), (4, 34, 4), 1),
    ("helmet", "head", "lens_lit.right", (-3, 28, 5), (-1, 30, 5.5), 0),
    ("chestplate", "body", "canvas.shell", (-5, 11, -3), (5, 25, 3), 0),
    ("chestplate", "body", "plate.chest", (-3, 16, 3), (3, 23, 4), 1),
    ("chestplate", "body", "strap.belt", (-5, 13, 3), (5, 14, 3.5), 0),
    ("chestplate", "right_arm", "canvas.sleeve", (-9, 11, -3), (-3, 25, 3), 0),
    ("chestplate", "right_arm", "plate.pauldron", (-10, 22, -3.5), (-3, 26, 3.5), 1),
    ("chestplate", "left_arm", "canvas.sleeve", (3, 11, -3), (9, 25, 3), 0),
    ("leggings", "body", "canvas.waist", (-4.5, 11.5, -2.5), (4.5, 14, 2.5), 0),
    ("leggings", "body", "buckle.belt", (-1, 12, 2.5), (1, 13.5, 3), 0),
    ("leggings", "right_leg", "canvas.leg", (-4.4, 1, -2.5), (0.6, 12.5, 2.5), 0),
    ("leggings", "left_leg", "canvas.leg", (-0.6, 1, -2.5), (4.4, 12.5, 2.5), 0),
    ("boots", "right_leg", "hide.boot", (-4.9, -1, -3), (1.1, 5, 3), 0),
    ("boots", "right_leg", "plate.toe", (-4.5, -1, 3), (0.7, 2, 4), 1),
    ("boots", "left_leg", "hide.boot", (-1.1, -1, -3), (4.9, 5, 3), 0),
]

CELL = 4
GARMENT = 64


def main() -> None:
    write(KIT, pbr=False)
    # Its twin with deliberately loud LabPBR maps -- every texel a steep bump, polished iron -- to see
    # whether the shader reads a kit's maps from the block atlas (the plan's spike S4).
    write(KIT + "_pbr", pbr=True)


def labpbr(size: tuple[int, int]) -> tuple[Image.Image, Image.Image]:
    """Loud maps: _n a pyramid in every 4-texel cell (RG the tangent normal, B no occlusion, A the surface),
    _s polished iron (R smoothness 230, G 230 = LabPBR's iron, B no porosity, A 255 = no emission)."""
    n = Image.new("RGBA", size)
    for x in range(size[0]):
        for y in range(size[1]):
            dx, dy = (x % CELL) - 1.5, (y % CELL) - 1.5
            nx, ny = (0.7 if dx > 0 else -0.7) if abs(dx) >= abs(dy) else 0.0, (0.7 if dy > 0 else -0.7) if abs(dy) > abs(dx) else 0.0
            n.putpixel((x, y), (int((nx * 0.5 + 0.5) * 255), int((ny * 0.5 + 0.5) * 255), 255, 255))
    return n, Image.new("RGBA", size, (230, 230, 0, 255))


def write(kit: str, pbr: bool) -> None:
    garment = Image.new("RGBA", (GARMENT, GARMENT), (0, 0, 0, 0))
    elements, folders = [], {}
    cell = 0
    for n, (piece, part, name, lo, hi, texture) in enumerate(CUBES):
        uuid = f"c{n}"
        faces = {}
        for face, colour in FACE_COLOURS.items():
            if texture == 1:
                faces[face] = {"uv": [0, 0, 16, 16], "texture": 1}
                continue
            u, v = (cell % (GARMENT // CELL)) * CELL, (cell // (GARMENT // CELL)) * CELL
            cell += 1
            for x in range(u, u + CELL):
                for y in range(v, v + CELL):
                    # A dark rim marks the cell's edge, so a face's turn shows too.
                    edge = x == u or y == v
                    garment.putpixel((x, y), (*[c // 2 for c in colour], 255) if edge else (*colour, 255))
            faces[face] = {"uv": [u, v, u + CELL, v + CELL], "texture": 0}
        elements.append({"name": name, "type": "cube", "uuid": uuid, "from": list(lo), "to": list(hi), "faces": faces})
        folders.setdefault(piece, {}).setdefault(part, []).append(uuid)
    outliner = [{"name": piece, "children": [{"name": part, "children": ids} for part, ids in parts.items()]}
                for piece, parts in folders.items()]
    project = {"meta": {"format_version": "4.5", "model_format": "free"}, "resolution": {"width": GARMENT, "height": GARMENT},
               "elements": elements, "outliner": outliner,
               "textures": [{"name": "garment"}, {"name": "material", "uv_width": 16, "uv_height": 16}]}
    kits = ASSETS / NS / "survivalistarmor/kits"
    kits.mkdir(parents=True, exist_ok=True)
    (kits / f"{kit}.bbmodel").write_text(json.dumps(project, indent=1) + "\n")
    textures = ASSETS / NS / "textures/kit" / kit
    textures.mkdir(parents=True, exist_ok=True)
    garment.save(textures / "garment.png")
    if pbr:
        for image, suffix in zip(labpbr(garment.size), ("_n", "_s")):
            image.save(textures / f"garment{suffix}.png")
    for palette, colour in PALETTES.items():
        Image.new("RGBA", (16, 16), (*colour, 255)).save(textures / f"material_{palette}.png")
        if pbr:
            for image, suffix in zip(labpbr((16, 16)), ("_n", "_s")):
                image.save(textures / f"material_{palette}{suffix}.png")
    atlases = ASSETS / "minecraft/atlases"
    atlases.mkdir(parents=True, exist_ok=True)
    sprites = sorted({f"{NS}:kit/{k}/{n}" for k in (KIT, KIT + "_pbr") for n in ["garment"] + [f"material_{p}" for p in PALETTES]})
    (atlases / "blocks.json").write_text(json.dumps({"sources": [{"type": "single", "resource": s} for s in sprites]}, indent=2) + "\n")
    print(f"wrote {kit}: {len(CUBES)} cubes" + (", with loud LabPBR maps" if pbr else ""))


if __name__ == "__main__":
    main()
