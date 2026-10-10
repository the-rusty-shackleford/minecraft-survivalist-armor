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

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * A kit, as the smithing table takes it (D-0001). Being a smithing template is what gives it the
 * table's ghost icons, its "Applies to / Ingredients" lines, and the table's preview of the
 * kitted piece on its armour stand.
 */
public final class KitTemplateItem extends SmithingTemplateItem {
    private static final List<ResourceLocation> ARMOUR_SLOTS = List.of(
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"));
    private static final List<ResourceLocation> SCRAP_SLOT = List.of(SurvivalistArmor.id("item/empty_slot_scrap"));

    private final Kit kit;

    public KitTemplateItem(Kit kit) {
        super(Component.translatable("item.survivalistarmor.kit.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.survivalistarmor." + kit.itemName() + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("upgrade.survivalistarmor.kit").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.survivalistarmor.kit.base_slot_description"),
                Component.translatable("item.survivalistarmor." + kit.itemName() + ".additions_slot_description"),
                ARMOUR_SLOTS, SCRAP_SLOT);
        this.kit = kit;
    }

    /** effects: returns the kit this item applies */
    public Kit kit() {
        return kit;
    }
}
