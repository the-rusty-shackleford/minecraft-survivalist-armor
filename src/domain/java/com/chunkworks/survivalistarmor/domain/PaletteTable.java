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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which palette a kit's material parts take on a piece of armour, by the piece's armour material
 * (D-0001: the base armour's own material colours them, so the Roadplate shows cut steel on iron and
 * teal composite on diamond). Read from {@code survivalistarmor/palettes.json} in every resource
 * pack, lowest first, a higher pack's entries replacing a lower's:
 * <pre>{"default": "neutral", "materials": {"minecraft:iron": "iron", "minecraft:gold": "gold"}}</pre>
 * A material with no entry takes the default, so modded armour always has a palette.
 *
 * <p>AF: material m takes palette {@code byMaterial(m)} if present, else {@code fallback}.
 * RI: byMaterial is unmodifiable; no name is blank.
 */
public final class PaletteTable {
    public static final PaletteTable EMPTY = new PaletteTable(Map.of(), "neutral");

    private final Map<String, String> byMaterial;
    private final String fallback;

    private PaletteTable(Map<String, String> byMaterial, String fallback) {
        this.byMaterial = Map.copyOf(byMaterial);
        this.fallback = fallback;
    }

    /**
     * effects: returns the table one file holds<br>
     * throws: {@link IllegalArgumentException} if the text is not JSON, or a name in it is blank
     */
    public static PaletteTable parse(String text) {
        Object root = Json.parse(text);
        String fallback = Json.string(Json.get(root, "default"), EMPTY.fallback);
        Map<String, String> entries = new HashMap<>();
        for (Map.Entry<String, Object> e : Json.map(Json.get(root, "materials")).entrySet()) {
            entries.put(e.getKey(), Json.string(e.getValue(), ""));
        }
        check(fallback);
        entries.forEach((k, v) -> {
            check(k);
            check(v);
        });
        return new PaletteTable(entries, fallback);
    }

    /**
     * effects: returns the tables {@code files} hold, lowest pack first, laid over one another: a
     * higher file's entries replace a lower's, and the highest file naming a default sets it<br>
     * throws: {@link IllegalArgumentException} as {@link #parse} does, for any file
     */
    public static PaletteTable layered(List<String> files) {
        Map<String, String> merged = new HashMap<>();
        String fallback = EMPTY.fallback;
        for (String text : files) {
            PaletteTable t = parse(text);
            merged.putAll(t.byMaterial);
            if (Json.get(Json.parse(text), "default") != null) {
                fallback = t.fallback;
            }
        }
        return new PaletteTable(merged, fallback);
    }

    /** effects: returns the palette of armour material {@code material} (a registry id such as minecraft:iron) */
    public String paletteFor(String material) {
        return byMaterial.getOrDefault(material, fallback);
    }

    /** effects: returns the palette of a material with no entry */
    public String fallback() {
        return fallback;
    }

    private static void check(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("a palette table names nothing blank");
        }
    }
}
