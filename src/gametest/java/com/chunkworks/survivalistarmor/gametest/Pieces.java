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
package com.chunkworks.survivalistarmor.gametest;

import com.chunkworks.survivalistarmor.Kit;
import com.chunkworks.survivalistarmor.SurvivalistContent;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;

/** Pieces of armour for the tests, as a player's own might be, and the game's recipe lookups. */
final class Pieces {
    private Pieces() {}

    /** effects: returns a piece of {@code item} carrying what a player's own might: enchantments, wear, a name, a trim, an anvil cost */
    static ItemStack worn(GameTestHelper helper, Item item) {
        HolderLookup.Provider r = helper.getLevel().registryAccess();
        ItemStack piece = new ItemStack(item);
        piece.enchant(r.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 4);
        piece.enchant(r.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        piece.setDamageValue(37);
        piece.set(DataComponents.CUSTOM_NAME, Component.literal("Dusty"));
        piece.set(DataComponents.TRIM, new ArmorTrim(r.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
                r.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST)));
        piece.set(DataComponents.REPAIR_COST, 3);
        return piece;
    }

    /** effects: returns {@code piece} with {@code kit} set on it */
    static ItemStack kitted(ItemStack piece, Kit kit) {
        ItemStack out = piece.copy();
        out.set(SurvivalistContent.KIT.get(), kit.id());
        return out;
    }

    /** effects: returns the kit item of {@code kit} */
    static ItemStack kit(Kit kit) {
        return new ItemStack(SurvivalistContent.kitItem(kit));
    }

    /** effects: returns the smithing recipe the game would pick for these three, if any */
    static Optional<RecipeHolder<SmithingRecipe>> smithing(GameTestHelper helper, ItemStack template, ItemStack base, ItemStack addition) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, new SmithingRecipeInput(template, base, addition), helper.getLevel());
    }

    /** effects: returns the crafting recipe the game would pick for a grid of {@code items}, read row by row in a 3x3 grid */
    static Optional<RecipeHolder<CraftingRecipe>> crafting(GameTestHelper helper, List<ItemStack> items) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid(items), helper.getLevel());
    }

    /** effects: returns a 3x3 grid holding {@code items} in its first slots */
    static CraftingInput grid(List<ItemStack> items) {
        java.util.ArrayList<ItemStack> all = new java.util.ArrayList<>(items);
        while (all.size() < 9) {
            all.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, all);
    }

    /** effects: fails the test unless {@code actual} is {@code expected}: the same item, count and every component */
    static void same(GameTestHelper helper, ItemStack actual, ItemStack expected, String where) {
        helper.assertTrue(ItemStack.isSameItemSameComponents(actual, expected) && actual.getCount() == expected.getCount(),
                where + ": got " + actual + " " + actual.getComponentsPatch() + ", wanted " + expected + " " + expected.getComponentsPatch());
    }
}
