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

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The entry point. A kit, applied to a piece of armour at the smithing table, changes how the
 * piece is drawn and nothing else (D-0001).
 */
@Mod(SurvivalistArmor.MOD_ID)
public final class SurvivalistArmorMod {
    public SurvivalistArmorMod(IEventBus modBus, ModContainer container) {
        SurvivalistContent.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SurvivalistConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, SurvivalistConfig.CLIENT_SPEC);
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, KitTooltip::add);
    }
}
