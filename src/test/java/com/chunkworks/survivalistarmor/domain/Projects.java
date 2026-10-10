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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Blockbench projects written for tests: cubes in folders, as Blockbench saves them (format 5: the
 * folders in a "groups" list keyed by uuid, the outliner naming them by uuid), or the older layout
 * with each folder's name in its outliner node.
 */
final class Projects {
    private Projects() {}

    /** A cube: its name, corners, and per face either a texture rectangle or nothing; or box UV from an offset. */
    static final class Cube {
        final String name;
        final double[] from;
        final double[] to;
        double inflate;
        double[] origin = {0, 0, 0};
        double[] rotation = {0, 0, 0};
        int texture;
        boolean boxUv;
        double[] uvOffset = {0, 0};
        boolean visible = true;
        final Map<String, double[]> uvs = new LinkedHashMap<>();
        final Map<String, Integer> turns = new LinkedHashMap<>();
        final List<String> blank = new ArrayList<>();

        Cube(String name, double x0, double y0, double z0, double x1, double y1, double z1) {
            this.name = name;
            this.from = new double[] {x0, y0, z0};
            this.to = new double[] {x1, y1, z1};
            for (String d : new String[] {"north", "south", "east", "west", "up", "down"}) {
                uvs.put(d, new double[] {0, 0, 4, 4});
            }
        }

        Cube inflate(double v) {
            inflate = v;
            return this;
        }

        Cube turned(double ox, double oy, double oz, double rx, double ry, double rz) {
            origin = new double[] {ox, oy, oz};
            rotation = new double[] {rx, ry, rz};
            return this;
        }

        Cube texture(int index) {
            texture = index;
            return this;
        }

        Cube uv(String face, double u1, double v1, double u2, double v2) {
            uvs.put(face, new double[] {u1, v1, u2, v2});
            return this;
        }

        Cube turn(String face, int degrees) {
            turns.put(face, degrees);
            return this;
        }

        Cube untextured(String face) {
            blank.add(face);
            return this;
        }

        Cube boxUv(double u, double v) {
            boxUv = true;
            uvOffset = new double[] {u, v};
            return this;
        }

        Cube hidden() {
            visible = false;
            return this;
        }
    }

    /** A folder: its name, its turn, and what it holds (cubes and folders). */
    static final class Folder {
        final String name;
        double[] origin = {0, 0, 0};
        double[] rotation = {0, 0, 0};
        final List<Object> children = new ArrayList<>();

        Folder(String name, Object... children) {
            this.name = name;
            this.children.addAll(List.of(children));
        }

        Folder turned(double ox, double oy, double oz, double rx, double ry, double rz) {
            origin = new double[] {ox, oy, oz};
            rotation = new double[] {rx, ry, rz};
            return this;
        }
    }

    /** effects: returns a format-5 project of {@code top}, its textures named {@code textures}, {@code w} by {@code h} */
    static String project(int w, int h, List<String> textures, Object... top) {
        return write(w, h, textures, true, List.of(top), List.of());
    }

    /** effects: as {@link #project}, with each folder's name in its outliner node, as older projects keep it */
    static String legacy(int w, int h, List<String> textures, Object... top) {
        return write(w, h, textures, false, List.of(top), List.of());
    }

    /** effects: a format-5 project with extra raw elements (a mesh, say) outside every folder */
    static String withRaw(int w, int h, List<String> textures, List<String> raw, Object... top) {
        return write(w, h, textures, true, List.of(top), raw);
    }

    private static String write(int w, int h, List<String> textures, boolean format5, List<Object> top, List<String> raw) {
        List<String> elements = new ArrayList<>(raw);
        List<String> groups = new ArrayList<>();
        int[] next = {0};
        String outliner = outline(top, elements, groups, format5, next);
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < textures.size(); i++) {
            t.append(i == 0 ? "" : ",").append("{\"name\":\"").append(textures.get(i)).append("\",\"width\":").append(w).append(",\"height\":").append(h).append('}');
        }
        return "{\"meta\":{\"format_version\":\"" + (format5 ? "5.0" : "4.5") + "\"},\"resolution\":{\"width\":" + w + ",\"height\":" + h + "},"
                + "\"elements\":[" + String.join(",", elements) + "],"
                + (format5 ? "\"groups\":[" + String.join(",", groups) + "]," : "")
                + "\"outliner\":" + outliner + ",\"textures\":[" + t + "]}";
    }

    private static String outline(List<Object> nodes, List<String> elements, List<String> groups, boolean format5, int[] next) {
        List<String> out = new ArrayList<>();
        for (Object n : nodes) {
            String uuid = "u" + next[0]++;
            if (n instanceof Cube c) {
                elements.add(element(c, uuid));
                out.add('"' + uuid + '"');
            } else {
                Folder f = (Folder) n;
                String children = outline(f.children, elements, groups, format5, next);
                String turn = "\"origin\":" + nums(f.origin) + ",\"rotation\":" + nums(f.rotation);
                if (format5) {
                    groups.add("{\"name\":\"" + f.name + "\",\"uuid\":\"" + uuid + "\"," + turn + "}");
                    out.add("{\"uuid\":\"" + uuid + "\",\"isOpen\":true,\"children\":" + children + "}");
                } else {
                    out.add("{\"name\":\"" + f.name + "\",\"uuid\":\"" + uuid + "\"," + turn + ",\"children\":" + children + "}");
                }
            }
        }
        return "[" + String.join(",", out) + "]";
    }

    private static String element(Cube c, String uuid) {
        StringBuilder faces = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, double[]> e : c.uvs.entrySet()) {
            faces.append(first ? "" : ",").append('"').append(e.getKey()).append("\":{\"uv\":").append(nums(e.getValue()));
            faces.append(",\"texture\":").append(c.blank.contains(e.getKey()) ? "null" : String.valueOf(c.texture));
            if (c.turns.containsKey(e.getKey())) {
                faces.append(",\"rotation\":").append(c.turns.get(e.getKey()));
            }
            faces.append('}');
            first = false;
        }
        return "{\"name\":\"" + c.name + "\",\"type\":\"cube\",\"uuid\":\"" + uuid + "\",\"visibility\":" + c.visible
                + ",\"from\":" + nums(c.from) + ",\"to\":" + nums(c.to) + ",\"inflate\":" + c.inflate
                + ",\"origin\":" + nums(c.origin) + ",\"rotation\":" + nums(c.rotation)
                + ",\"box_uv\":" + c.boxUv + ",\"uv_offset\":" + nums(c.uvOffset) + ",\"faces\":{" + faces + "}}";
    }

    private static String nums(double[] v) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            b.append(i == 0 ? "" : ",").append(String.format(Locale.ROOT, "%s", v[i]));
        }
        return b.append(']').toString();
    }

    /** effects: a chestplate's body folder holding {@code cubes}, the usual first test */
    static Folder chestBody(Object... cubes) {
        return new Folder("chestplate", new Folder("body", cubes));
    }
}
