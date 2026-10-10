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

/** A point or a direction in three dimensions, in sixteenths of a block unless said otherwise. Immutable. */
public record Vec(double x, double y, double z) {
    public static final Vec ZERO = new Vec(0, 0, 0);

    public Vec plus(Vec o) {
        return new Vec(x + o.x, y + o.y, z + o.z);
    }

    public Vec minus(Vec o) {
        return new Vec(x - o.x, y - o.y, z - o.z);
    }

    public Vec times(double k) {
        return new Vec(x * k, y * k, z * k);
    }

    public double dot(Vec o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec cross(Vec o) {
        return new Vec(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    /** requires: this is not zero. effects: returns this scaled to length 1 */
    public Vec unit() {
        double l = length();
        return new Vec(x / l, y / l, z / l);
    }
}
