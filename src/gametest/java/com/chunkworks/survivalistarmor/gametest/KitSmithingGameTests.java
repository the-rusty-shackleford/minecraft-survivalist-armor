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
import com.chunkworks.survivalistarmor.KitSmithingRecipe;
import com.chunkworks.survivalistarmor.SurvivalistArmor;
import com.chunkworks.survivalistarmor.SurvivalistConfig;
import com.chunkworks.survivalistarmor.SurvivalistContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A kit applied at the smithing table (D-0001).
 *
 * <p>Partitions:
 * <ul>
 *   <li>the base: a worn piece (enchanted, damaged, named, trimmed); dyed leather; a piece with
 *       another kit; with this kit already; not armour (a sword, an elytra); armour the server
 *       refuses; armour of another slot (the turtle helmet);</li>
 *   <li>the addition: the kit's scrap; anything else;</li>
 *   <li>taking the result: the kit used up (the default), kept (the server's option).</li>
 * </ul>
 */
@GameTestHolder(SurvivalistArmor.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KitSmithingGameTests {
    public KitSmithingGameTests() {}

    private static final String TEMPLATE = "plot";
    private static final String REFUSING = "survivalistarmor_refusing";
    private static final String KEEPING = "survivalistarmor_keeping";

    @GameTest(template = TEMPLATE)
    public void aKitGoesOnAWornPieceKeepingEverythingElse(GameTestHelper helper) {
        ItemStack piece = Pieces.worn(helper, Items.IRON_CHESTPLATE);
        ItemStack out = apply(helper, Kit.ROADPLATE, piece, new ItemStack(Items.IRON_NUGGET));
        Pieces.same(helper, out, Pieces.kitted(piece, Kit.ROADPLATE), "the kitted piece");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aDyedLeatherPieceKeepsItsDye(GameTestHelper helper) {
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3366CC, true));
        ItemStack out = apply(helper, Kit.DUSTER, boots, new ItemStack(Items.STRING));
        Pieces.same(helper, out, Pieces.kitted(boots, Kit.DUSTER), "dyed boots");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void anotherKitTakesTheFirstOnesPlace(GameTestHelper helper) {
        ItemStack piece = Pieces.kitted(Pieces.worn(helper, Items.GOLDEN_HELMET), Kit.ROADPLATE);
        ItemStack out = apply(helper, Kit.MAGPIE, piece, new ItemStack(Items.COPPER_INGOT));
        helper.assertValueEqual(out.get(SurvivalistContent.KIT.get()), Kit.MAGPIE.id(), "the kit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void theSameKitAgainMakesNothing(GameTestHelper helper) {
        ItemStack piece = Pieces.kitted(Pieces.worn(helper, Items.IRON_CHESTPLATE), Kit.ROADPLATE);
        helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.ROADPLATE), piece, new ItemStack(Items.IRON_NUGGET)).isEmpty(), "no result");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void whatIsNotArmourIsRefused(GameTestHelper helper) {
        for (ItemStack base : List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.ELYTRA), new ItemStack(Items.STICK))) {
            helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.ROADPLATE), base, new ItemStack(Items.IRON_NUGGET)).isEmpty(), "refused: " + base);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void everySlotsArmourTakesAKit(GameTestHelper helper) {
        for (ItemStack base : List.of(new ItemStack(Items.TURTLE_HELMET), new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.CHAINMAIL_BOOTS))) {
            helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.BLACKLUNG), base, new ItemStack(Items.COAL)).isPresent(), "taken: " + base);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void theWrongScrapIsRefused(GameTestHelper helper) {
        helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.ROADPLATE), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.STRING)).isEmpty(),
                "string is not the Roadplate's scrap");
        helper.succeed();
    }

    @BeforeBatch(batch = REFUSING)
    public static void refuseIronChestplates(ServerLevel level) {
        SurvivalistConfig.BLACKLIST.set(List.of("minecraft:iron_chestplate"));
    }

    @AfterBatch(batch = REFUSING)
    public static void refuseAsConfigured(ServerLevel level) {
        SurvivalistConfig.BLACKLIST.set(SurvivalistConfig.BLACKLIST.getDefault());
    }

    @GameTest(template = TEMPLATE, batch = REFUSING)
    public void armourTheServerRefusesTakesNoKit(GameTestHelper helper) {
        helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.ROADPLATE), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_NUGGET)).isEmpty(),
                "the refused piece");
        helper.assertTrue(Pieces.smithing(helper, Pieces.kit(Kit.ROADPLATE), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.IRON_NUGGET)).isPresent(),
                "another piece");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void takingTheResultUsesUpTheKit(GameTestHelper helper) {
        SmithingMenu menu = table(helper);
        take(helper, menu);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "the kit used up");
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "the piece taken");
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "the scrap used up");
        helper.succeed();
    }

    @BeforeBatch(batch = KEEPING)
    public static void keepKits(ServerLevel level) {
        SurvivalistConfig.CONSUME_TEMPLATE.set(false);
    }

    @AfterBatch(batch = KEEPING)
    public static void useUpKits(ServerLevel level) {
        SurvivalistConfig.CONSUME_TEMPLATE.set(true);
    }

    @GameTest(template = TEMPLATE, batch = KEEPING)
    public void whereTheServerKeepsKitsTheKitStays(GameTestHelper helper) {
        SmithingMenu menu = table(helper);
        take(helper, menu);
        helper.assertTrue(menu.getSlot(0).getItem().is(SurvivalistContent.kitItem(Kit.ROADPLATE)), "the kit stays");
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "the scrap still used up");
        helper.succeed();
    }

    /** effects: returns what the smithing table makes of the three, failing the test if it makes nothing */
    private static ItemStack apply(GameTestHelper helper, Kit kit, ItemStack base, ItemStack addition) {
        var recipe = Pieces.smithing(helper, Pieces.kit(kit), base, addition);
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof KitSmithingRecipe, "a kit recipe for " + base);
        return recipe.get().value().assemble(new SmithingRecipeInput(Pieces.kit(kit), base, addition),
                helper.getLevel().registryAccess());
    }

    /** effects: returns a smithing table's menu holding a Roadplate kit, an iron chestplate and an iron nugget */
    private static SmithingMenu table(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, Blocks.SMITHING_TABLE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(at)));
        menu.getSlot(0).set(Pieces.kit(Kit.ROADPLATE));
        menu.getSlot(1).set(new ItemStack(Items.IRON_CHESTPLATE));
        menu.getSlot(2).set(new ItemStack(Items.IRON_NUGGET));
        menu.createResult();
        return menu;
    }

    /** effects: takes the table's result, as a player's click does */
    private static void take(GameTestHelper helper, SmithingMenu menu) {
        ItemStack result = menu.getSlot(3).getItem();
        helper.assertTrue(result.has(SurvivalistContent.KIT.get()), "a kitted result: " + result);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        menu.getSlot(3).onTake(player, menu.getSlot(3).remove(1));
    }
}
