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
import com.chunkworks.survivalistarmor.KitRemovalRecipe;
import com.chunkworks.survivalistarmor.SurvivalistArmor;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A kit cut off with shears (D-0001).
 *
 * <p>Partitions: the grid (a kitted piece and shears, in either order; a piece without a kit; a third
 * thing); the shears (fresh, one use from breaking).
 */
@GameTestHolder(SurvivalistArmor.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KitRemovalGameTests {
    public KitRemovalGameTests() {}

    private static final String TEMPLATE = "plot";

    @GameTest(template = TEMPLATE)
    public void shearsCutTheKitOffKeepingEverythingElse(GameTestHelper helper) {
        ItemStack piece = Pieces.worn(helper, Items.DIAMOND_CHESTPLATE);
        for (List<ItemStack> grid : List.of(List.of(Pieces.kitted(piece, Kit.BLUECOAT), new ItemStack(Items.SHEARS)),
                List.of(new ItemStack(Items.SHEARS), ItemStack.EMPTY, Pieces.kitted(piece, Kit.BLUECOAT)))) {
            var recipe = Pieces.crafting(helper, grid);
            helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof KitRemovalRecipe, "the removal recipe for " + grid);
            Pieces.same(helper, recipe.get().value().assemble(Pieces.grid(grid), helper.getLevel().registryAccess()), piece, "the piece as it was");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void theShearsComeBackAPointWorse(GameTestHelper helper) {
        CraftingInput grid = Pieces.grid(List.of(Pieces.kitted(Pieces.worn(helper, Items.IRON_BOOTS), Kit.SCRAPPER), new ItemStack(Items.SHEARS)));
        NonNullList<ItemStack> left = Pieces.crafting(helper, List.of(grid.getItem(0), grid.getItem(1))).orElseThrow().value().getRemainingItems(grid);
        helper.assertTrue(left.get(1).is(Items.SHEARS), "the shears stay");
        helper.assertValueEqual(left.get(1).getDamageValue(), 1, "the shears' wear");
        helper.assertTrue(left.get(0).isEmpty(), "nothing else stays");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void shearsOneUseFromBreakingBreak(GameTestHelper helper) {
        ItemStack worn = new ItemStack(Items.SHEARS);
        worn.setDamageValue(worn.getMaxDamage() - 1);
        CraftingInput grid = Pieces.grid(List.of(Pieces.kitted(new ItemStack(Items.IRON_HELMET), Kit.ROADPLATE), worn));
        NonNullList<ItemStack> left = Pieces.crafting(helper, List.of(grid.getItem(0), grid.getItem(1))).orElseThrow().value().getRemainingItems(grid);
        helper.assertTrue(left.get(1).isEmpty(), "the shears break");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aPieceWithoutAKitIsNotCut(GameTestHelper helper) {
        helper.assertTrue(Pieces.crafting(helper, List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.SHEARS))).isEmpty(), "no recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void anythingElseInTheGridSpoilsIt(GameTestHelper helper) {
        List<ItemStack> grid = List.of(Pieces.kitted(new ItemStack(Items.IRON_HELMET), Kit.ROADPLATE), new ItemStack(Items.SHEARS), new ItemStack(Items.STICK));
        helper.assertTrue(Pieces.crafting(helper, grid).isEmpty(), "no recipe");
        helper.succeed();
    }
}
