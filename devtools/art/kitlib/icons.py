"""icons: each kit's template as an item -- its signature piece drawn from its own textures, 128 pixels square
with its LabPBR maps, so the seven are told apart at a glance (the plan's "Kit template icons") -- and the
smithing screen's ghost for the scrap slot, drawn as the game draws its own: a 16-pixel silhouette in one grey.

The piece is rasterised once, orthographic from a three-quarter view, and every map is sampled through
the same raster: the albedo shaded as the GUI's flat sprites are (light from above and the front left,
baked in), _s straight from the kit's own _s (what the faces are made of), _n the faces' facing in the
picture's own frame (x right, y up), occlusion full, height from nearness. A darker rim outlines the
silhouette as the game's item sprites are outlined.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import numpy as np

from .geom import Face
from .pack import Spot
from .preview import LIGHT, view_matrix

SIZE = 128
MARGIN = 6

# Which piece shows each kit best, and from where (yaw 0 is the front; positive turns toward the figure's left).
SIGNATURE = {
    "roadplate": ("chestplate", 22.0, 10.0),   # the sign
    "magpie": ("helmet", 30.0, 14.0),          # the hard hat, its busbar crest
    "bluecoat": ("helmet", 30.0, 12.0),        # the sealed helmet, its lit visor
    "blacklung": ("helmet", 22.0, 10.0),       # the mask, its orange eyes
    "hazmat": ("helmet", 38.0, 12.0),          # the boxy hood, its side filter
    "duster": ("helmet", 24.0, 10.0),          # the hood, the goggles in its shadow
    "scrapper": ("chestplate", 24.0, 10.0),    # the chain-link
}

# The scrap slot's ghost: a cut plate, two bolt holes along its top, torn away below (vanilla's ghosts are one grey, #555555, on nothing).
SCRAP_GHOST = [
    "................",
    "................",
    "..############..",
    "..############..",
    "..##.######.##..",
    "..############..",
    "..############..",
    "..###########...",
    "..##########....",
    "..#########.#...",
    "...#######.##...",
    "...######.......",
    "....####........",
    ".....##.........",
    "................",
    "................",
]


def raster(faces: list[Face], yaw: float, pitch: float) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """effects: returns, per pixel of a SIZE-square picture of the faces fitted into it: the face's index (-1 none), its
    texture's s and t (0-1 across and down the face), the face's normal in the picture's frame, and nearness."""
    R = view_matrix(yaw, pitch)
    pts = np.concatenate([f.corners for f in faces])
    v = (R @ pts.T).T
    lo, hi = v.min(axis=0), v.max(axis=0)
    scale = (SIZE - 2 * MARGIN) / max(hi[0] - lo[0], hi[1] - lo[1])
    mid = (lo + hi) / 2
    index = np.full((SIZE, SIZE), -1)
    s_at = np.zeros((SIZE, SIZE))
    t_at = np.zeros((SIZE, SIZE))
    normal = np.zeros((SIZE, SIZE, 3))
    depth = np.full((SIZE, SIZE), -np.inf)
    for i, f in enumerate(faces):
        c = (R @ f.corners.T).T
        sx = (c[:, 0] - mid[0]) * scale + SIZE / 2
        sy = -(c[:, 1] - mid[1]) * scale + SIZE / 2
        sz = c[:, 2]
        n = R @ f.normal
        if n[2] <= 0:
            continue  # facing away: an icon shows the outside only
        tl, bl, tr = np.array([sx[0], sy[0]]), np.array([sx[1], sy[1]]), np.array([sx[3], sy[3]])
        m = np.array([tr - tl, bl - tl]).T
        if abs(np.linalg.det(m)) < 1e-6:
            continue
        inv = np.linalg.inv(m)
        x0, x1 = int(max(0, np.floor(sx.min()))), int(min(SIZE - 1, np.ceil(sx.max())))
        y0, y1 = int(max(0, np.floor(sy.min()))), int(min(SIZE - 1, np.ceil(sy.max())))
        if x0 > x1 or y0 > y1:
            continue
        yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        st = np.stack([xx + 0.5 - tl[0], yy + 0.5 - tl[1]], axis=-1) @ inv.T
        s, t = st[..., 0], st[..., 1]
        inside = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
        z = sz[0] + s * (sz[3] - sz[0]) + t * (sz[1] - sz[0])
        win = inside & (z > depth[y0:y1 + 1, x0:x1 + 1])
        sl = (slice(y0, y1 + 1), slice(x0, x1 + 1))
        depth[sl] = np.where(win, z, depth[sl])
        index[sl] = np.where(win, i, index[sl])
        s_at[sl] = np.where(win, s, s_at[sl])
        t_at[sl] = np.where(win, t, t_at[sl])
        normal[sl] = np.where(win[..., None], n, normal[sl])
    return index, s_at, t_at, normal, depth


def sample(index, s_at, t_at, faces, spots: dict[int, Spot], sheets: dict[str, np.ndarray], order: list[int]) -> np.ndarray:
    """effects: returns, per pixel, the texel of its face's texture on the named sheet (garment or material); 0 where none."""
    some = next(iter(sheets.values()))
    out = np.zeros((SIZE, SIZE, some.shape[2]))
    for y, x in zip(*np.nonzero(index >= 0)):
        k = order[index[y, x]]
        spot = spots[k]
        tex = sheets[spot.sheet]
        ti = spot.y + min(spot.h - 1, int(t_at[y, x] * spot.h))
        sj = spot.x + min(spot.w - 1, int(s_at[y, x] * spot.w))
        out[y, x] = tex[ti, sj]
    return out


def kit_icon(name: str, faces: list[Face], spots: dict[int, Spot], albedo: dict[str, np.ndarray], spec: dict[str, np.ndarray]) \
        -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """effects: returns the kit template's icon (SIZE, SIZE, 4) floats 0-1, its _n and _s (uint8): `albedo` and `spec` hold
    the garment's and the kit's natural material sheet's colour and _s, by sheet name."""
    piece, yaw, pitch = SIGNATURE[name]
    order = [i for i, f in enumerate(faces) if f.cube.piece == piece]
    mine = [faces[i] for i in order]
    index, s_at, t_at, normal, depth = raster(mine, yaw, pitch)
    seen = index >= 0
    colour = sample(index, s_at, t_at, faces, spots, albedo, order)
    light = view_matrix(yaw, pitch) @ (LIGHT / np.linalg.norm(LIGHT))
    shade = 0.62 + 0.42 * np.clip(normal @ light, 0, 1)
    rgb = colour[..., :3] * shade[..., None]
    seen &= colour[..., 3] > 0.5  # chain-link's holes stay holes
    # The rim: silhouette pixels with an empty neighbour are darkened, as the game's sprites are outlined.
    pad = np.pad(seen, 1)
    edge = seen & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    rgb = np.where(edge[..., None], rgb * 0.6, rgb)
    out = np.dstack([np.clip(rgb, 0, 1), seen.astype(float)])
    s = sample(index, s_at, t_at, faces, spots, spec, order).astype(np.uint8)
    s[~seen] = (0, 0, 0, 255)
    n = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    n[..., 0] = np.clip((normal[..., 0] * 0.5 + 0.5) * 255, 0, 255).astype(np.uint8)
    n[..., 1] = np.clip((normal[..., 1] * 0.5 + 0.5) * 255, 0, 255).astype(np.uint8)
    n[..., 2] = 255
    near = np.where(seen, depth, np.nan)
    if seen.any():
        lo, hi = np.nanmin(near), np.nanmax(near)
        h = (near - lo) / max(hi - lo, 1e-6)
        n[..., 3] = np.where(seen, np.clip((0.8 + 0.2 * np.nan_to_num(h)) * 255, 1, 255), 255).astype(np.uint8)
    n[~seen, 0:2] = 128
    return out, n, s


def scrap_ghost() -> np.ndarray:
    """effects: returns the scrap slot's ghost icon (16, 16, 4) uint8."""
    out = np.zeros((16, 16, 4), dtype=np.uint8)
    for y, row in enumerate(SCRAP_GHOST):
        for x, ch in enumerate(row):
            if ch == "#":
                out[y, x] = (85, 85, 85, 255)
    return out
