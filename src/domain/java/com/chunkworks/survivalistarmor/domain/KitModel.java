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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A kit's Blockbench project ({@code .bbmodel}), read into faces (D-0001). The project holds all
 * four pieces: its top folders are the pieces ({@code helmet}, {@code chestplate}, {@code leggings},
 * {@code boots}), the next the body parts each piece's cubes ride on ({@code head}, {@code body},
 * {@code right_arm}, {@code left_arm}, {@code right_leg}, {@code left_leg}); deeper folders are the
 * author's own. A cube anywhere else is skipped and named in the warnings.
 *
 * <p>The frame is the project's: the figure stands on y 0 facing +z with its right arm toward -x,
 * as a Blockbench "Generic" project shows it. Cubes keep their inflate and their rotation about
 * their origin (X, then Y, then Z, as Blockbench turns them, the way Vanilla Wheels reads its
 * projects), and a folder's rotation about its origin turns everything in it. Texture coordinates
 * are per face or the classic box layout, counted in the texture's own UV size where it has one
 * (Blockbench's per-texture size) and in the project's resolution otherwise; a face with no texture
 * is not drawn. The project's two textures are {@code garment} and {@code material}.
 *
 * <p>AF: the faces {@code faces}, textured from images {@code textureWidth} by
 * {@code textureHeight} pixels. RI: faces is unmodifiable; both sizes are positive.
 */
public final class KitModel {
    private static final String[] DIRECTIONS = {"north", "south", "east", "west", "up", "down"};
    private static final double FLAT = 1e-9;

    private final List<KitFace> faces;
    private final int textureWidth;
    private final int textureHeight;

    private KitModel(List<KitFace> faces, int textureWidth, int textureHeight) {
        this.faces = List.copyOf(faces);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    /** What a project read into: the model, and what was skipped and why. */
    public record Parsed(KitModel model, List<String> warnings) {
        public Parsed {
            warnings = List.copyOf(warnings);
        }
    }

    public List<KitFace> faces() {
        return faces;
    }

    /** effects: returns the faces of {@code piece}, in project order */
    public List<KitFace> faces(Piece piece) {
        return faces.stream().filter(f -> f.piece() == piece).toList();
    }

    /** effects: returns the width in pixels of the images the project's coordinates count in */
    public int textureWidth() {
        return textureWidth;
    }

    public int textureHeight() {
        return textureHeight;
    }

    /**
     * effects: returns the project's faces and the warnings for what it skipped<br>
     * throws: {@link IllegalArgumentException} when the text is not a Blockbench project, or when
     * no cube of it rides on a piece's part
     */
    public static Parsed parse(String text) {
        Object root = Json.parse(text);
        List<Object> elements = Json.list(Json.get(root, "elements"));
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("no elements: not a Blockbench project");
        }
        double resW = Json.number(Json.get(Json.get(root, "resolution"), "width"), 16);
        double resH = Json.number(Json.get(Json.get(root, "resolution"), "height"), 16);
        if (resW <= 0 || resH <= 0) {
            throw new IllegalArgumentException("a texture resolution must be positive");
        }
        List<String> warnings = new ArrayList<>();

        // Each texture's slot, and the size its faces' coordinates count in: its own UV size where it has one
        // (Blockbench's per-texture "uv_width" and "uv_height"), else the project's resolution.
        List<Sheet> sheets = new ArrayList<>();
        for (Object t : Json.list(Json.get(root, "textures"))) {
            String name = Json.string(Json.get(t, "name"), "texture");
            int dot = name.lastIndexOf('.');
            String bare = dot > 0 ? name.substring(0, dot) : name;
            KitFace.Slot slot = KitFace.Slot.GARMENT;
            if (bare.equals("material")) {
                slot = KitFace.Slot.MATERIAL;
            } else if (!bare.equals("garment")) {
                warnings.add("texture " + name + " is neither garment nor material; drawn as the garment");
            }
            double w = Json.number(Json.get(t, "uv_width"), resW), h = Json.number(Json.get(t, "uv_height"), resH);
            sheets.add(new Sheet(slot, w > 0 ? w : resW, h > 0 ? h : resH));
        }

        // The outliner: each element's folder path and the turns of the folders around it, innermost
        // first. A format-5 project keeps its folders in a "groups" list keyed by uuid; an older one in
        // the outliner's nodes.
        Map<String, Object> groups = new HashMap<>();
        for (Object g : Json.list(Json.get(root, "groups"))) {
            groups.put(Json.string(Json.get(g, "uuid"), ""), g);
        }
        Map<String, Placement> placements = new HashMap<>();
        walk(Json.list(Json.get(root, "outliner")), List.of(), List.of(), placements, groups);

        List<KitFace> faces = new ArrayList<>();
        int cube = 0;
        for (Object e : elements) {
            String name = Json.string(Json.get(e, "name"), "cube");
            if (!Json.bool(Json.get(e, "visibility"), true)) {
                continue;
            }
            String type = Json.string(Json.get(e, "type"), "cube");
            if (!type.equals("cube")) {
                warnings.add(name + ": a " + type + " element, skipped (kits are cubes)");
                continue;
            }
            Placement at = placements.getOrDefault(Json.string(Json.get(e, "uuid"), ""), new Placement(List.of(), List.of()));
            Piece piece = at.path().isEmpty() ? null : Piece.byFolder(at.path().get(0)).orElse(null);
            Part part = at.path().size() < 2 ? null : Part.byFolder(at.path().get(1)).orElse(null);
            if (piece == null || part == null || !piece.parts().contains(part)) {
                warnings.add(name + ": in " + String.join("/", at.path()) + ", not in a piece's part folder; skipped");
                continue;
            }
            cube(e, name, cube++, piece, part, at.turns(), new Sheet(KitFace.Slot.GARMENT, resW, resH), sheets, faces);
        }
        if (faces.isEmpty()) {
            throw new IllegalArgumentException("no faces: no cube rides on a piece's part");
        }
        return new Parsed(new KitModel(faces, (int) resW, (int) resH), warnings);
    }

    /** A texture: the slot it fills, and the size its faces' coordinates count in. */
    private record Sheet(KitFace.Slot slot, double width, double height) {}

    /** Where an element sits: its folder names from the top, and the folders' turns about their origins, innermost first. */
    private record Placement(List<String> path, List<Turn> turns) {}

    /** A turn about {@code origin}, in degrees about X, then Y, then Z. */
    private record Turn(double[] origin, double[] degrees) {
        boolean none() {
            return degrees[0] == 0 && degrees[1] == 0 && degrees[2] == 0;
        }
    }

    private static void walk(List<Object> nodes, List<String> path, List<Turn> turns, Map<String, Placement> out, Map<String, Object> groups) {
        for (Object n : nodes) {
            if (n instanceof String uuid) {
                out.put(uuid, new Placement(path, turns));
                continue;
            }
            Object g = groups.getOrDefault(Json.string(Json.get(n, "uuid"), ""), n);
            String name = Json.string(Json.get(g, "name"), Json.string(Json.get(n, "name"), "group"));
            Turn turn = new Turn(Json.numbers(Json.get(g, "origin"), 0, 0, 0), Json.numbers(Json.get(g, "rotation"), 0, 0, 0));
            List<String> inner = new ArrayList<>(path);
            inner.add(name);
            List<Turn> innerTurns = new ArrayList<>();
            if (!turn.none()) {
                innerTurns.add(turn);
            }
            innerTurns.addAll(turns);
            walk(Json.list(Json.get(n, "children")), List.copyOf(inner), List.copyOf(innerTurns), out, groups);
        }
    }

    private static void cube(Object e, String name, int cube, Piece piece, Part part, List<Turn> folders, Sheet project,
                             List<Sheet> sheets, List<KitFace> faces) {
        double[] from = Json.numbers(Json.get(e, "from"), 0, 0, 0);
        double[] to = Json.numbers(Json.get(e, "to"), 1, 1, 1);
        double inflate = Json.number(Json.get(e, "inflate"), 0);
        double x0 = Math.min(from[0], to[0]) - inflate, x1 = Math.max(from[0], to[0]) + inflate;
        double y0 = Math.min(from[1], to[1]) - inflate, y1 = Math.max(from[1], to[1]) + inflate;
        double z0 = Math.min(from[2], to[2]) - inflate, z1 = Math.max(from[2], to[2]) + inflate;
        Turn own = new Turn(Json.numbers(Json.get(e, "origin"), 0, 0, 0), Json.numbers(Json.get(e, "rotation"), 0, 0, 0));
        boolean square = own.none() && folders.isEmpty();
        Box box = square ? new Box(new Vec(x0, y0, z0), new Vec(x1, y1, z1)) : null;
        String material = Materials.of(name);

        Map<String, Object> faceMap = Json.map(Json.get(e, "faces"));
        boolean boxUv = Json.bool(Json.get(e, "box_uv"), false);
        double[] uvOffset = Json.numbers(Json.get(e, "uv_offset"), 0, 0);
        for (String dir : DIRECTIONS) {
            Object f = faceMap.get(dir);
            Object tex = Json.get(f, "texture");
            if (f == null || tex == null) {
                continue; // no face, or a face Blockbench draws without a texture: not drawn
            }
            int index = (int) Json.number(tex, 0);
            Sheet sheet = index >= 0 && index < sheets.size() ? sheets.get(index) : project;
            double resW = sheet.width(), resH = sheet.height();
            double[] uv = boxUv ? boxUvOf(dir, uvOffset, x1 - x0, y1 - y0, z1 - z0) : Json.numbers(Json.get(f, "uv"), 0, 0, resW, resH);
            int turn = ((int) Math.round(Json.number(Json.get(f, "rotation"), 0) / 90.0)) & 3;
            // Corners seen from outside: top-left, top-right, bottom-right, bottom-left.
            Vec[] c = switch (dir) {
                case "north" -> new Vec[] {new Vec(x1, y1, z0), new Vec(x0, y1, z0), new Vec(x0, y0, z0), new Vec(x1, y0, z0)};
                case "south" -> new Vec[] {new Vec(x0, y1, z1), new Vec(x1, y1, z1), new Vec(x1, y0, z1), new Vec(x0, y0, z1)};
                case "east" -> new Vec[] {new Vec(x1, y1, z1), new Vec(x1, y1, z0), new Vec(x1, y0, z0), new Vec(x1, y0, z1)};
                case "west" -> new Vec[] {new Vec(x0, y1, z0), new Vec(x0, y1, z1), new Vec(x0, y0, z1), new Vec(x0, y0, z0)};
                case "up" -> new Vec[] {new Vec(x0, y1, z0), new Vec(x1, y1, z0), new Vec(x1, y1, z1), new Vec(x0, y1, z1)};
                default -> new Vec[] {new Vec(x0, y0, z1), new Vec(x1, y0, z1), new Vec(x1, y0, z0), new Vec(x0, y0, z0)};
            };
            // Wound the other way round -- top-left, bottom-left, bottom-right, top-right -- so that, seen from
            // outside, the corners run counter-clockwise, as the game's block faces do. As the game's FaceBakery
            // does, corner i takes the texture rectangle's corner i + turn in the same order, so a face turned 90
            // shows its texture turned clockwise, as Minecraft and Blockbench show it.
            Vec[] ccw = {c[0], c[3], c[2], c[1]};
            Uv[] t = {new Uv(uv[0] / resW, uv[1] / resH), new Uv(uv[0] / resW, uv[3] / resH), new Uv(uv[2] / resW, uv[3] / resH),
                    new Uv(uv[2] / resW, uv[1] / resH)};
            List<Vec> corners = new ArrayList<>(4);
            List<Uv> uvs = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                corners.add(place(ccw[i], own, folders));
                uvs.add(t[(i + turn) & 3]);
            }
            Vec n = corners.get(1).minus(corners.get(0)).cross(corners.get(3).minus(corners.get(0)));
            if (n.length() < FLAT) {
                continue; // a side of a flat cube: nothing to draw
            }
            faces.add(new KitFace(piece, part, cube, material, sheet.slot(), corners, uvs, n.unit(), box));
        }
    }

    /** effects: returns {@code p} turned by the cube's own rotation, then by each folder's, innermost first */
    private static Vec place(Vec p, Turn own, List<Turn> folders) {
        Vec q = rotate(p, own);
        for (Turn t : folders) {
            q = rotate(q, t);
        }
        return q;
    }

    /** Blockbench's box UV: the classic entity layout from the offset, the sides in a row under the top and bottom. */
    private static double[] boxUvOf(String dir, double[] off, double w, double h, double d) {
        double u = off[0], v = off[1];
        return switch (dir) {
            case "up" -> new double[] {u + d, v, u + d + w, v + d};
            case "down" -> new double[] {u + d + w, v + d, u + d + w + w, v};
            case "east" -> new double[] {u, v + d, u + d, v + d + h};
            case "north" -> new double[] {u + d, v + d, u + d + w, v + d + h};
            case "west" -> new double[] {u + d + w, v + d, u + d + w + d, v + d + h};
            default -> new double[] {u + d + w + d, v + d, u + d + w + d + w, v + d + h};
        };
    }

    /** effects: returns {@code p} turned about the turn's origin by its degrees: X, then Y, then Z */
    static Vec rotate(Vec p, Turn turn) {
        double[] r = turn.degrees();
        if (turn.none()) {
            return p;
        }
        double[] o = turn.origin();
        double x = p.x() - o[0], y = p.y() - o[1], z = p.z() - o[2];
        double a = Math.toRadians(r[0]);
        double y1 = y * Math.cos(a) - z * Math.sin(a), z1 = y * Math.sin(a) + z * Math.cos(a);
        y = y1;
        z = z1;
        double b = Math.toRadians(r[1]);
        double x1 = x * Math.cos(b) + z * Math.sin(b), z2 = -x * Math.sin(b) + z * Math.cos(b);
        x = x1;
        z = z2;
        double g = Math.toRadians(r[2]);
        double x2 = x * Math.cos(g) - y * Math.sin(g), y2 = x * Math.sin(g) + y * Math.cos(g);
        return new Vec(x2 + o[0], y2 + o[1], z + o[2]);
    }
}
