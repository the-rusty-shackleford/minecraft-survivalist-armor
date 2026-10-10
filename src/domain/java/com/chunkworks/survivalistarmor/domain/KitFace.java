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

import java.util.List;

/**
 * One face of one cube of a kit, as its Blockbench project holds it: in the project's own frame,
 * where the figure stands on y 0 facing +z (south) with its right arm toward -x, in sixteenths of a
 * block.
 *
 * <p>AF: the quadrilateral {@code corners}, wound counter-clockwise seen from outside, of cube
 * number {@code cube}, riding on {@code part} of {@code piece}, made of {@code material}, textured
 * from the kit's {@code slot} texture at {@code uvs}, corner for corner; {@code normal} points out
 * of the cube; {@code box} is the cube's extent when the cube is square to the axes, else null.
 * RI: corners and uvs hold four each and are unmodifiable; normal has length 1; part is one of
 * piece's parts.
 */
public record KitFace(Piece piece, Part part, int cube, String material, Slot slot, List<Vec> corners, List<Uv> uvs, Vec normal,
                      Box box) {
    public KitFace {
        corners = List.copyOf(corners);
        uvs = List.copyOf(uvs);
        if (corners.size() != 4 || uvs.size() != 4) {
            throw new IllegalArgumentException("a face has four corners and four texture coordinates");
        }
        if (!piece.parts().contains(part)) {
            throw new IllegalArgumentException(piece + " does not ride on " + part);
        }
    }

    /** The two textures a kit draws from: its garment, and the sheet of its base-material parts, coloured by the armour's material. */
    public enum Slot { GARMENT, MATERIAL }
}
