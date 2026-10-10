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
package com.chunkworks.survivalistarmor;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * The kits (D-0001, nfx's §6). Each is an item, a template for the smithing table, and a look: a
 * Blockbench project and its textures in {@code assets/survivalistarmor/survivalistarmor/kits/}. A
 * new kit is a constant here plus its data: its look, its recipes, its names.
 */
public enum Kit {
    /** Canvas work coat over cut hide, big collar, face rag and welding goggles. */
    DUSTER,
    /** Chain-link fence cut into panels and wired onto a work jacket; a half-face twin-cartridge respirator. */
    SCRAPPER,
    /** A breastplate cut out of a road sign, bolted over a padded undersuit; a welding visor. */
    ROADPLATE,
    /** Brass busbar, copper pipe and contacts from a substation, mounted on a worker's coat. */
    MAGPIE,
    /** A pre-collapse hardsuit in faded teal, with leather, webbing and wood slats lashed over it. */
    BLUECOAT,
    /** Black lames and scrap, lit orange lenses, a full gas mask, hazard shoulders. */
    BLACKLUNG,
    /** A sealed quilted chemical suit, boxy hood, round side filter, bedroll and canister on the back. */
    HAZMAT;

    /** effects: returns the kit's id, which a kitted piece carries: survivalistarmor:roadplate and the like */
    public ResourceLocation id() {
        return SurvivalistArmor.id(name().toLowerCase(Locale.ROOT));
    }

    /** effects: returns the registry name of the kit's item: roadplate_kit and the like */
    public String itemName() {
        return name().toLowerCase(Locale.ROOT) + "_kit";
    }
}
