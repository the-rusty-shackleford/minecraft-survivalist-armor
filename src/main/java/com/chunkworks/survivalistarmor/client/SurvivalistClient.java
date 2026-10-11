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
package com.chunkworks.survivalistarmor.client;

import com.chunkworks.survivalistarmor.SurvivalistArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/** The client's registrations: the kits' reload and their atlas, and forgetting sprites when an atlas they draw from is stitched anew. */
@EventBusSubscriber(modid = SurvivalistArmor.MOD_ID, value = Dist.CLIENT)
public final class SurvivalistClient {
    private SurvivalistClient() {}

    @SubscribeEvent
    public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(KitLibrary.INSTANCE);
        event.registerReloadListener(KitAtlas.create(Minecraft.getInstance().getTextureManager()));
    }

    /**
     * A new atlas holds new sprites: whatever was looked up or baked from the old one is forgotten. The
     * kits draw from their own atlas, and a kitted item's particle icon is its armour's, on the block atlas.
     */
    @SubscribeEvent
    public static void onStitched(TextureAtlasStitchedEvent event) {
        ResourceLocation atlas = event.getAtlas().location();
        if (atlas.equals(KitAtlas.LOCATION) || atlas.equals(InventoryMenu.BLOCK_ATLAS)) {
            KitSkins.clear();
            KitItemModels.clear();
        }
    }
}
