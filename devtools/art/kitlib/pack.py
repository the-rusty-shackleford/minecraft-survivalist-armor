"""pack: lay a kit's faces out on its two textures, eight pixels to a model unit (nfx's §7: 128x).

Garment faces on the garment canvas, material faces on the material sheet; shelves, tallest first,
two pixels of padding round each so filtering at a distance takes a face's own colour, never a
neighbour's. Each sheet's width is fixed, its height the next multiple of sixteen that holds it: what
four mipmap levels need, and no more (a power of two doubled Duster's garment). The kits' own atlas
(D-0008) stitches without mipmaps, so the rounding costs a few rows a sheet there and nothing else.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

from dataclasses import dataclass

from .geom import Face

PAD = 2


@dataclass(frozen=True)
class Spot:
    """Where a face's texture lies: which sheet, and its rectangle in pixels."""
    sheet: str
    x: int
    y: int
    w: int
    h: int


def pack(faces: list[Face], garment_width: int = 1024, material_width: int = 512) -> tuple[dict[int, Spot], dict[str, tuple[int, int]]]:
    """effects: returns each face's spot (by its position in `faces`) and each sheet's size."""
    spots: dict[int, Spot] = {}
    sizes: dict[str, tuple[int, int]] = {}
    for sheet, width in (("garment", garment_width), ("material", material_width)):
        mine = [(i, f) for i, f in enumerate(faces) if (sheet == "material") == f.cube.base]
        mine.sort(key=lambda p: (-p[1].pixels()[1], -p[1].pixels()[0]))
        x = y = shelf = 0
        for i, f in mine:
            w, h = f.pixels()
            if w + 2 * PAD > width:
                raise ValueError(f"{f.cube.name} {f.direction} is {w} pixels wide, wider than its sheet")
            if x + w + 2 * PAD > width:
                x, y, shelf = 0, y + shelf, 0
            spots[i] = Spot(sheet, x + PAD, y + PAD, w, h)
            x += w + 2 * PAD
            shelf = max(shelf, h + 2 * PAD)
        used = y + shelf
        sizes[sheet] = (width, max(16, -(-used // 16) * 16))
    return spots, sizes
