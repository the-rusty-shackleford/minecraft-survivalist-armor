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
import java.util.Optional;

/**
 * A piece of armour a kit dresses: its folder in a kit's Blockbench project, the body parts its cubes
 * may ride on, and which of the game's two armour layers it is drawn on, which decides the trim
 * texture it takes (the outer for a helmet, chestplate or boots; the inner for leggings).
 */
public enum Piece {
    HELMET("helmet", Layer.OUTER, List.of(Part.HEAD)),
    CHESTPLATE("chestplate", Layer.OUTER, List.of(Part.BODY, Part.RIGHT_ARM, Part.LEFT_ARM)),
    LEGGINGS("leggings", Layer.INNER, List.of(Part.BODY, Part.RIGHT_LEG, Part.LEFT_LEG)),
    BOOTS("boots", Layer.OUTER, List.of(Part.RIGHT_LEG, Part.LEFT_LEG));

    /** The game's two armour layers: the outer (grown a unit) and the inner (grown half a unit). */
    public enum Layer { OUTER, INNER }

    private final String folder;
    private final Layer layer;
    private final List<Part> parts;

    Piece(String folder, Layer layer, List<Part> parts) {
        this.folder = folder;
        this.layer = layer;
        this.parts = parts;
    }

    public String folder() {
        return folder;
    }

    public Layer layer() {
        return layer;
    }

    /** effects: returns the parts this piece's cubes may ride on */
    public List<Part> parts() {
        return parts;
    }

    /** effects: returns the piece named {@code folder}, if any */
    public static Optional<Piece> byFolder(String folder) {
        for (Piece p : values()) {
            if (p.folder.equals(folder)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
