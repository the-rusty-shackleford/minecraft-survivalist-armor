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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A kitted piece's tooltip names its kit as a trimmed piece names its trim (D-0001): under the
 * "Upgrade:" heading, after the trim's own lines if it has a trim. The piece's own name never
 * changes.
 */
final class KitTooltip {
    private KitTooltip() {}

    private static final String UPGRADE = "item.minecraft.smithing_template.upgrade";
    /** The dust of the reference: warm tan. */
    private static final int COLOUR = 0xB8A27A;

    static void add(ItemTooltipEvent event) {
        ResourceLocation kit = event.getItemStack().get(SurvivalistContent.KIT.get());
        if (kit == null) {
            return;
        }
        List<Component> lines = event.getToolTip();
        Component line = CommonComponents.space().append(Component.translatable("kit." + kit.getNamespace() + "." + kit.getPath()).withColor(COLOUR));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getContents() instanceof TranslatableContents t && t.getKey().equals(UPGRADE)) {
                // The trim's heading, its pattern, its material: the kit goes after them.
                lines.add(Math.min(i + 3, lines.size()), line);
                return;
            }
        }
        int at = Math.min(1, lines.size());
        lines.add(at, line);
        lines.add(at, Component.translatable(UPGRADE).withStyle(ChatFormatting.GRAY));
    }
}
