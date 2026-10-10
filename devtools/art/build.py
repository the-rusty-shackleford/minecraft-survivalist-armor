"""build.py: paint and write the kits (D-0002; the clean side of the wall).

    uv run --no-project --with numpy --with pillow python devtools/art/build.py [kit ...]

For each kit named (every kit in devtools/art/kits/ if none): its geometry, its faces culled as the
mod culls them, packed eight pixels to a unit, painted, and written into src/main/resources -- its
Blockbench project, its garment and one material sheet per palette, each with LabPBR _n and _s maps.
Then the block atlas's list of every kit's sprites, each kit template's icon (its signature piece, with
its maps), the scrap slot's ghost, and a preview sheet per kit in build/art/: the front, both sides,
the back and a three-quarter view on a body, in the armour the kit is designed toward first.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import importlib
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from kitlib.icons import kit_icon, scrap_ghost  # noqa: E402
from kitlib.pack import pack  # noqa: E402
from kitlib.paint import Painter  # noqa: E402
from kitlib.palettes import palettes  # noqa: E402
from kitlib.preview import render, sheet  # noqa: E402
from kitlib.style import DUST  # noqa: E402
from kitlib.write import Sheets, garment_sheet, material_sheet, png, write_atlas, write_kit  # noqa: E402

ROOT = HERE.parents[1]
RESOURCES = ROOT / "src/main/resources"
OUT = ROOT / "build/art"

VIEWS = [("front", 0, 8), ("left", 90, 8), ("back", 180, 8), ("right", 270, 8), ("three-quarter", 35, 18)]


def build(name: str) -> None:
    started = time.time()
    module = importlib.import_module(f"kits.{name}")
    kit = module.build()
    faces = kit.faces()
    spots, sizes = pack(faces)
    painter = Painter(kit, module.LOOKS, dust=module.DUST)
    sheets = Sheets(sizes)
    for i, f in enumerate(faces):
        sheets.put(spots[i], painter.paint(f))
    pals = palettes()
    write_kit(RESOURCES, kit, faces, spots, sizes, sheets, pals, DUST)
    garment, garment_s, _ = garment_sheet(sheets)
    material, material_s, _ = material_sheet(sheets, pals[getattr(module, "PALETTE", "iron")], DUST)
    icon, icon_n, icon_s = kit_icon(name, faces, spots, {"garment": garment, "material": material},
                                    {"garment": garment_s, "material": material_s})
    items = RESOURCES / "assets/survivalistarmor/textures/item"
    png(icon, items / f"{name}_kit.png")
    png(icon_n, items / f"{name}_kit_n.png")
    png(icon_s, items / f"{name}_kit_s.png")
    OUT.mkdir(parents=True, exist_ok=True)
    images = [render(faces, spots, garment, material, yaw, pitch, scale=12.0, size=(420, 520)) for _, yaw, pitch in VIEWS]
    sheet(images, [v[0] for v in VIEWS]).resize((sum(i.shape[1] for i in images) * 2 // 2, (520 + 18))).save(OUT / f"{name}-preview.png")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        print(f"  {piece:10s} {kit.count(piece):3d} cubes")
    print(f"{name}: {len(faces)} faces, garment {sizes['garment']}, material sheet {sizes['material']}, {time.time() - started:.1f} s")


def main() -> None:
    names = [a for a in sys.argv[1:] if not a.startswith("-")] or sorted(p.stem for p in (HERE / "kits").glob("*.py") if p.stem != "__init__")
    for name in names:
        build(name)
    sprites = []
    for d in sorted((RESOURCES / "assets/survivalistarmor/textures/kit").iterdir()):
        for f in sorted(d.glob("*.png")):
            if not f.stem.endswith(("_n", "_s")):
                sprites.append(f"survivalistarmor:kit/{d.name}/{f.stem}")
    write_atlas(RESOURCES, sprites)
    print(f"atlas: {len(sprites)} sprites")
    png(scrap_ghost(), RESOURCES / "assets/survivalistarmor/textures/item/empty_slot_scrap.png")


if __name__ == "__main__":
    main()
