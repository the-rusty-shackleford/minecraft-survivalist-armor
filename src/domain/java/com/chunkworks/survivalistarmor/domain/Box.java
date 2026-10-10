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
 * An axis-aligned box. Immutable.
 *
 * <p>RI: min.x <= max.x, min.y <= max.y, min.z <= max.z.
 */
public record Box(Vec min, Vec max) {
    public Box {
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("min past max: " + min + " " + max);
        }
    }

    /** effects: returns whether {@code p} lies in this box or within {@code slack} of it */
    public boolean contains(Vec p, double slack) {
        return p.x() >= min.x() - slack && p.x() <= max.x() + slack
                && p.y() >= min.y() - slack && p.y() <= max.y() + slack
                && p.z() >= min.z() - slack && p.z() <= max.z() + slack;
    }
}
