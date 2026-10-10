"""checks: what a kit must be, measured on its cubes: how much of each cube shows, where it reaches.

The kit tests (devtools/art/tests) hold every kit to these; they live here so a kit's author can run
them on a kit in progress (`visibility(kit)` lists what is buried).

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import math

import numpy as np

from .geom import DIRECTIONS, REACH, SEE_THROUGH, Cube, Kit, corners, rotate


def unrotate(p: np.ndarray, origin: np.ndarray, degrees: tuple[float, float, float]) -> np.ndarray:
    """effects: returns points p (..., 3) turned back by what `rotate` turns them by: Z, then Y, then X, each undone."""
    q = p - origin
    a, b, g = (math.radians(d) for d in degrees)
    x, y, z = q[..., 0], q[..., 1], q[..., 2]
    x, y = x * math.cos(g) + y * math.sin(g), -x * math.sin(g) + y * math.cos(g)
    x, z = x * math.cos(b) - z * math.sin(b), x * math.sin(b) + z * math.cos(b)
    y, z = y * math.cos(a) + z * math.sin(a), -y * math.sin(a) + z * math.cos(a)
    return np.stack([x, y, z], axis=-1) + origin


def world_corners(cube: Cube) -> np.ndarray:
    """effects: returns the cube's eight corners where it stands, turned."""
    (x0, y0, z0), (x1, y1, z1) = cube.lo, cube.hi
    pts = np.array([(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)], dtype=float)
    return rotate(pts, cube.pivot(), cube.rotation) if cube.turned else pts


def inside(cube: Cube, p: np.ndarray) -> np.ndarray:
    """effects: returns, for points p (n, 3), whether each lies inside the cube as it stands."""
    if cube.turned:
        p = unrotate(p, cube.pivot(), cube.rotation)
    lo, hi = np.array(cube.lo), np.array(cube.hi)
    return np.all((p > lo) & (p < hi), axis=-1)


def visibility(kit: Kit, samples: int = 6) -> dict[int, float]:
    """effects: returns, for each cube (by its index), how much its best-shown face shows: the share of that face no other
    cube of its piece and part holds, sampled on a grid a hair outside it. Chain-link holds nothing. A strap's ring faces
    lie inside the sleeve it rounds, as they should, so a cube is judged by its best face, not by area."""
    out = {}
    t = (np.arange(samples) + 0.5) / samples
    u, v = np.meshgrid(t, t)
    for i, cube in enumerate(kit.cubes):
        others = [g for g in kit.cubes if g is not cube and g.piece == cube.piece and g.part == cube.part and g.material not in SEE_THROUGH]
        best = 0.0
        for d in DIRECTIONS:
            if d in cube.drop:
                continue
            c = corners(cube.lo, cube.hi, d)
            if cube.turned:
                c = rotate(c, cube.pivot(), cube.rotation)
            n = np.cross(c[1] - c[0], c[3] - c[0])
            if np.linalg.norm(n) < 1e-9:
                continue
            n = n / np.linalg.norm(n)
            # corners: top-left, bottom-left, bottom-right, top-right
            p = (c[0][None, None] + (c[3] - c[0])[None, None] * u[..., None] + (c[1] - c[0])[None, None] * v[..., None]).reshape(-1, 3)
            p = p + n * REACH * 2
            hidden = np.zeros(len(p), bool)
            for g in others:
                hidden |= inside(g, p)
            best = max(best, 1.0 - float(hidden.mean()))
        out[i] = best
    return out
