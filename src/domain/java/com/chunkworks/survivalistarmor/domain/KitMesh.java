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
 * A kit as the game draws it: for each piece, its quads in packed arrays, once per body part in that
 * part's own frame (worn, and on the arm in first person), and once all together in an item's
 * one-block box (the piece as an item).
 *
 * <p>A face that another cube of the same piece and part covers entirely is left out: it can never
 * be seen, and it would only be painted for nothing. Parts move apart, so only a cube on the same
 * part covers; a cube turned off the axes neither covers nor is covered; and a cube that can be seen
 * through (chain-link) covers nothing.
 *
 * <p>AF: for each piece p, {@code worn(p)} is p drawn on a body, part by part, and {@code item(p)}
 * the same quads standing as an item. RI: every array of a {@link Quads} holds its quads' worth of
 * entries; no array is written after construction (they are handed out unwrapped, for drawing
 * without copying, and callers must not write them).
 */
public final class KitMesh {
    /** How far out of a face a covering cube must reach, in sixteenths of a block. */
    private static final double REACH = 0.01;
    private static final double SLACK = 1e-6;

    /**
     * Quads in packed arrays, four corners each.
     *
     * <ul>
     *   <li>{@code positions}: twelve per quad, in blocks;</li>
     *   <li>{@code normals}: three per quad;</li>
     *   <li>{@code uvs}: eight per quad, as fractions of the quad's texture;</li>
     *   <li>{@code trimUvs}: eight per quad, as fractions of the game's 64-by-32 armour layout, NaN where
     *       the quad takes no trim;</li>
     *   <li>{@code material}: per quad, whether it draws from the material sheet rather than the garment;</li>
     *   <li>{@code lit}: per quad, whether it glows.</li>
     * </ul>
     */
    public record Quads(int count, float[] positions, float[] normals, float[] uvs, float[] trimUvs, boolean[] material, boolean[] lit) {}

    /** One body part's quads, in that part's frame (see {@link RestPose#toPart}). */
    public record PartQuads(Part part, Quads quads) {}

    private final Map<Piece, List<PartQuads>> worn;
    private final Map<Piece, Quads> item;
    private final int textureWidth;
    private final int textureHeight;

    private KitMesh(Map<Piece, List<PartQuads>> worn, Map<Piece, Quads> item, int textureWidth, int textureHeight) {
        this.worn = worn;
        this.item = item;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    /** effects: returns {@code model} as the game draws it */
    public static KitMesh of(KitModel model) {
        Map<Piece, List<PartQuads>> worn = new EnumMap<>(Piece.class);
        Map<Piece, Quads> item = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            List<KitFace> faces = model.faces(piece);
            if (faces.isEmpty()) {
                continue;
            }
            List<KitFace> seen = new ArrayList<>();
            List<PartQuads> parts = new ArrayList<>();
            for (Part part : piece.parts()) {
                List<KitFace> on = faces.stream().filter(f -> f.part() == part).toList();
                List<KitFace> shown = on.stream().filter(f -> !covered(f, on)).toList();
                if (!shown.isEmpty()) {
                    parts.add(new PartQuads(part, wornQuads(piece, part, shown)));
                    seen.addAll(shown);
                }
            }
            if (!parts.isEmpty()) {
                worn.put(piece, List.copyOf(parts));
                item.put(piece, itemQuads(piece, seen));
            }
        }
        return new KitMesh(worn, item, model.textureWidth(), model.textureHeight());
    }

    /** effects: returns {@code piece}'s quads, part by part, empty if the kit does not dress it */
    public List<PartQuads> worn(Piece piece) {
        return worn.getOrDefault(piece, List.of());
    }

    /** effects: returns {@code piece}'s quads standing as an item in a one-block box, or null if the kit does not dress it */
    public Quads item(Piece piece) {
        return item.get(piece);
    }

    /** effects: returns whether the kit dresses {@code piece} */
    public boolean dresses(Piece piece) {
        return worn.containsKey(piece);
    }

    public int textureWidth() {
        return textureWidth;
    }

    public int textureHeight() {
        return textureHeight;
    }

    /** effects: returns whether a cube of {@code others} other than f's own holds all of f, reached just outside it */
    static boolean covered(KitFace f, List<KitFace> others) {
        if (f.box() == null) {
            return false;
        }
        Vec out = f.normal().times(REACH);
        for (KitFace g : others) {
            if (g.cube() == f.cube() || g.box() == null || Materials.seeThrough(g.material())) {
                continue;
            }
            boolean all = true;
            for (Vec c : f.corners()) {
                if (!g.box().contains(c.plus(out), SLACK)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    private static Quads wornQuads(Piece piece, Part part, List<KitFace> faces) {
        Packer p = new Packer(faces.size());
        for (KitFace f : faces) {
            List<Vec> local = f.corners().stream().map(c -> RestPose.toPart(part, c)).toList();
            Vec normal = RestPose.directionToModel(f.normal());
            List<Uv> trim = Materials.trimmable(f.material()) ? TrimProjection.project(piece.layer(), part, local, normal) : null;
            p.add(local, 1.0 / 16.0, Vec.ZERO, normal, f, trim);
        }
        return p.done();
    }

    /** The piece stands as an item as it stands worn, centred in a one-block box that its longest side fills. */
    private static Quads itemQuads(Piece piece, List<KitFace> faces) {
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (KitFace f : faces) {
            for (Vec c : f.corners()) {
                lo[0] = Math.min(lo[0], c.x());
                lo[1] = Math.min(lo[1], c.y());
                lo[2] = Math.min(lo[2], c.z());
                hi[0] = Math.max(hi[0], c.x());
                hi[1] = Math.max(hi[1], c.y());
                hi[2] = Math.max(hi[2], c.z());
            }
        }
        double longest = Math.max(hi[0] - lo[0], Math.max(hi[1] - lo[1], hi[2] - lo[2]));
        double scale = 1.0 / longest;
        Vec centre = new Vec((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2);
        Packer p = new Packer(faces.size());
        for (KitFace f : faces) {
            List<Vec> local = f.corners().stream().map(c -> RestPose.toPart(f.part(), c)).toList();
            List<Uv> trim = Materials.trimmable(f.material())
                    ? TrimProjection.project(piece.layer(), f.part(), local, RestPose.directionToModel(f.normal()))
                    : null;
            List<Vec> placed = f.corners().stream().map(c -> c.minus(centre)).toList();
            p.add(placed, scale, new Vec(0.5, 0.5, 0.5), f.normal(), f, trim);
        }
        return p.done();
    }

    /** Packs faces into the arrays of a {@link Quads}. */
    private static final class Packer {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final float[] trimUvs;
        private final boolean[] material;
        private final boolean[] lit;
        private int n;

        Packer(int quads) {
            positions = new float[quads * 12];
            normals = new float[quads * 3];
            uvs = new float[quads * 8];
            trimUvs = new float[quads * 8];
            material = new boolean[quads];
            lit = new boolean[quads];
        }

        /** effects: adds f with its corners at {@code corners} times {@code scale} plus {@code offset}, facing {@code normal} */
        void add(List<Vec> corners, double scale, Vec offset, Vec normal, KitFace f, List<Uv> trim) {
            for (int k = 0; k < 4; k++) {
                Vec c = corners.get(k);
                positions[n * 12 + k * 3] = (float) (c.x() * scale + offset.x());
                positions[n * 12 + k * 3 + 1] = (float) (c.y() * scale + offset.y());
                positions[n * 12 + k * 3 + 2] = (float) (c.z() * scale + offset.z());
                uvs[n * 8 + k * 2] = (float) f.uvs().get(k).u();
                uvs[n * 8 + k * 2 + 1] = (float) f.uvs().get(k).v();
                trimUvs[n * 8 + k * 2] = trim == null ? Float.NaN : (float) trim.get(k).u();
                trimUvs[n * 8 + k * 2 + 1] = trim == null ? Float.NaN : (float) trim.get(k).v();
            }
            normals[n * 3] = (float) normal.x();
            normals[n * 3 + 1] = (float) normal.y();
            normals[n * 3 + 2] = (float) normal.z();
            material[n] = f.slot() == KitFace.Slot.MATERIAL;
            lit[n] = Materials.lit(f.material());
            n++;
        }

        Quads done() {
            return new Quads(n, positions, normals, uvs, trimUvs, material, lit);
        }
    }
}
