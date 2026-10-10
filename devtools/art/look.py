"""look.py: a close look at a painted kit -- one view, scaled up, centred where you say -- to judge a joint by eye.

    uv run --no-project --with numpy --with pillow python devtools/art/look.py <kit> <yaw> <pitch> <x,y,z> [scale] [palette]

Renders from the kit as built (its project and textures under src/main/resources), not by repainting:
reads the faces back from the project, so what it shows is what the mod reads. Writes
build/art/<kit>-look-<yaw>-<pitch>.png.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from kitlib.geom import Cube, Face, Kit, corners, rotate  # noqa: E402
from kitlib.pack import Spot  # noqa: E402
from kitlib.preview import render  # noqa: E402

ROOT = HERE.parents[1]
ASSETS = ROOT / "src/main/resources/assets/survivalistarmor"


def load(kit_name: str, palette: str):
    proj = json.loads((ASSETS / "survivalistarmor/kits" / f"{kit_name}.bbmodel").read_text())
    garment = np.asarray(Image.open(ASSETS / "textures/kit" / kit_name / "garment.png").convert("RGBA")) / 255.0
    material = np.asarray(Image.open(ASSETS / "textures/kit" / kit_name / f"material_{palette}.png").convert("RGBA")) / 255.0
    faces, spots = [], {}
    for index, el in enumerate(proj["elements"]):
        cube = Cube(el["name"], tuple(el["from"]), tuple(el["to"]), "", "", tuple(el.get("rotation", (0, 0, 0))), tuple(el.get("origin", (0, 0, 0))))
        for d, f in el["faces"].items():
            if f.get("texture") is None:
                continue
            c = rotate(corners(cube.lo, cube.hi, d), np.array(cube.origin), cube.rotation)
            n = np.cross(c[1] - c[0], c[3] - c[0])
            if np.linalg.norm(n) < 1e-9:
                continue
            u1, v1, u2, v2 = f["uv"]
            spots[len(faces)] = Spot("material" if f["texture"] == 1 else "garment", int(u1), int(v1), int(u2 - u1), int(v2 - v1))
            faces.append(Face(cube, index, d, c, n / np.linalg.norm(n)))
    return faces, spots, garment, material


def main() -> None:
    kit, yaw, pitch, at = sys.argv[1], float(sys.argv[2]), float(sys.argv[3]), tuple(float(v) for v in sys.argv[4].split(","))
    scale = float(sys.argv[5]) if len(sys.argv) > 5 else 30.0
    palette = sys.argv[6] if len(sys.argv) > 6 else "iron"
    faces, spots, garment, material = load(kit, palette)
    img = render(faces, spots, garment, material, yaw, pitch, scale=scale, look=at, size=(900, 900))
    out = ROOT / "build/art" / f"{kit}-look-{int(yaw)}-{int(pitch)}-{palette}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((img * 255).astype(np.uint8)).save(out)
    print(out)


if __name__ == "__main__":
    main()
