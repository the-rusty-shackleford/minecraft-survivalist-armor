"""preview: draw a painted kit on a body, offscreen, from named views -- to judge it by eye between booth runs.

Orthographic, a z-buffer, each face's own texture sampled nearest from the sheets as packed, both
sides of every face drawn (worn armour is drawn without culling), lit from above and the front left
as the game's entity light falls. The body under the kit is drawn plain, so how the kit sits on it
shows. Views are yaw:pitch (yaw 0 looks at the figure's front from +z; 90 at its left side, from
+x; 180 its back; 270 its right side).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image

from .geom import BODY, Face, corners
from .pack import Spot

SKIN = np.array([0.55, 0.45, 0.38])
BACKGROUND = np.array([0.42, 0.47, 0.52])
LIGHT = np.array([0.45, 0.75, 0.48])


def view_matrix(yaw: float, pitch: float) -> np.ndarray:
    a, b = math.radians(yaw), math.radians(pitch)
    ry = np.array([[math.cos(a), 0, -math.sin(a)], [0, 1, 0], [math.sin(a), 0, math.cos(a)]])
    rx = np.array([[1, 0, 0], [0, math.cos(b), -math.sin(b)], [0, math.sin(b), math.cos(b)]])
    return rx @ ry


def render(faces: list[Face], spots: dict[int, Spot], garment: np.ndarray, material: np.ndarray, yaw: float, pitch: float,
           scale: float = 12.0, look: tuple[float, float, float] = (0.0, 16.0, 0.0), size: tuple[int, int] = (480, 560),
           body: bool = True) -> np.ndarray:
    """effects: returns an image (h, w, 3) of the kit's faces from (yaw, pitch), `scale` pixels to a model unit, centred on `look`."""
    W, H = size
    img = np.broadcast_to(BACKGROUND, (H, W, 3)).copy()
    depth = np.full((H, W), -np.inf)
    R = view_matrix(yaw, pitch)
    centre = np.array(look)
    light = R @ (LIGHT / np.linalg.norm(LIGHT))

    def draw(c: np.ndarray, normal: np.ndarray, tex: np.ndarray | None, colour: np.ndarray | None) -> None:
        v = (R @ (c - centre).T).T
        sx = v[:, 0] * scale + W / 2
        sy = -v[:, 1] * scale + H / 2
        sz = v[:, 2]
        tl, bl, tr = np.array([sx[0], sy[0]]), np.array([sx[1], sy[1]]), np.array([sx[3], sy[3]])
        m = np.array([tr - tl, bl - tl]).T
        det = np.linalg.det(m)
        if abs(det) < 1e-6:
            return
        inv = np.linalg.inv(m)
        x0, x1 = int(max(0, np.floor(sx.min()))), int(min(W - 1, np.ceil(sx.max())))
        y0, y1 = int(max(0, np.floor(sy.min()))), int(min(H - 1, np.ceil(sy.max())))
        if x0 > x1 or y0 > y1:
            return
        yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        p = np.stack([xx + 0.5 - tl[0], yy + 0.5 - tl[1]], axis=-1)
        st = p @ inv.T
        s, t = st[..., 0], st[..., 1]
        inside = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
        z = sz[0] + s * (sz[3] - sz[0]) + t * (sz[1] - sz[0])
        n = R @ normal
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(n if n[2] >= 0 else -n, light)))
        if tex is not None:
            th, tw = tex.shape[:2]
            ti = np.clip((t * th).astype(int), 0, th - 1)
            sj = np.clip((s * tw).astype(int), 0, tw - 1)
            texel = tex[ti, sj]
            inside &= texel[..., 3] > 0.5
            rgb = texel[..., :3] * shade
        else:
            rgb = np.broadcast_to(colour * shade, s.shape + (3,))
        win = inside & (z > depth[y0:y1 + 1, x0:x1 + 1])
        depth[y0:y1 + 1, x0:x1 + 1] = np.where(win, z, depth[y0:y1 + 1, x0:x1 + 1])
        img[y0:y1 + 1, x0:x1 + 1] = np.where(win[..., None], rgb, img[y0:y1 + 1, x0:x1 + 1])

    if body:
        for lo, hi in BODY.values():
            for d in ("north", "south", "east", "west", "up", "down"):
                c = corners(lo, hi, d)
                nrm = np.cross(c[1] - c[0], c[3] - c[0])
                draw(c, nrm / np.linalg.norm(nrm), None, SKIN)
    for i, f in enumerate(faces):
        spot = spots[i]
        sheet = material if spot.sheet == "material" else garment
        draw(f.corners, f.normal, sheet[spot.y:spot.y + spot.h, spot.x:spot.x + spot.w], None)
    return np.clip(img, 0, 1)


def sheet(images: list[np.ndarray], labels: list[str]) -> Image.Image:
    """effects: returns the images side by side, each labelled, as one picture."""
    from PIL import ImageDraw
    h = max(i.shape[0] for i in images)
    w = sum(i.shape[1] for i in images)
    out = Image.new("RGB", (w, h + 18), (30, 30, 30))
    x = 0
    draw = ImageDraw.Draw(out)
    for img, label in zip(images, labels):
        out.paste(Image.fromarray((img * 255).astype(np.uint8)), (x, 18))
        draw.text((x + 4, 3), label, fill=(220, 220, 220))
        x += img.shape[1]
    return out
