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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Partitions: a material with an entry, without one; one file, several layered (a higher entry
 * replacing a lower, a higher file with or without a default); a bad file (not JSON, a blank name).
 */
class PaletteTableTest {
    private static final String BASE = "{\"default\":\"neutral\",\"materials\":{\"minecraft:iron\":\"iron\",\"minecraft:gold\":\"gold\"}}";

    @Test
    void aMaterialWithAnEntryTakesItsPalette() {
        assertEquals("iron", PaletteTable.parse(BASE).paletteFor("minecraft:iron"));
    }

    @Test
    void aMaterialWithoutOneTakesTheDefault() {
        assertEquals("neutral", PaletteTable.parse(BASE).paletteFor("galestone:galestone"));
    }

    @Test
    void aHigherFilesEntriesReplaceALowersAndTheRestRemain() {
        String pack = "{\"materials\":{\"minecraft:gold\":\"brass\",\"galestone:galestone\":\"galestone\"}}";
        PaletteTable t = PaletteTable.layered(List.of(BASE, pack));
        assertEquals("iron", t.paletteFor("minecraft:iron"));
        assertEquals("brass", t.paletteFor("minecraft:gold"));
        assertEquals("galestone", t.paletteFor("galestone:galestone"));
        assertEquals("neutral", t.fallback(), "a higher file without a default keeps the lower's");
    }

    @Test
    void theHighestFileNamingADefaultSetsIt() {
        PaletteTable t = PaletteTable.layered(List.of(BASE, "{\"default\":\"scrap\"}"));
        assertEquals("scrap", t.paletteFor("anything:else"));
    }

    @Test
    void aBadFileIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PaletteTable.parse("{"));
        assertThrows(IllegalArgumentException.class, () -> PaletteTable.parse("{\"materials\":{\"minecraft:iron\":\"\"}}"));
    }
}
