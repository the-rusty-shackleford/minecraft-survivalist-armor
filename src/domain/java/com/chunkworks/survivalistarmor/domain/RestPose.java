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

/**
 * From a kit project's frame to the frame of the body part a cube rides on.
 *
 * <p>A project stands its figure on y 0, its neck at y 24, facing +z with its right arm toward -x:
 * the world as the figure would stand in it facing south. The game's humanoid model has its own
 * frame, y down from the neck and the face toward -z, which is the world's turned half round the x
 * axis; each part is drawn from its pivot, so a point rides on a part as its offset from the pivot.
 * The game moves the part (walking, sneaking, swimming, a baby's scale) and the cube moves with it.
 */
public final class RestPose {
    private RestPose() {}

    /** The project's y of the humanoid model's origin, the neck. */
    public static final double NECK = 24.0;

    /** effects: returns the project point {@code p} as an offset from {@code part}'s pivot, in the humanoid model's frame */
    public static Vec toPart(Part part, Vec p) {
        return toModel(p).minus(part.pivot());
    }

    /** effects: returns the project point {@code p} in the humanoid model's frame */
    public static Vec toModel(Vec p) {
        return new Vec(p.x(), NECK - p.y(), -p.z());
    }

    /** effects: returns the project direction {@code d} in the humanoid model's frame */
    public static Vec directionToModel(Vec d) {
        return new Vec(d.x(), -d.y(), -d.z());
    }
}
