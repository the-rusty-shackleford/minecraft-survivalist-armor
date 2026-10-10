"""geom: a kit as cubes on a body, and the faces the game draws of them.

The frame is the kit project's, as the mod reads it (domain/KitModel, domain/RestPose): Blockbench
pixels (sixteen to a block), the figure standing on y 0 facing +z (south), its right arm toward -x,
its neck at y 24. A cube rides on a piece's part; its rotation, if any, is about its origin, X then Y
then Z. A face's corners run as the mod's parser runs them -- top-left, bottom-left, bottom-right,
top-right seen from outside -- and its texture rectangle lies on them the same way, top-left at the
rectangle's top-left. A face another unturned cube on the same piece and part covers entirely is
culled exactly as the mod culls it (domain/KitMesh.covered), and written without a texture so the
game never draws a face nobody painted.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

PIECES = {
    "helmet": ("head",),
    "chestplate": ("body", "right_arm", "left_arm"),
    "leggings": ("body", "right_leg", "left_leg"),
    "boots": ("right_leg", "left_leg"),
}
DIRECTIONS = ("north", "south", "east", "west", "up", "down")

# The game's body at rest, in the project's frame: (min, max).
BODY = {
    "head": ((-4.0, 24.0, -4.0), (4.0, 32.0, 4.0)),
    "body": ((-4.0, 12.0, -2.0), (4.0, 24.0, 2.0)),
    "right_arm": ((-8.0, 12.0, -2.0), (-4.0, 24.0, 2.0)),
    "left_arm": ((4.0, 12.0, -2.0), (8.0, 24.0, 2.0)),
    "right_leg": ((-3.9, 0.0, -2.0), (0.1, 12.0, 2.0)),
    "left_leg": ((-0.1, 0.0, -2.0), (3.9, 12.0, 2.0)),
}

PX = 8  # texture pixels to a model unit: 128 to a block, Pixlli's density (nfx's §7)
REACH = 0.01
SEE_THROUGH = {"mesh"}  # chain-link: what is behind it shows through its holes, so it hides nothing (Materials.java)
SLACK = 1e-6


def rotate(p: np.ndarray, origin: np.ndarray, degrees: tuple[float, float, float]) -> np.ndarray:
    """effects: returns points p (..., 3) turned about origin by degrees about X, then Y, then Z (Blockbench's order)."""
    if not any(degrees):
        return p
    q = p - origin
    a, b, g = (math.radians(d) for d in degrees)
    x, y, z = q[..., 0], q[..., 1], q[..., 2]
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    x, y = x * math.cos(g) - y * math.sin(g), x * math.sin(g) + y * math.cos(g)
    return np.stack([x, y, z], axis=-1) + origin


@dataclass
class Cube:
    """A cube of a kit: its name (material first: "plate.chest"), its corners, where it rides, how it is turned.

    `base` puts its faces on the material sheet, coloured by the armour's own material (the
    salvaged plate, the busbar, the mesh); otherwise they are the kit's garment. `paint` holds the
    cube's decals and options for the painter; `drop` names faces nobody sees (a helmet's floor).
    """
    name: str
    lo: tuple[float, float, float]
    hi: tuple[float, float, float]
    piece: str
    part: str
    rotation: tuple[float, float, float] = (0.0, 0.0, 0.0)
    origin: tuple[float, float, float] | None = None
    base: bool = False
    paint: dict = field(default_factory=dict)
    drop: tuple[str, ...] = ()

    @property
    def material(self) -> str:
        return self.name.split(".", 1)[0].strip().lower()

    @property
    def turned(self) -> bool:
        return any(self.rotation)

    def pivot(self) -> np.ndarray:
        if self.origin is not None:
            return np.array(self.origin, dtype=float)
        return (np.array(self.lo, dtype=float) + np.array(self.hi, dtype=float)) / 2

    def contains(self, p: np.ndarray, slack: float) -> bool:
        lo, hi = np.array(self.lo), np.array(self.hi)
        return bool(np.all(p >= lo - slack) and np.all(p <= hi + slack))


@dataclass
class Face:
    """A face the game draws: its cube, direction, corners (top-left, bottom-left, bottom-right, top-right), outward normal."""
    cube: Cube
    index: int
    direction: str
    corners: np.ndarray  # (4, 3)
    normal: np.ndarray

    @property
    def width(self) -> float:
        return float(np.linalg.norm(self.corners[3] - self.corners[0]))

    @property
    def height(self) -> float:
        return float(np.linalg.norm(self.corners[1] - self.corners[0]))

    def pixels(self) -> tuple[int, int]:
        """effects: returns the face's texture size: eight pixels to a unit, at least one."""
        return max(1, round(self.width * PX)), max(1, round(self.height * PX))

    def points(self, w: int, h: int) -> np.ndarray:
        """effects: returns the 3D point at the middle of each of the face's w x h texture pixels, (h, w, 3)."""
        tl, bl, tr = self.corners[0], self.corners[1], self.corners[3]
        u = (np.arange(w) + 0.5) / w
        v = (np.arange(h) + 0.5) / h
        return tl + (tr - tl) * u[None, :, None] + (bl - tl) * v[:, None, None]


def corners(lo, hi, direction: str) -> np.ndarray:
    """effects: returns a box's face corners, top-left, bottom-left, bottom-right, top-right seen from outside (the mod's order)."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    c = {  # top-left, top-right, bottom-right, bottom-left
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[direction]
    return np.array([c[0], c[3], c[2], c[1]], dtype=float)


class Kit:
    """A kit's cubes, piece by piece."""

    def __init__(self, name: str):
        self.name = name
        self.cubes: list[Cube] = []

    def add(self, piece: str, part: str, name: str, lo, hi, **kw) -> Cube:
        if part not in PIECES[piece]:
            raise ValueError(f"{piece} does not ride on {part}")
        lo, hi = tuple(map(float, lo)), tuple(map(float, hi))
        lo, hi = tuple(min(a, b) for a, b in zip(lo, hi)), tuple(max(a, b) for a, b in zip(lo, hi))
        cube = Cube(name, lo, hi, piece, part, **kw)
        self.cubes.append(cube)
        return cube

    def faces(self) -> list[Face]:
        """effects: returns every face the game draws, covered ones left out as the mod leaves them out."""
        out: list[Face] = []
        for i, cube in enumerate(self.cubes):
            for d in DIRECTIONS:
                if d in cube.drop:
                    continue
                c = rotate(corners(cube.lo, cube.hi, d), cube.pivot(), cube.rotation)
                n = np.cross(c[1] - c[0], c[3] - c[0])
                if np.linalg.norm(n) < 1e-9:
                    continue
                f = Face(cube, i, d, c, n / np.linalg.norm(n))
                if not self.covered(f):
                    out.append(f)
        return out

    def covered(self, f: Face) -> bool:
        if f.cube.turned:
            return False
        out = f.normal * REACH
        for g in self.cubes:
            if g is f.cube or g.turned or g.piece != f.cube.piece or g.part != f.cube.part or g.material in SEE_THROUGH:
                continue
            if all(g.contains(c + out, SLACK) for c in f.corners):
                return True
        return False

    def count(self, piece: str) -> int:
        return sum(1 for c in self.cubes if c.piece == piece)
