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

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What the mod registers (D-0001): the kit component on a piece of armour, the kits, the two recipe
 * types, and a creative tab. The kit on a piece is one data component holding the kit's id and
 * nothing else: the piece keeps its item, its stats, its name and every other component. Removed
 * from a world, the mod's component is dropped as the stack loads and the piece is kept (D-0001,
 * "Uninstalling").
 */
public final class SurvivalistContent {
    private SurvivalistContent() {}

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SurvivalistArmor.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SurvivalistArmor.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SurvivalistArmor.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SurvivalistArmor.MOD_ID);

    /** The kit a piece is drawn as: {@code survivalistarmor:roadplate} and the like. Saved, and sent to clients with the stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> KIT =
            COMPONENTS.registerComponentType("kit", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    private static final Map<Kit, DeferredItem<KitTemplateItem>> KIT_ITEMS = new EnumMap<>(Kit.class);

    static {
        for (Kit kit : Kit.values()) {
            KIT_ITEMS.put(kit, ITEMS.register(kit.itemName(), () -> new KitTemplateItem(kit)));
        }
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KitSmithingRecipe>> KIT_SMITHING =
            SERIALIZERS.register("kit_smithing", () -> KitSmithingRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KitRemovalRecipe>> KIT_REMOVAL =
            SERIALIZERS.register("kit_removal", () -> KitRemovalRecipe.SERIALIZER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("kits", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.survivalistarmor"))
            .icon(() -> new ItemStack(kitItem(Kit.ROADPLATE)))
            .displayItems((params, out) -> KIT_ITEMS.values().forEach(i -> out.accept(i.get())))
            .build());

    /** effects: returns {@code kit}'s item */
    public static KitTemplateItem kitItem(Kit kit) {
        return KIT_ITEMS.get(kit).get();
    }

    /** effects: registers the mod's content on {@code modBus} */
    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        SERIALIZERS.register(modBus);
        TABS.register(modBus);
    }
}
