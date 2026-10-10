/*
 * Survivalist Armor - craftable cosmetic armour kits, applied at the smithing table.
 * Copyright (C) 2026 Rusty Shackleford and nfx
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.chunkworks.survivalistarmor.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where a trim lands on a kit (D-0001): a face takes the trim texture's coordinates of the game's own
 * armour box under it. Each of the game's armour boxes is rebuilt here exactly as its model part
 * builds it -- grown, mirrored for a left limb, its faces laid out in the 64-by-32 layout and their
 * corners given their texture coordinates in the same order -- and a kit face is projected onto the
 * box face that faces most nearly as it does. A point of a face that stands out past that box face
 * runs on into the next ones of the same box's layout, as a trim's pattern wraps round the game's
 * armour, and never into another box's.
 */
public final class TrimProjection {
    private TrimProjection() {}

    private static final double WIDTH = 64.0;
    private static final double HEIGHT = 32.0;

    /**
     * One face of a box as the game textures it: corner {@code origin} takes ({@code u1}, {@code v1});
     * {@code uAxis} runs from it to the corner taking u2, {@code vAxis} to the corner taking v2.
     */
    record BoxFace(Vec normal, Vec origin, Vec uAxis, Vec vAxis, double u1, double v1, double u2, double v2) {}

    /** A whole box: its faces, and the rectangle of the layout its faces fill, in texture pixels. */
    record LaidBox(List<BoxFace> faces, double uMin, double vMin, double uMax, double vMax) {}

    private static final Map<Piece.Layer, Map<Part, LaidBox>> BOXES = new EnumMap<>(Piece.Layer.class);

    static {
        for (Piece.Layer layer : Piece.Layer.values()) {
            double grow = layer == Piece.Layer.OUTER ? 1.0 : 0.5;
            Map<Part, LaidBox> parts = new EnumMap<>(Part.class);
            parts.put(Part.HEAD, box(0, 0, -4, -8, -4, 8, 8, 8, grow, false));
            parts.put(Part.BODY, box(16, 16, -4, 0, -2, 8, 12, 4, grow, false));
            parts.put(Part.RIGHT_ARM, box(40, 16, -3, -2, -2, 4, 12, 4, grow, false));
            parts.put(Part.LEFT_ARM, box(40, 16, -1, -2, -2, 4, 12, 4, grow, true));
            parts.put(Part.RIGHT_LEG, box(0, 16, -2, 0, -2, 4, 12, 4, grow, false));
            parts.put(Part.LEFT_LEG, box(0, 16, -2, 0, -2, 4, 12, 4, grow, true));
            BOXES.put(layer, parts);
        }
    }

    /**
     * requires: corners are a face's, in {@code part}'s frame (see {@link RestPose#toPart}), and
     * normal is the face's outward unit normal in the humanoid model's frame<br>
     * effects: returns the trim texture coordinates of the corners, as fractions of the layout
     */
    public static List<Uv> project(Piece.Layer layer, Part part, List<Vec> corners, Vec normal) {
        LaidBox laid = BOXES.get(layer).get(part);
        BoxFace face = laid.faces().get(0);
        double best = Double.NEGATIVE_INFINITY;
        for (BoxFace f : laid.faces()) {
            double d = f.normal().dot(normal);
            if (d > best) {
                best = d;
                face = f;
            }
        }
        List<Uv> out = new ArrayList<>(corners.size());
        for (Vec p : corners) {
            Vec rel = p.minus(face.origin());
            double s = rel.dot(face.uAxis()) / face.uAxis().dot(face.uAxis());
            double t = rel.dot(face.vAxis()) / face.vAxis().dot(face.vAxis());
            double u = clamp(face.u1() + s * (face.u2() - face.u1()), laid.uMin(), laid.uMax());
            double v = clamp(face.v1() + t * (face.v2() - face.v1()), laid.vMin(), laid.vMax());
            out.add(new Uv(u / WIDTH, v / HEIGHT));
        }
        return out;
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    /**
     * effects: returns the box the game builds for {@code addBox(ox, oy, oz, w, h, d)} at texture
     * offset (texU, texV), grown by {@code grow}, mirrored if {@code mirror}: ModelPart.Cube's corners
     * and texture coordinates, face by face
     */
    static LaidBox box(int texU, int texV, double ox, double oy, double oz, double w, double h, double d, double grow, boolean mirror) {
        double x0 = ox - grow, y0 = oy - grow, z0 = oz - grow;
        double x1 = ox + w + grow, y1 = oy + h + grow, z1 = oz + d + grow;
        if (mirror) {
            double swap = x1;
            x1 = x0;
            x0 = swap;
        }
        // ModelPart.Cube's eight corners, by its own names.
        Vec v7 = new Vec(x0, y0, z0), v0 = new Vec(x1, y0, z0), v1 = new Vec(x1, y1, z0), v2 = new Vec(x0, y1, z0);
        Vec v3 = new Vec(x0, y0, z1), v4 = new Vec(x1, y0, z1), v5 = new Vec(x1, y1, z1), v6 = new Vec(x0, y1, z1);
        double f4 = texU, f5 = texU + d, f6 = texU + d + w, f7 = texU + d + w + w, f8 = texU + d + w + d, f9 = texU + d + w + d + w;
        double f10 = texV, f11 = texV + d, f12 = texV + d + h;
        Vec centre = new Vec((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2);
        List<BoxFace> faces = List.of(
                face(centre, v4, v3, v7, v0, f5, f10, f6, f11),   // down
                face(centre, v1, v2, v6, v5, f6, f11, f7, f10),   // up
                face(centre, v7, v3, v6, v2, f4, f11, f5, f12),   // west
                face(centre, v0, v7, v2, v1, f5, f11, f6, f12),   // north
                face(centre, v4, v0, v1, v5, f6, f11, f8, f12),   // east
                face(centre, v3, v4, v5, v6, f8, f11, f9, f12));  // south
        return new LaidBox(faces, texU, texV, texU + 2 * (d + w), texV + d + h);
    }

    /**
     * effects: returns the face with corners a, b, c, d as ModelPart.Polygon textures them: a takes
     * (u2, v1), b (u1, v1), c (u1, v2), d (u2, v2); its normal points away from the box's centre
     */
    private static BoxFace face(Vec centre, Vec a, Vec b, Vec c, Vec d, double u1, double v1, double u2, double v2) {
        Vec middle = a.plus(c).times(0.5);
        return new BoxFace(middle.minus(centre).unit(), b, a.minus(b), c.minus(b), u1, v1, u2, v2);
    }
}
