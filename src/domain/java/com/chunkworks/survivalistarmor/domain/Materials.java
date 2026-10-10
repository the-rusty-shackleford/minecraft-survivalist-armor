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

import java.util.Locale;
import java.util.Set;

/**
 * What a cube's name says about how its faces are drawn. A cube's material is its name up to the
 * first dot, lower-cased: {@code plate.chest_left} is plate, {@code lens_lit.right} is lens_lit.
 * The art pipeline paints by the same names (D-0002).
 *
 * <ul>
 *   <li>A trim shows on the garment's surfaces and on its plates, the places a trim's pattern would
 *       sit on the game's own armour, and nowhere on hardware: lenses, hoses, canisters, filters,
 *       pouches, straps, buckles, rivets, tape (D-0001).</li>
 *   <li>A lit lens is drawn at full brightness, and is the one thing that glows.</li>
 *   <li>Chain-link mesh has holes in it: what is behind it shows through, so it hides nothing.</li>
 * </ul>
 */
public final class Materials {
    private Materials() {}

    private static final Set<String> TRIMMABLE = Set.of(
            "plate", "shell", "lames", "sheet", "panel",
            "canvas", "cloth", "coat", "hide", "leather", "padding", "quilt", "suit", "hood");

    private static final Set<String> LIT = Set.of("lens_lit");

    private static final Set<String> SEE_THROUGH = Set.of("mesh");

    /** effects: returns the material a cube named {@code cubeName} is made of */
    public static String of(String cubeName) {
        int dot = cubeName.indexOf('.');
        return (dot < 0 ? cubeName : cubeName.substring(0, dot)).trim().toLowerCase(Locale.ROOT);
    }

    /** effects: returns whether a trim shows on faces of {@code material} */
    public static boolean trimmable(String material) {
        return TRIMMABLE.contains(material);
    }

    /** effects: returns whether faces of {@code material} glow */
    public static boolean lit(String material) {
        return LIT.contains(material);
    }

    /** effects: returns whether what is behind a cube of {@code material} shows through it */
    public static boolean seeThrough(String material) {
        return SEE_THROUGH.contains(material);
    }
}
