"""measure.py: the measuring side of the clean-room wall (D-0002). Reads Pixlli; writes numbers.

    uv run --no-project --with numpy --with pillow python devtools/art/measure.py [pixlli.zip]

Reads Rusty's own copy of Pixlli 128x (personal use; by default the one in his Prism instance) and
writes devtools/art/pixlli-style.json: for each material class, scalars and a palette of at most
eight colours -- no image, crop, trace or downscale. The painter (devtools/art/kitlib) never imports
this file and never opens Pixlli; it reads only the numbers, and a test holds it to that.

Per texture, over its opaque pixels:
- value: luma percentiles 5, 25, 50, 75, 95 (sRGB luma, 0 to 1);
- hue: the circular mean hue in degrees, and saturation's median and 90th percentile (HSV);
- palette: up to eight k-means centres, darkest first, as hex;
- grain: the mean luma step to the next pixel, and to the pixel 4 and 16 away (how fine the variation is);
- rim: how much darker the outer twelfth of a block's face is than its middle (Pixlli's outlining);
- from the _s map: smoothness median and 90th percentile (0 to 1), F0 as LabPBR stores it (its most
  common value, 0-255), porosity median (0-64 scale);
- from the _n map: the mean tilt of the normal in degrees, the median ambient occlusion (0 to 1), and
  the 5th percentile of height (0 to 1).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import colorsys
import io
import json
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
OUT = HERE / "pixlli-style.json"
DEFAULT = Path.home() / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/Minecraft/minecraft/resourcepacks/Pixlli V57 26.3-1.13 128x.zip"
T = "assets/minecraft/textures/"

# class name -> texture path under textures/
CLASSES = {
    # What the kits must sit among, judged beside in game (nfx's §9).
    "stone": "block/stone", "oak_planks": "block/oak_planks", "smooth_stone": "block/smooth_stone",
    "cobblestone": "block/cobblestone", "dirt": "block/dirt", "coarse_dirt": "block/coarse_dirt",
    # Materials the kits are made of.
    "iron_block": "block/iron_block", "gold_block": "block/gold_block", "diamond_block": "block/diamond_block",
    "netherite_block": "block/netherite_block", "copper_block": "block/copper_block",
    "oxidized_copper": "block/oxidized_copper", "white_wool": "block/white_wool", "brown_wool": "block/brown_wool",
    "chain": "block/chain",
    # The game's armour, as Pixlli draws each material.
    "armor_iron": "models/armor/iron_layer_1", "armor_gold": "models/armor/gold_layer_1",
    "armor_diamond": "models/armor/diamond_layer_1", "armor_netherite": "models/armor/netherite_layer_1",
    "armor_chainmail": "models/armor/chainmail_layer_1", "armor_leather": "models/armor/leather_layer_1",
    "armor_turtle": "models/armor/turtle_layer_1",
}


def load(z: zipfile.ZipFile, path: str) -> np.ndarray | None:
    try:
        return np.asarray(Image.open(io.BytesIO(z.read(T + path + ".png"))).convert("RGBA"), dtype=np.float64) / 255.0
    except KeyError:
        return None


def luma(rgb: np.ndarray) -> np.ndarray:
    return rgb[..., 0] * 0.2126 + rgb[..., 1] * 0.7152 + rgb[..., 2] * 0.0722


def kmeans(px: np.ndarray, k: int, rng: np.random.Generator) -> np.ndarray:
    if len(px) <= k:
        return px
    centres = px[rng.choice(len(px), k, replace=False)]
    for _ in range(25):
        d = ((px[:, None, :] - centres[None, :, :]) ** 2).sum(-1)
        nearest = d.argmin(1)
        for j in range(k):
            members = px[nearest == j]
            if len(members):
                centres[j] = members.mean(0)
    return centres


def measure(img: np.ndarray, s: np.ndarray | None, n: np.ndarray | None, rng: np.random.Generator) -> dict:
    opaque = img[..., 3] > 0.5
    rgb = img[..., :3]
    y = luma(rgb)
    px = rgb[opaque]
    hsv = np.array([colorsys.rgb_to_hsv(*p) for p in px[rng.choice(len(px), min(len(px), 4000), replace=False)]])
    angle = hsv[:, 0] * 2 * np.pi
    hue = (np.degrees(np.arctan2(np.sin(angle).mean(), np.cos(angle).mean())) + 360) % 360
    sample = px[rng.choice(len(px), min(len(px), 6000), replace=False)]
    centres = kmeans(sample, 8, rng)
    centres = centres[np.argsort(luma(centres))]
    h, w = y.shape

    def step(d: int) -> float:
        a = np.abs(y[:, d:] - y[:, :-d])[opaque[:, d:] & opaque[:, :-d]]
        return float(a.mean()) if a.size else 0.0

    rim = max(1, min(h, w) // 12)
    edge = np.zeros_like(opaque)
    edge[:rim, :] = edge[-rim:, :] = edge[:, :rim] = edge[:, -rim:] = True
    out = {
        "value": [round(float(np.percentile(y[opaque], q)), 3) for q in (5, 25, 50, 75, 95)],
        "hue": round(float(hue), 1),
        "saturation": [round(float(np.median(hsv[:, 1])), 3), round(float(np.percentile(hsv[:, 1], 90)), 3)],
        "palette": ["#%02x%02x%02x" % tuple(int(round(c * 255)) for c in centre) for centre in centres],
        "grain": [round(step(1), 4), round(step(4), 4), round(step(16), 4)],
        "rim": round(float(y[opaque & ~edge].mean() - y[opaque & edge].mean()), 4) if (opaque & edge).any() and (opaque & ~edge).any() else 0.0,
    }
    if s is not None and s.shape[:2] == img.shape[:2]:
        sm = s[..., 0][opaque]
        f0 = np.round(s[..., 1][opaque] * 255).astype(int)
        out["smoothness"] = [round(float(np.median(sm)), 3), round(float(np.percentile(sm, 90)), 3)]
        out["f0"] = int(np.bincount(f0).argmax())
        po = s[..., 2][opaque] * 255
        out["porosity"] = round(float(np.median(po[po <= 64])) if (po <= 64).any() else 0.0, 1)
    if n is not None and n.shape[:2] == img.shape[:2]:
        nx, ny = n[..., 0] * 2 - 1, n[..., 1] * 2 - 1
        tilt = np.degrees(np.arcsin(np.clip(np.sqrt(nx ** 2 + ny ** 2), 0, 1)))
        out["tilt"] = round(float(tilt[opaque].mean()), 2)
        out["occlusion"] = round(float(np.median(n[..., 2][opaque])), 3)
        out["height_p5"] = round(float(np.percentile(n[..., 3][opaque], 5)), 3)
    return out


def main() -> None:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT
    rng = np.random.default_rng(7)
    style = {"source": "Pixlli 128x, measured (numbers only; D-0002)", "classes": {}}
    with zipfile.ZipFile(src) as z:
        for name, path in CLASSES.items():
            img = load(z, path)
            if img is None:
                print("no", path)
                continue
            style["classes"][name] = measure(img, load(z, path + "_s"), load(z, path + "_n"), rng)
            print(f"{name:16s} value {style['classes'][name]['value']} hue {style['classes'][name]['hue']}")
    OUT.write_text(json.dumps(style, indent=1) + "\n")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
