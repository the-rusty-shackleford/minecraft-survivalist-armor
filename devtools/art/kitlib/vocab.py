"""vocab: the shared vocabulary's shapes (nfx's §5 and §8.1), as cubes on a kit.

What ties seven different builds into one set is vocabulary, not geometry: each kit has a respirator,
lenses, a bandolier from right shoulder to left hip with its cases, pouches that hang off the
surface, a filter canister and a hose, straps crossing panel seams, one stencil, the dust. These
helpers make those shapes as cubes at the depths §8.1 gives -- straps 0.5-0.75 off the surface,
pouches and cases 1-2, plate edges 0.5-1, hoses 1-1.5 -- in the kit project's frame (geom.py).

The interfaces are the same in every kit, so pieces mixed across kits meet without clipping (§8):
a chest piece's hem stops at y 12.5, above the leggings' belt; leggings rise no higher than y 14 and,
above the hem, stay within 4.75 of the body's centre side to side and 2.85 front and back, inside any
chest piece; boots stop at y 6.6, where knee pads and coat skirts begin, and below that the trouser
legs stay within 2.6 of the leg's centre and the boots stand outside 2.9; a helmet's hood sits round
the collar.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import math

from .geom import BODY, Cube, Kit

# The interfaces every kit keeps (tests/test_kits.py holds them).
CHEST_HEM = 12.5        # a chest piece reaches no lower on the body than this
LEGGINGS_TOP = 14.0     # leggings reach no higher
LEGGINGS_WIDTH = 4.75   # above the hem, leggings reach no further than this either side of the body's centre,
LEGGINGS_DEPTH = 2.85   # nor further than this before or behind it: inside any chest piece
CHEST_INNER = 2.9       # a chest piece's innermost layer stands at least this far before and behind
BOOT_TOP = 6.6          # boots reach no higher on the leg; knee pads and skirts start here
TROUSER_OUTSIDE = 2.6   # below BOOT_TOP, trouser legs reach no further out from the leg's centre
BOOT_INSIDE = 2.9       # a boot's shaft stands at least this far out


def grown(part: str, g: float, y0: float | None = None, y1: float | None = None) -> tuple[tuple, tuple]:
    """effects: returns the body part's box grown by g on every side, cut to y0..y1 if given."""
    (x0, a0, z0), (x1, a1, z1) = BODY[part]
    lo = (x0 - g, (a0 - g) if y0 is None else y0, z0 - g)
    hi = (x1 + g, (a1 + g) if y1 is None else y1, z1 + g)
    return lo, hi


def shell(kit: Kit, piece: str, part: str, name: str, g: float, y0=None, y1=None, **kw) -> Cube:
    """effects: adds a layer round the whole body part, g out from it (a hood, an undersuit, a sleeve, a trouser leg)."""
    lo, hi = grown(part, g, y0, y1)
    return kit.add(piece, part, name, lo, hi, **kw)


def band(kit: Kit, piece: str, part: str, name: str, g: float, y0: float, y1: float, **kw) -> Cube:
    """effects: adds a band round the body part between heights y0 and y1, g out from it (a cuff, a collar, a strap ring, tape)."""
    return shell(kit, piece, part, name, g, y0, y1, **kw)


def along(kit: Kit, piece: str, part: str, name: str, start: tuple[float, float], end: tuple[float, float], z0: float, z1: float,
          width: float, **kw) -> Cube:
    """effects: adds a strap from start to end (x, y) on the body's front or back, its faces z0..z1, turned
    about z to run between them (the bandolier, a sling)."""
    (sx, sy), (ex, ey) = start, end
    cx, cy = (sx + ex) / 2, (sy + ey) / 2
    length = math.hypot(ex - sx, ey - sy)
    angle = math.degrees(math.atan2(ey - sy, ex - sx))
    return kit.add(piece, part, name, (cx - length / 2, cy - width / 2, z0), (cx + length / 2, cy + width / 2, z1),
                   rotation=(0.0, 0.0, angle), origin=(cx, cy, (z0 + z1) / 2), **kw)


def cases(kit: Kit, piece: str, part: str, name: str, start: tuple[float, float], end: tuple[float, float], z0: float,
          depth: float, at: list[float], size: tuple[float, float], **kw) -> list[Cube]:
    """effects: adds a case at each fraction `at` of the way from start to end, standing `depth` off z0, turned with the
    strap; clustered, never evenly spaced (nfx's §7)."""
    (sx, sy), (ex, ey) = start, end
    angle = math.degrees(math.atan2(ey - sy, ex - sx))
    out = []
    for i, t in enumerate(at):
        cx, cy = sx + (ex - sx) * t, sy + (ey - sy) * t
        w, h = size
        out.append(kit.add(piece, part, f"{name}{i}", (cx - w / 2, cy - h / 2, z0), (cx + w / 2, cy + h / 2, z0 + depth),
                           rotation=(0.0, 0.0, angle), origin=(cx, cy, z0 + depth / 2), **kw))
    return out


def pouch(kit: Kit, piece: str, part: str, name: str, lo, hi, face: str = "south", **kw) -> list[Cube]:
    """effects: adds a pouch from lo to hi and its flap over the top of the face it opens on (south: front)."""
    body = kit.add(piece, part, name, lo, hi, **kw)
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    flap_h = min(1.2, (y1 - y0) * 0.4)
    if face == "south":
        flap = kit.add(piece, part, name + "_flap", (x0 - 0.15, y1 - flap_h, z1), (x1 + 0.15, y1 + 0.2, z1 + 0.35), **kw)
    elif face == "north":
        flap = kit.add(piece, part, name + "_flap", (x0 - 0.15, y1 - flap_h, z0 - 0.35), (x1 + 0.15, y1 + 0.2, z0), **kw)
    elif face == "east":
        flap = kit.add(piece, part, name + "_flap", (x1, y1 - flap_h, z0 - 0.15), (x1 + 0.35, y1 + 0.2, z1 + 0.15), **kw)
    else:
        flap = kit.add(piece, part, name + "_flap", (x0 - 0.35, y1 - flap_h, z0 - 0.15), (x0, y1 + 0.2, z1 + 0.15), **kw)
    return [body, flap]


def cylinder(kit: Kit, piece: str, part: str, name: str, centre: tuple[float, float, float], radius: float, length: float, axis: str,
             **kw) -> list[Cube]:
    """effects: adds a round thing -- a canister, a filter, a tank, a rolled bedroll -- as two crossed boxes along `axis`
    (x, y or z), stepped so its section reads round without a sub-pixel chamfer anywhere (§8: keep it blocky)."""
    cx, cy, cz = centre
    half = length / 2
    inner = radius * 0.62
    spans = {"x": ((cx - half, cx + half), None), "y": ((cy - half, cy + half), None), "z": ((cz - half, cz + half), None)}
    out = []
    for i, (a, b) in enumerate(((radius, inner), (inner, radius))):
        # The second box's ends stand a hair inside the first's, so their end caps never share a plane and flicker.
        half = length / 2 - (0.01 if i else 0.0)
        if axis == "x":
            lo, hi = (cx - half, cy - a, cz - b), (cx + half, cy + a, cz + b)
        elif axis == "y":
            lo, hi = (cx - a, cy - half, cz - b), (cx + a, cy + half, cz + b)
        else:
            lo, hi = (cx - a, cy - b, cz - half), (cx + a, cy + b, cz + half)
        out.append(kit.add(piece, part, f"{name}" if i == 0 else f"{name}_x", lo, hi, **kw))
    return out


def ring(kit: Kit, piece: str, part: str, name: str, centre: tuple[float, float, float], radius: float, thickness: float, axis: str,
         **kw) -> list[Cube]:
    """effects: adds a coil of hose or cable as a square ring of four bars round `axis` (x, y or z) through `centre`,
    `radius` out to its outer edge, its bars `thickness` thick (nfx's §6: Hazmat's coiled hoses)."""
    c = list(centre)
    i = "xyz".index(axis)
    a, b = [j for j in range(3) if j != i]  # the ring's plane
    out = []
    t = thickness
    bars = (("top", (c[a] - radius, c[b] + radius - t), (c[a] + radius, c[b] + radius)),
            ("bottom", (c[a] - radius, c[b] - radius), (c[a] + radius, c[b] - radius + t)),
            ("near", (c[a] - radius, c[b] - radius + t), (c[a] - radius + t, c[b] + radius - t)),
            ("far", (c[a] + radius - t, c[b] - radius + t), (c[a] + radius, c[b] + radius - t)))
    for label, (a0, b0), (a1, b1) in bars:
        lo, hi = [0.0] * 3, [0.0] * 3
        lo[i], hi[i] = c[i] - t / 2, c[i] + t / 2
        lo[a], hi[a] = a0, a1
        lo[b], hi[b] = b0, b1
        out.append(kit.add(piece, part, f"{name}_{label}", tuple(lo), tuple(hi), **kw))
    return out
