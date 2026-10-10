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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The armour no kit goes on (D-0001), as the server's config lists it: a mod's id refuses every
 * piece of that mod ({@code geckolib_armor}), an item's id refuses that piece
 * ({@code create:copper_backtank}). Refused armour is turned away at the smithing table and, should
 * it carry a kit from before, drawn as itself.
 *
 * <p>AF: refuses item i iff i's namespace is in {@code mods} or i is in {@code items}.
 * RI: both sets unmodifiable and lower-case.
 */
public final class Blacklist {
    public static final Blacklist NONE = new Blacklist(Set.of(), Set.of());

    private final Set<String> mods;
    private final Set<String> items;

    private Blacklist(Set<String> mods, Set<String> items) {
        this.mods = Set.copyOf(mods);
        this.items = Set.copyOf(items);
    }

    /** effects: returns the blacklist {@code entries} make; blank entries are ignored */
    public static Blacklist of(List<String> entries) {
        Set<String> mods = new HashSet<>();
        Set<String> items = new HashSet<>();
        for (String e : entries) {
            String s = e.trim().toLowerCase(Locale.ROOT);
            if (s.isEmpty()) {
                continue;
            }
            (s.indexOf(':') < 0 ? mods : items).add(s);
        }
        return new Blacklist(mods, items);
    }

    /** effects: returns whether no kit goes on the item with registry id {@code itemId} (namespace:path) */
    public boolean refuses(String itemId) {
        String id = itemId.toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        return mods.contains(namespace) || items.contains(colon < 0 ? "minecraft:" + id : id);
    }
}
