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

import java.util.Optional;

/**
 * A part of the game's humanoid body that a kit's cubes ride on, and so move with: its folder name
 * in a kit's Blockbench project, and its pivot as the game's humanoid model places it at rest, in
 * that model's own frame (y down from the neck, the face toward -z, the right arm toward -x).
 */
public enum Part {
    HEAD("head", new Vec(0, 0, 0)),
    BODY("body", new Vec(0, 0, 0)),
    RIGHT_ARM("right_arm", new Vec(-5, 2, 0)),
    LEFT_ARM("left_arm", new Vec(5, 2, 0)),
    RIGHT_LEG("right_leg", new Vec(-1.9, 12, 0)),
    LEFT_LEG("left_leg", new Vec(1.9, 12, 0));

    private final String folder;
    private final Vec pivot;

    Part(String folder, Vec pivot) {
        this.folder = folder;
        this.pivot = pivot;
    }

    /** effects: returns the folder name that binds cubes to this part */
    public String folder() {
        return folder;
    }

    /** effects: returns this part's pivot at rest, in the humanoid model's frame */
    public Vec pivot() {
        return pivot;
    }

    /** effects: returns the part named {@code folder}, if any */
    public static Optional<Part> byFolder(String folder) {
        for (Part p : values()) {
            if (p.folder.equals(folder)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
