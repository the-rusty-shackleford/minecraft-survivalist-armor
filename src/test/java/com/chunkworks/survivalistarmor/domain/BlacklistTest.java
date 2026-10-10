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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Partitions: an entry naming a mod, an item; an item of that mod, of another; case; blank entries; an id without a namespace. */
class BlacklistTest {
    @Test
    void aModsEntryRefusesEveryPieceOfIt() {
        Blacklist b = Blacklist.of(List.of("mowziesmobs"));
        assertTrue(b.refuses("mowziesmobs:wrought_helmet"));
        assertFalse(b.refuses("minecraft:iron_helmet"));
    }

    @Test
    void anItemsEntryRefusesThatPieceAlone() {
        Blacklist b = Blacklist.of(List.of("create:copper_backtank"));
        assertTrue(b.refuses("create:copper_backtank"));
        assertFalse(b.refuses("create:copper_diving_helmet"));
    }

    @Test
    void caseAndBlanksDoNotMatter() {
        Blacklist b = Blacklist.of(List.of(" Galestone ", "", "  "));
        assertTrue(b.refuses("GALESTONE:galestone_chestplate"));
    }

    @Test
    void anIdWithoutANamespaceIsTheGames() {
        assertTrue(Blacklist.of(List.of("minecraft:turtle_helmet")).refuses("turtle_helmet"));
        assertTrue(Blacklist.of(List.of("minecraft")).refuses("iron_boots"));
    }

    @Test
    void theEmptyListRefusesNothing() {
        assertFalse(Blacklist.NONE.refuses("minecraft:iron_chestplate"));
    }
}
