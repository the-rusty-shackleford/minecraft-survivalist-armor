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

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * A kit cut off (D-0001): a kitted piece of armour and a pair of shears in the crafting grid, nothing
 * else, make the piece as it was before the kit -- every other component kept, enchantments
 * included, which is why this is not the grindstone. The shears come back a point the worse for it.
 */
public final class KitRemovalRecipe extends CustomRecipe {
    public static final RecipeSerializer<KitRemovalRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(KitRemovalRecipe::new);

    public KitRemovalRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int[] at = find(input);
        if (at == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = input.getItem(at[0]).copyWithCount(1);
        out.remove(SurvivalistContent.KIT.get());
        return out;
    }

    /** effects: returns what stays in the grid: the shears, a point more worn, or nothing if that breaks them */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = super.getRemainingItems(input);
        int[] at = find(input);
        if (at != null) {
            ItemStack shears = input.getItem(at[1]).copyWithCount(1);
            if (shears.isDamageableItem()) {
                int worn = shears.getDamageValue() + 1;
                shears = worn >= shears.getMaxDamage() ? ItemStack.EMPTY : shears;
                if (!shears.isEmpty()) {
                    shears.setDamageValue(worn);
                }
            }
            left.set(at[1], shears);
        }
        return left;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    /** effects: returns {kitted piece's slot, shears' slot} if the grid holds exactly those two things, else null */
    private static int[] find(CraftingInput input) {
        int piece = -1, shears = -1;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (piece < 0 && s.getItem() instanceof ArmorItem && s.has(SurvivalistContent.KIT.get())) {
                piece = i;
            } else if (shears < 0 && (s.is(Tags.Items.TOOLS_SHEAR) || s.is(Items.SHEARS))) {
                shears = i;
            } else {
                return null;
            }
        }
        return piece >= 0 && shears >= 0 ? new int[] {piece, shears} : null;
    }
}
