"""paint: a kit's textures, painted face by face from its geometry (nfx's §7 and §9; D-0002).

Every face is painted in passes, all from what the face is and where it sits, nothing drawn by hand:

1. its material's base and pattern -- canvas twill, quilting, hide grain, webbing weave, brushed or
   dented metal, chain-link, wood grain, glass, glow -- with a colour shift per cube (panel to panel);
2. the near-outline nfx's reference reads by (§3.3): a dark rim at every face's edge, which is a
   cube's edge, with wear just inside it -- bright on hard materials, frayed on cloth -- where the edge
   is not tucked against something;
3. ambient occlusion from the geometry itself, rays cast at the kit's other cubes and the body: grime
   pooling where a strap meets a plate, no painted drop shadows (§7);
4. decals the kit names: stencils, hazard stripes, road-sign paint, rivets, stitching;
5. dust last, over everything (§7): heavier toward the boots, a film on what faces up, pooled in
   recesses; where it lies thick it breaks metalness and smoothness (§9).

A garment face is painted to its colour. A face of the material sheet is painted as a grey key on its
palette's ramp, with masks for weathering, paint, wear and dust, so each armour material's sheet is
the same painting mapped through its own ramp (palettes.py). Each pass writes height, smoothness, F0
and porosity beside colour, from which the LabPBR maps are written (write.py).

Calibration is Pixlli's measured numbers (style.py): its environment's value band, its grain, its
rim, its normals' tilt. No pixel of Pixlli is read.

Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
"""
from __future__ import annotations

import zlib
from dataclasses import dataclass, field

import numpy as np

from . import font
from .geom import BODY, Face, Kit, rotate
from .style import DUST, DUST_LIGHT, HAZARD, HAZARD_DARK, cap_saturation, clamp_value, luma, rgb


@dataclass(frozen=True)
class Look:
    """How a material is painted.

    kind: canvas, padding, quilt, cloth, leather, webbing, rubber, metal, paint, glass, glow, mesh, wood.
    colour: its base colour (a material-sheet part ignores it: its palette colours it).
    smooth, f0, porosity: its LabPBR response (smoothness 0-1; F0 as LabPBR stores it; porosity 0-64).
    variation: how much its panels differ; rim: its outline's darkness; wear: how worn its edges are;
    relief: how deep its pattern is; emission: 1-254 to glow (lens_lit), 0 not; exempt: free of the
    saturation cap (nfx's two exceptions: hazard yellow and lens glow).
    """
    kind: str
    colour: tuple[float, float, float]
    smooth: float
    f0: int
    porosity: int
    variation: float = 0.05
    rim: float = 0.16
    wear: float = 0.5
    relief: float = 1.0
    emission: int = 0
    exempt: bool = False
    patina: tuple[float, float, float] | None = None


def look(kind: str, colour: str, smooth: float, f0: int = 8, porosity: int = 0, **kw) -> Look:
    if isinstance(kw.get("patina"), str):
        kw["patina"] = tuple(rgb(kw["patina"]))
    return Look(kind, tuple(rgb(colour)), smooth, f0, porosity, **kw)


# Material presets; a kit gives each its own colour.
def canvas(c: str, **kw) -> Look:
    return look("canvas", c, 0.12, 6, 48, **kw)


def padding(c: str, **kw) -> Look:
    return look("padding", c, 0.10, 5, 52, **({"rim": 0.20} | kw))


def quilt(c: str, **kw) -> Look:
    return look("quilt", c, 0.30, 8, 30, **({"rim": 0.22} | kw))


def cloth(c: str, **kw) -> Look:
    return look("cloth", c, 0.08, 5, 56, **kw)


def leather(c: str, **kw) -> Look:
    return look("leather", c, 0.30, 7, 40, **kw)


def webbing(c: str, **kw) -> Look:
    return look("webbing", c, 0.14, 6, 46, **({"rim": 0.24} | kw))


def rubber(c: str, **kw) -> Look:
    return look("rubber", c, 0.20, 9, 8, **({"variation": 0.02, "wear": 0.25} | kw))


def steel(c: str = "#6f7275", **kw) -> Look:
    return look("metal", c, 0.62, 230, 0, **({"rim": 0.22, "wear": 0.7} | kw))


def painted(c: str, **kw) -> Look:
    return look("paint", c, 0.40, 12, 0, **({"rim": 0.2, "wear": 0.7} | kw))


def glass(c: str = "#151817", **kw) -> Look:
    return look("glass", c, 0.93, 10, 0, **({"variation": 0.0, "rim": 0.3, "wear": 0.0, "relief": 0.0} | kw))


def glow(c: str = "#f08a2a", **kw) -> Look:
    return look("glow", c, 0.90, 10, 0, **({"variation": 0.0, "rim": 0.2, "wear": 0.0, "relief": 0.0, "emission": 230, "exempt": True} | kw))


def wood(c: str = "#6a4b31", **kw) -> Look:
    return look("wood", c, 0.22, 6, 30, **kw)


def ceramic(c: str = "#c4beb0", **kw) -> Look:
    return look("paint", c, 0.72, 12, 0, **({"rim": 0.25, "wear": 0.4} | kw))


def copper(c: str = "#6a3f2a", **kw) -> Look:
    return look("metal", c, 0.55, 234, 0, **({"rim": 0.22, "wear": 0.6, "patina": "#4f6a5a"} | kw))


def hose(c: str, **kw) -> Look:
    return look("hose", c, 0.22, 9, 6, **({"variation": 0.03, "wear": 0.3} | kw))


def mesh() -> Look:
    return look("mesh", "#808080", 0.5, 230, 0, rim=0.0, wear=0.4)


def hazard() -> Look:
    return look("hazard", "#c6a42e", 0.28, 10, 4, rim=0.20, wear=0.8, exempt=True)


# The material sheet's parts: painted as a key, coloured by the palette; chain-link as a weave with holes.
BASE = Look("metal", (0.5, 0.5, 0.5), 0.6, 230, 0, rim=0.24, wear=0.75)
BASE_MESH = Look("mesh", (0.5, 0.5, 0.5), 0.55, 230, 0, rim=0.0, wear=0.4)


@dataclass
class Painted:
    """One face's painting.

    Garment: colour (h, w, 3), alpha, height, occlusion, smooth, f0, porosity, emission, exempt.
    Material sheet: key (h, w) on the ramp, weather, wear, paint (h, w, 4) and dust masks, the rest as above.
    """
    w: int
    h: int
    alpha: np.ndarray
    height: np.ndarray
    occlusion: np.ndarray
    colour: np.ndarray | None = None
    smooth: np.ndarray | None = None
    f0: np.ndarray | None = None
    porosity: np.ndarray | None = None
    emission: np.ndarray | None = None
    exempt: np.ndarray | None = None
    key: np.ndarray | None = None
    weather: np.ndarray | None = None
    wear: np.ndarray | None = None
    paint: np.ndarray | None = None
    dust: np.ndarray | None = None


# --- noise --------------------------------------------------------------------------------------

def rng_for(*parts) -> np.random.Generator:
    return np.random.default_rng(zlib.crc32("/".join(map(str, parts)).encode()))


def value_noise(rng: np.random.Generator, h: int, w: int, scale: float) -> np.ndarray:
    """effects: returns smooth noise in 0..1 over (h, w), features about `scale` pixels apart."""
    gh, gw = int(np.ceil(h / scale)) + 2, int(np.ceil(w / scale)) + 2
    g = rng.random((gh, gw))
    y = (np.arange(h) + 0.5) / scale
    x = (np.arange(w) + 0.5) / scale
    y0, x0 = np.floor(y).astype(int), np.floor(x).astype(int)
    fy, fx = y - y0, x - x0
    fy, fx = fy * fy * (3 - 2 * fy), fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]
    b = g[y0][:, x0 + 1]
    c = g[y0 + 1][:, x0]
    d = g[y0 + 1][:, x0 + 1]
    top = a + (b - a) * fx[None, :]
    bot = c + (d - c) * fx[None, :]
    return top + (bot - top) * fy[:, None]


# --- geometry-derived fields --------------------------------------------------------------------

def edge_distance(h: int, w: int) -> np.ndarray:
    """effects: returns each pixel's distance in pixels to its face's nearest edge (0.5 at the rim)."""
    y = np.minimum(np.arange(h) + 0.5, h - np.arange(h) - 0.5)
    x = np.minimum(np.arange(w) + 0.5, w - np.arange(w) - 0.5)
    return np.minimum(y[:, None], x[None, :])


def hemisphere(normal: np.ndarray, n: int, rng: np.random.Generator) -> np.ndarray:
    """effects: returns n cosine-weighted directions about normal."""
    u1, u2 = rng.random(n), rng.random(n)
    r, phi = np.sqrt(u1), 2 * np.pi * u2
    local = np.stack([r * np.cos(phi), r * np.sin(phi), np.sqrt(1 - u1)], axis=1)
    a = np.array([1.0, 0.0, 0.0]) if abs(normal[0]) < 0.9 else np.array([0.0, 1.0, 0.0])
    t = np.cross(normal, a)
    t /= np.linalg.norm(t)
    b = np.cross(normal, t)
    return local[:, 0:1] * t + local[:, 1:2] * b + local[:, 2:3] * normal


class Occluders:
    """The boxes a face's rays may hit: a piece's cubes and the body under them, each in its own turned frame."""

    def __init__(self, kit: Kit, piece: str):
        self.boxes = []
        for c in kit.cubes:
            if c.piece == piece and c.material not in ("mesh",):
                self.boxes.append((np.array(c.lo), np.array(c.hi), c.pivot(), c.rotation, id(c)))
        for lo, hi in BODY.values():
            self.boxes.append((np.array(lo), np.array(hi), np.zeros(3), (0.0, 0.0, 0.0), 0))

    def occlusion(self, points: np.ndarray, normal: np.ndarray, own: int, rng: np.random.Generator, rays: int = 14,
                  reach: float = 3.5) -> np.ndarray:
        """effects: returns the fraction of each point's hemisphere left open (1 = unoccluded), points (N, 3)."""
        dirs = hemisphere(normal, rays, rng)
        o = points[:, None, :] + normal * 0.03
        d = np.broadcast_to(dirs[None, :, :], (points.shape[0], rays, 3))
        hit = np.zeros((points.shape[0], rays), dtype=bool)
        for lo, hi, pivot, rot, ident in self.boxes:
            if ident == own:
                continue
            oo, dd = o, d
            if any(rot):
                inv = tuple(-r for r in rot)
                # Undo the turn: Z, then Y, then X, each the opposite way.
                oo = _unrotate(o, pivot, rot)
                dd = _unrotate(d, np.zeros(3), rot)
            with np.errstate(divide="ignore", invalid="ignore"):
                t1 = (lo - oo) / dd
                t2 = (hi - oo) / dd
            tmin = np.nanmax(np.minimum(t1, t2), axis=-1)
            tmax = np.nanmin(np.maximum(t1, t2), axis=-1)
            hit |= (tmax >= np.maximum(tmin, 0.0)) & (tmin <= reach)
        return 1.0 - hit.mean(axis=1)


def _unrotate(p: np.ndarray, origin: np.ndarray, degrees) -> np.ndarray:
    a, b, g = (np.radians(d) for d in degrees)
    q = p - origin
    x, y, z = q[..., 0], q[..., 1], q[..., 2]
    x, y = x * np.cos(-g) - y * np.sin(-g), x * np.sin(-g) + y * np.cos(-g)
    x, z = x * np.cos(-b) + z * np.sin(-b), -x * np.sin(-b) + z * np.cos(-b)
    y, z = y * np.cos(-a) - z * np.sin(-a), y * np.sin(-a) + z * np.cos(-a)
    return np.stack([x, y, z], axis=-1) + origin


def rivet_spots(h: int, w: int, rng: np.random.Generator, pitch: int) -> list[tuple[int, int]]:
    """effects: returns rivet positions round a face's border, clustered rather than evenly spaced (§7), one now and then lost."""
    out = []
    m = 3
    for y in range(m, h - m, max(4, pitch)):
        for x in (m, w - m - 1):
            out.append((y + int(rng.integers(-1, 2)), x))
    for x in range(m + 4, w - m - 4, max(4, pitch)):
        for y in (m, h - m - 1):
            out.append((y, x + int(rng.integers(-1, 2))))
    return [(y, x) for (y, x) in out if 0 <= y < h and 0 <= x < w and rng.random() > 0.15]


def down_in_texture(face: Face) -> tuple[float, float]:
    """effects: returns the world's down as a direction across the face's texture (u right, v down the texture)."""
    right = face.corners[3] - face.corners[0]
    downv = face.corners[1] - face.corners[0]
    world = np.array([0.0, -1.0, 0.0])
    du = float(np.dot(world, right / max(np.linalg.norm(right), 1e-9)))
    dv = float(np.dot(world, downv / max(np.linalg.norm(downv), 1e-9)))
    n = max(1e-9, (du * du + dv * dv) ** 0.5)
    return du / n, dv / n


def top_edge(h: int, w: int, du: float, dv: float, rng: np.random.Generator) -> list[tuple[int, int]]:
    """effects: returns a few points along the face's uppermost edge, where rain and rust start."""
    if abs(dv) >= abs(du):
        row = 0 if dv > 0 else h - 1
        return [(row, int(x)) for x in rng.integers(0, w, size=max(1, w // 9))]
    col = 0 if du > 0 else w - 1
    return [(int(y), col) for y in rng.integers(0, h, size=max(1, h // 9))]


def streaks(h: int, w: int, du: float, dv: float, seeds: list[tuple[int, int]], rng: np.random.Generator) -> np.ndarray:
    """effects: returns streaks running down the face from each seed, fading as they go."""
    out = np.zeros((h, w))
    for (y, x) in seeds:
        length = int(rng.integers(5, 24))
        strength = float(rng.uniform(0.4, 1.0))
        wide = rng.random() < 0.3
        for k in range(length):
            yk, xk = int(round(y + dv * k)), int(round(x + du * k))
            if not (0 <= yk < h and 0 <= xk < w):
                break
            v = strength * (1 - k / length) ** 1.3
            out[yk, xk] = max(out[yk, xk], v)
            if wide and 0 <= xk + 1 < w:
                out[yk, xk + 1] = max(out[yk, xk + 1], v * 0.6)
    return out


# --- patterns -------------------------------------------------------------------------------------

def pattern(kind: str, h: int, w: int, rng: np.random.Generator, along_u: bool) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """effects: returns a material's pattern over (h, w): value shift, height, and alpha."""
    lum = np.zeros((h, w))
    ht = np.zeros((h, w))
    alpha = np.ones((h, w))
    yy, xx = np.mgrid[0:h, 0:w]
    mid = value_noise(rng, h, w, 4.0) - 0.5
    low = value_noise(rng, h, w, 14.0) - 0.5
    if kind == "canvas":
        twill = ((xx + yy) % 4 == 0)
        lum += -0.022 * twill + 0.035 * mid + 0.04 * low
        ht += 0.10 * twill + 0.15 * mid
    elif kind == "padding":
        # Quilted channels: stitched seams every five pixels across the long way, each channel puffed.
        period = 7
        col = (xx if along_u is False or h >= w else yy) % period
        puff = np.sin(np.pi * (col + 0.5) / period)
        seam = col == 0
        lum += 0.025 * (puff - 0.6) - 0.05 * seam + 0.03 * mid + 0.04 * low
        ht += 0.45 * puff - 0.25 * seam
    elif kind == "cloth":
        lum += 0.03 * mid + 0.05 * low
        ht += 0.2 * mid
    elif kind == "quilt":
        # Diamond quilting: two families of stitched seams at 45 degrees, each cell puffed between them, and the
        # broad blotches a coated suit takes. Rows of seams across the long way read as planks (2026-10-09).
        period = 14
        a, b = (xx + yy) % period, (xx - yy) % period
        puff = np.sin(np.pi * (a + 0.5) / period) * np.sin(np.pi * (b + 0.5) / period)
        seam = ((a == 0) | (b == 0)) & ((xx + yy) % 3 != 0)
        blotch = value_noise(rng, h, w, 9.0) - 0.5
        lum += 0.035 * (puff - 0.5) - 0.05 * seam + 0.02 * mid + 0.07 * blotch
        ht += 0.5 * puff - 0.3 * seam
    elif kind == "leather":
        blot = value_noise(rng, h, w, 3.0) - 0.5
        lum += 0.05 * blot + 0.05 * low
        crease = np.zeros((h, w), bool)
        for _ in range(max(1, (h * w) // 260)):
            cy, cx = rng.integers(0, h), rng.integers(0, w)
            length = rng.integers(3, 8)
            for k in range(length):
                y, x = cy + (k if rng.random() < 0.5 else 0), cx + k
                if 0 <= y < h and 0 <= x < w:
                    crease[y, x] = True
        lum += -0.06 * crease
        ht += 0.25 * blot - 0.3 * crease
    elif kind == "webbing":
        ribs = ((xx if not along_u else yy) % 2 == 0)
        lum += -0.03 * ribs + 0.02 * mid
        ht += 0.25 * ribs
    elif kind == "rubber":
        lum += 0.012 * mid + 0.02 * low
        ht += 0.05 * mid
    elif kind == "hose":
        # Ribbed: a band every three pixels across the hose's long way, each rib raised and catching light.
        rib = ((xx if along_u else yy) % 3) == 0
        lum += 0.06 * rib - 0.02 + 0.012 * mid
        ht += 0.5 * rib
    elif kind in ("metal", "paint"):
        # Brushed: fine streaks running the plate's long way; a little broad variation, no clouds.
        streak = (value_noise(rng, h, 1, 1.2)[:, 0] - 0.5)[:, None] if along_u else (value_noise(rng, 1, w, 1.2)[0] - 0.5)[None, :]
        lum += 0.035 * streak + 0.015 * low
        # Scratches: short bright lines; dents: dark blobs with a bright lip.
        for _ in range(max(1, (h * w) // 500)):
            y, x = rng.integers(0, h), rng.integers(0, w)
            dy, dx = rng.choice([-1, 0, 1]), 1
            for k in range(rng.integers(3, 10)):
                yk, xk = y + dy * k // 2, x + dx * k
                if 0 <= yk < h and 0 <= xk < w:
                    lum[yk, xk] += 0.08
                    ht[yk, xk] -= 0.15
        dents = value_noise(rng, h, w, 6.0)
        dent = np.clip((dents - 0.86) * 8, 0, 1)
        lum += -0.06 * dent
        ht += -0.5 * dent
    elif kind == "wood":
        grain = value_noise(rng, max(1, h // 8 + 2), w, 1.0) if along_u else value_noise(rng, h, max(1, w // 8 + 2), 1.0)
        g = np.kron(grain, np.ones((8, 1)))[:h, :w] if along_u else np.kron(grain, np.ones((1, 8)))[:h, :w]
        lum += 0.06 * (g - 0.5) + 0.03 * low
        ht += 0.3 * (g - 0.5)
    elif kind == "mesh":
        # Chain-link: two families of diagonal wires, a diamond weave, holes between (nfx's §6: not rings).
        # Diamonds a block and a half across and two-pixel wire, so the weave still reads at arm's length.
        p = 12
        a = (xx + yy) % p
        b = (xx - yy) % p
        wire = (a < 2) | (b < 2)
        alpha = wire.astype(float)
        cross_over = (a < 2) & ((yy // p) % 2 == 0)
        lum += 0.08 * cross_over
        ht += 0.6 * wire
    return lum, ht, alpha


# --- the painter ----------------------------------------------------------------------------------

@dataclass
class Painter:
    """Paints a kit's faces. `looks` names each material's look; `dust` scales the dust pass for the kit."""
    kit: Kit
    looks: dict
    dust: float = 1.0
    seed: str = ""
    occluders: dict = field(default_factory=dict)

    def look(self, face: Face) -> Look:
        if face.cube.base:
            return BASE_MESH if face.cube.material == "mesh" else BASE
        m = face.cube.material
        if m not in self.looks:
            raise KeyError(f"{self.kit.name}: no look for material {m!r} ({face.cube.name})")
        return self.looks[m]

    def paint(self, face: Face) -> Painted:
        w, h = face.pixels()
        rng = rng_for(self.kit.name, self.seed, face.index, face.direction)
        lk = self.look(face)
        cube = face.cube
        # Which way its texture runs along: a strap's weave and a plate's brushing follow its long side.
        along_u = face.width >= face.height
        lum, ht, alpha = pattern(lk.kind, h, w, rng, along_u)
        d = edge_distance(h, w)

        # Occlusion, at half resolution where the face is large.
        pts = face.points(w, h)
        occ_key = cube.piece
        if occ_key not in self.occluders:
            self.occluders[occ_key] = Occluders(self.kit, cube.piece)
        step = 2 if w * h > 400 else 1
        sub = pts[::step, ::step].reshape(-1, 3)
        ao_sub = self.occluders[occ_key].occlusion(sub, face.normal, id(cube), rng).reshape(pts[::step, ::step].shape[:2])
        ao = np.kron(ao_sub, np.ones((step, step)))[:h, :w]
        ao = np.clip(ao, 0.0, 1.0)

        # The near-outline and the wear just inside it, less where the edge is tucked away.
        rim = d < 1.0
        band = (d >= 1.0) & (d < 2.5)
        wear_noise = value_noise(rng, h, w, 2.0)
        open_edge = ao > 0.55
        wear = band & open_edge & (wear_noise > 1.0 - 0.55 * lk.wear)
        ht = ht + np.clip(d / 1.5, 0, 1) ** 0.5 * 0.4  # a bevelled edge: lower at the rim
        # Where the cube's per-panel shift lies.
        panel = (rng_for(self.kit.name, self.seed, face.index, "panel").random() - 0.5) * 2 * lk.variation

        # Dust: by height (heavier toward the boots), facing up, pooled in recesses, mottled.
        y = pts[..., 1]
        low_noise = value_noise(rng, h, w, 10.0)
        dust = (0.02 + 0.36 * np.clip(1 - y / 33.0, 0, 1) ** 2.2 + 0.26 * max(0.0, face.normal[1]) + 0.26 * (1 - ao)
                + 0.18 * (low_noise - 0.5))
        dust = np.clip(dust * self.dust, 0, 0.85)
        if lk.kind in ("glass", "glow"):
            dust *= 0.25

        if cube.base:
            return self._base(face, lk, w, h, lum, ht, alpha, ao, rim, wear, panel, dust, rng)
        return self._garment(face, lk, w, h, lum, ht, alpha, ao, rim, wear, panel, dust, rng)

    def _garment(self, face, lk, w, h, lum, ht, alpha, ao, rim, wear, panel, dust, rng) -> Painted:
        base = np.array(lk.colour)
        colour = np.broadcast_to(base, (h, w, 3)).copy()
        shift = lum + panel
        colour = colour * (1 + shift[..., None] * 2.2)
        if lk.kind == "hazard" or face.cube.paint.get("hazard") in (True, face.direction, "all"):
            colour, ht = self._hazard(face, h, w, colour, ht, rng)
        if lk.kind == "glass":
            hl = np.zeros((h, w), bool)
            hl[: max(1, h // 4), : max(1, w // 3)] = True
            colour = np.where(hl[..., None], colour + 0.10, colour)
        if lk.kind == "glow":
            yy, xx = np.mgrid[0:h, 0:w]
            c = np.exp(-(((yy - h / 2) / max(1, h / 2)) ** 2 + ((xx - w / 2) / max(1, w / 2)) ** 2))
            colour = colour * (0.75 + 0.45 * c[..., None])
        # Wear: bright on hard things, frayed and pale on cloth.
        if lk.kind in ("metal", "paint", "hazard", "rubber"):
            colour = np.where(wear[..., None], colour * 0.6 + np.array([0.58, 0.57, 0.55]) * 0.4, colour)
        elif lk.kind not in ("glass", "glow"):
            colour = np.where(wear[..., None], colour * 1.10, colour)
        colour = self._decals(face, h, w, colour, rng, ht)
        if lk.patina is not None:
            d = edge_distance(h, w)
            mask = np.clip(1.6 - d, 0, 1) * 0.5 + (1 - ao) * 0.7 + np.clip((value_noise(rng, h, w, 2.0) - 0.8) * 4, 0, 1) * 0.5
            du, dv = down_in_texture(face)
            if abs(face.normal[1]) < 0.7:
                mask += streaks(h, w, du, dv, top_edge(h, w, du, dv, rng), rng) * 0.8
            colour = colour + (np.array(lk.patina) - colour) * (np.clip(mask, 0, 1) * 0.45)[..., None]
        colour = np.where(rim[..., None], colour * (1 - lk.rim), colour)
        colour = colour * (0.62 + 0.38 * ao[..., None])
        # Dust last, over everything; on glow and glass hardly at all.
        dc = DUST + (DUST_LIGHT - DUST) * value_noise(rng, h, w, 3.0)[..., None]
        colour = colour + (dc - colour) * (dust * 0.75)[..., None]
        exempt = np.full((h, w), lk.exempt)
        if face.cube.paint.get("hazard"):
            exempt = exempt | (np.abs(colour - HAZARD).sum(-1) < 0.35)
        capped = clamp_value(cap_saturation(colour))
        colour = np.where(exempt[..., None], np.clip(colour, 0, 1), capped)
        smooth = np.full((h, w), lk.smooth)
        smooth = smooth + (0.12 - smooth) * np.clip(dust * 1.3, 0, 1)
        f0 = np.full((h, w), lk.f0, dtype=int)
        f0 = np.where((dust > 0.45) & (f0 >= 230), 10, f0)
        f0 = np.where(wear & (lk.kind == "paint"), 230, f0)
        porosity = np.clip(np.full((h, w), lk.porosity) + dust * 30, 0, 64).astype(int)
        emission = np.full((h, w), lk.emission, dtype=int)
        ht = ht * lk.relief
        ht = ht + (ht.mean() - ht) * np.clip(dust, 0, 1) * 0.6  # dust fills the relief in
        return Painted(w, h, alpha, ht, ao, colour=colour, smooth=smooth, f0=f0, porosity=porosity, emission=emission, exempt=exempt)

    def _base(self, face, lk, w, h, lum, ht, alpha, ao, rim, wear, panel, dust, rng) -> Painted:
        key = np.full((h, w), 0.56) + lum * 2.0 + panel * 1.2
        key = np.where(rim, key - 0.30, key)
        key = np.where(wear, 0.93, key)
        d = edge_distance(h, w)
        # Rust and grime gather at the cut edges and where the plate is tucked against something, a few specks
        # between; rivets and the top edge weep it downward in streaks (never in blobs mid-panel, §7).
        weather = np.clip(1.6 - d, 0, 1) * 0.5 + (1 - ao) * 0.6
        weather += np.clip((value_noise(rng, h, w, 2.0) - 0.84) * 5, 0, 1) * 0.5
        heads = []
        if face.cube.paint.get("rivets") in (True, face.direction):
            heads = rivet_spots(h, w, rng, face.cube.paint.get("pitch", 7))
        du, dv = down_in_texture(face)
        if abs(face.normal[1]) < 0.7:
            seeds = list(heads) + top_edge(h, w, du, dv, rng)
            weather += streaks(h, w, du, dv, seeds, rng) * 0.85
        for (y, x) in heads:
            key[y, x] = 0.95
            sy, sx = y + int(round(dv)), x + int(round(du))
            if 0 <= sy < h and 0 <= sx < w:
                key[sy, sx] = 0.12
        key = np.clip(key * (0.55 + 0.45 * ao), 0.0, 1.0)
        weather = np.clip(weather, 0, 1)
        if lk.kind == "mesh":
            # A cut panel is bound along its edges by a wire threaded through, which is what makes it a panel.
            alpha = np.maximum(alpha, (d < 2.0).astype(float))
        paint = np.zeros((h, w, 4))
        paint = self._sign(face, h, w, paint, rng)
        paint = self._decal_layer(face, h, w, paint, rng)
        return Painted(w, h, alpha, ht * lk.relief, ao, key=key, weather=weather, wear=wear.astype(float), paint=paint, dust=dust)

    # --- decals ---------------------------------------------------------------------------------

    def _hazard(self, face, h, w, colour, ht, rng):
        """Hazard stripes, 45 degrees, peeling: a corner lifted, a torn edge (nfx's §5 and §7: never clean)."""
        yy, xx = np.mgrid[0:h, 0:w]
        period = 8
        stripe = ((xx + yy) // (period // 2)) % 2 == 0
        tape = np.where(stripe[..., None], HAZARD, HAZARD_DARK)
        peel = value_noise(rng, h, w, 3.0) > 0.78
        corner = (xx + yy) < max(2, min(h, w) // 3)
        torn = peel | (corner if rng.random() < 0.5 else (xx + (h - yy)) < max(2, min(h, w) // 3))
        colour = np.where(torn[..., None], colour, tape)
        ht = ht + np.where(torn, -0.1, 0.12)
        return colour, ht

    def _decals(self, face, h, w, colour, rng, ht):
        p = face.cube.paint
        if p.get("stitch"):
            dash = np.zeros((h, w), bool)
            if h > 6 and w > 6:
                dash[2, ::3] = True
                dash[h - 3, ::3] = True
                dash[::3, 2] = True
                dash[::3, w - 3] = True
            colour = np.where(dash[..., None], colour * 0.72, colour)
        if p.get("rivets") in (True, face.direction):
            colour, _ = self._rivets(h, w, colour, rng, p.get("pitch", 7))
        stencil = p.get("stencil")
        if stencil and stencil.get("face", "south") == face.direction:
            colour = self._stencil(h, w, colour, rng, stencil)
        spiral = p.get("spiral")
        if spiral:
            colour = self._spiral(face, h, w, colour, spiral)
        return colour

    @staticmethod
    def _spiral(face, h, w, colour, spec):
        """A rolled thing's end (a bedroll): the roll's spiral, drawn by where each pixel lies round the roll's axis, so
        the two crossed boxes of one roll show one spiral between them."""
        axis = "xyz".index(spec["axis"])
        if abs(face.normal[axis]) < 0.9:
            return colour
        rel = face.points(w, h) - np.array(spec["centre"], dtype=float)
        u, v = [rel[..., i] for i in range(3) if i != axis]
        r = np.hypot(u, v)
        turn = (np.arctan2(v, u) / (2 * np.pi)) % 1.0
        pitch = spec.get("pitch", 0.55)
        line = ((r / pitch - turn) % 1.0) < 0.38
        return np.where(line[..., None], colour * 0.62, colour * 1.04)

    def _rivets(self, h, w, colour, rng, pitch):
        out = colour.copy()
        positions = []
        m = 3
        for y in range(m, h - m, max(4, pitch)):
            for x in (m, w - m - 1):
                positions.append((y, x))
        for x in range(m, w - m, max(4, pitch)):
            for y in (m, h - m - 1):
                positions.append((y, x))
        for (y, x) in positions:
            if rng.random() < 0.15:
                continue  # one lost
            if 0 <= y < h and 0 <= x < w:
                out[y, x] = np.array([0.66, 0.64, 0.60])
                if y + 1 < h and x + 1 < w:
                    out[y + 1, x + 1] = out[y + 1, x + 1] * 0.55
        return out, positions

    def _stencil(self, h, w, colour, rng, spec):
        mask = font.text_mask(spec["text"], spec.get("scale", 2))
        mh, mw = mask.shape
        at = spec.get("at", (0.5, 0.5))
        y0 = int(at[1] * h - mh / 2)
        x0 = int(at[0] * w - mw / 2)
        paint = rgb(spec.get("colour", "#d6cdb8"))
        out = colour.copy()
        overspray = value_noise(rng, h, w, 1.5)
        for y in range(mh):
            for x in range(mw):
                yy, xx = y0 + y, x0 + x
                if 0 <= yy < h and 0 <= xx < w and mask[y, x] and overspray[yy, xx] > 0.18:
                    out[yy, xx] = out[yy, xx] * 0.25 + paint * 0.75
        return out

    def _sign(self, face, h, w, paint, rng):
        """A road sign's paint on a cut plate (the Roadplate): the face colour, a border, lettering, worn through to the metal."""
        sign = face.cube.paint.get("sign")
        if not sign or sign.get("face", "south") != face.direction:
            return paint
        out = paint.copy()
        field = rgb(sign["field"])
        out[..., :3] = field
        out[..., 3] = 1.0
        if sign.get("border"):
            b = sign.get("border_width", 3)
            border = np.zeros((h, w), bool)
            border[:b, :] = border[-b:, :] = border[:, :b] = border[:, -b:] = True
            out[border, :3] = rgb(sign["border"])
        if sign.get("text"):
            mask = font.text_mask(sign["text"], sign.get("scale", 3), stencil=False)
            mh, mw = mask.shape
            ox, oy = sign.get("offset", (0, 0))
            y0 = int(h / 2 - mh / 2) + oy
            x0 = int(w / 2 - mw / 2) + ox
            ys, xs = np.nonzero(mask)
            ys, xs = ys + y0, xs + x0
            ok = (ys >= 0) & (ys < h) & (xs >= 0) & (xs < w)
            out[ys[ok], xs[ok], :3] = rgb(sign["ink"])
        # Worn through where the plate was cut and knocked about: the paint lost along the cut edges and corners,
        # along scratches, in chips, and in a few scuffed patches; most of the sign is still there to read.
        d = edge_distance(h, w)
        gone = (d < 1.5) | ((d < 3.5) & (value_noise(rng, h, w, 2.0) > 0.45))
        gone |= value_noise(rng, h, w, 2.5) > 0.86
        gone |= value_noise(rng, h, w, 9.0) < sign.get("wear", 0.18)
        for _ in range(max(2, (h * w) // 700)):
            y, x = rng.integers(0, h), rng.integers(0, w)
            dy = rng.choice([-1, 0, 1])
            for k in range(rng.integers(6, 18)):
                yk, xk = y + (dy * k) // 3, x + k
                if 0 <= yk < h and 0 <= xk < w:
                    gone[yk, xk] = True
        out[gone, 3] = 0.0
        out[..., 3] *= 0.94
        return out

    def _decal_layer(self, face, h, w, paint, rng):
        p = face.cube.paint
        if p.get("hazard") in (True, face.direction, "all"):
            yy, xx = np.mgrid[0:h, 0:w]
            stripe = ((xx + yy) // 4) % 2 == 0
            out = paint.copy()
            out[..., :3] = np.where(stripe[..., None], HAZARD, HAZARD_DARK)
            torn = value_noise(rng, h, w, 3.0) > 0.74
            out[..., 3] = np.where(torn, 0.0, 0.95)
            return out
        stencil = p.get("stencil")
        if stencil and stencil.get("face", "south") == face.direction:
            mask = font.text_mask(stencil["text"], stencil.get("scale", 2))
            mh, mw = mask.shape
            at = stencil.get("at", (0.5, 0.5))
            y0, x0 = int(at[1] * h - mh / 2), int(at[0] * w - mw / 2)
            out = paint.copy()
            ys, xs = np.nonzero(mask)
            ys, xs = ys + y0, xs + x0
            ok = (ys >= 0) & (ys < h) & (xs >= 0) & (xs < w)
            out[ys[ok], xs[ok], :3] = rgb(stencil.get("colour", "#d6cdb8"))
            out[ys[ok], xs[ok], 3] = 0.8
            return out
        return paint
