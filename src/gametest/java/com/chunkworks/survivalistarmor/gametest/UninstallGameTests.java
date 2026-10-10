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

import com.chunkworks.survivalistarmor.SurvivalistArmor;
import com.chunkworks.survivalistarmor.SurvivalistContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A world the mod is taken out of (D-0001, "Uninstalling"): every piece of armour that wore a kit
 * loads again as the plain piece, its enchantments, wear, name and trim intact, and only the kit
 * is gone. A save is read here as the game reads it once the mod is removed: our component's key,
 * wherever it sits, renamed to one no registry holds.
 *
 * <p>Partitions. Each case is a kitted iron chestplate carrying every other component a player's
 * own piece might: enchantments, damage, a custom name, a trim, an anvil cost.
 * <ul>
 *   <li>Where the stack is saved: on its own; inside another item's component (a shulker box's
 *       contents, a bundle's); in a block entity (a chest); in an entity (an item frame, a dropped
 *       item); worn (an armour stand, a zombie); in a player's inventory (a main slot and the
 *       chest slot).</li>
 *   <li>How it is read: the game's own lenient read, which keeps the stack, or a strict one, which
 *       refuses the unknown key. The first case checks the strict one too, so the rename is known
 *       to bite.</li>
 * </ul>
 */
@GameTestHolder(SurvivalistArmor.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UninstallGameTests {
    public UninstallGameTests() {}

    private static final String TEMPLATE = "plot";
    private static final String KEY = SurvivalistArmor.id("kit").toString();
    /** A component key no registry holds: ours, as a world without the mod sees it. */
    private static final String GONE = "survivalistarmor_uninstalled:kit";

    @GameTest(template = TEMPLATE)
    public void aStackOnItsOwnKeepsAllButTheKit(GameTestHelper helper) {
        HolderLookup.Provider r = registries(helper);
        ItemStack kitted = kitted(helper);
        Tag saved = kitted.save(r);
        helper.assertTrue(saved.toString().contains(KEY), "the kit is saved with the stack: " + saved);
        Tag uninstalled = uninstalled(saved);
        helper.assertTrue(ItemStack.CODEC.parse(r.createSerializationContext(NbtOps.INSTANCE), uninstalled).result().isEmpty(),
                "a strict read refuses the unknown component, so the rename bites: " + uninstalled);
        same(helper, ItemStack.parse(r, uninstalled).orElse(ItemStack.EMPTY), plain(kitted), "the stack on its own");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aShulkerBoxsContentsKeepAllButTheKit(GameTestHelper helper) {
        HolderLookup.Provider r = registries(helper);
        ItemStack kitted = kitted(helper);
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(kitted.copy())));
        ItemStack loaded = ItemStack.parse(r, uninstalled(box.save(r))).orElse(ItemStack.EMPTY);
        helper.assertTrue(loaded.is(Items.SHULKER_BOX), "the box loads: " + loaded);
        ItemContainerContents contents = loaded.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        same(helper, contents.copyOne(), plain(kitted), "a shulker box's contents");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aBundlesContentsKeepAllButTheKit(GameTestHelper helper) {
        HolderLookup.Provider r = registries(helper);
        ItemStack kitted = kitted(helper);
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(kitted.copy())));
        ItemStack loaded = ItemStack.parse(r, uninstalled(bundle.save(r))).orElse(ItemStack.EMPTY);
        helper.assertTrue(loaded.is(Items.BUNDLE), "the bundle loads: " + loaded);
        BundleContents contents = loaded.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        helper.assertValueEqual(contents.size(), 1, "stacks in the bundle");
        same(helper, contents.getItemUnsafe(0), plain(kitted), "a bundle's contents");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aChestKeepsItsPieceButNotTheKit(GameTestHelper helper) {
        HolderLookup.Provider r = registries(helper);
        ItemStack kitted = kitted(helper);
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(at);
        chest.setItem(0, kitted.copy());
        CompoundTag saved = chest.saveWithoutMetadata(r);
        chest.clearContent();
        chest.loadWithComponents(uninstalled(saved), r);
        same(helper, chest.getItem(0), plain(kitted), "a chest");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void anItemFrameKeepsItsPieceButNotTheKit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack kitted = kitted(helper);
        ItemFrame frame = new ItemFrame(EntityType.ITEM_FRAME, level);
        frame.setItem(kitted.copy(), false);
        ItemFrame loaded = new ItemFrame(EntityType.ITEM_FRAME, level);
        loaded.load(uninstalled(saved(frame)));
        same(helper, loaded.getItem(), plain(kitted), "an item frame");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aDroppedItemKeepsItsPieceButNotTheKit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack kitted = kitted(helper);
        ItemEntity dropped = new ItemEntity(level, 0.0, 0.0, 0.0, kitted.copy());
        ItemEntity loaded = new ItemEntity(EntityType.ITEM, level);
        loaded.load(uninstalled(saved(dropped)));
        helper.assertFalse(loaded.isRemoved(), "the dropped item is still there");
        same(helper, loaded.getItem(), plain(kitted), "a dropped item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void anArmourStandKeepsItsPieceButNotTheKit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack kitted = kitted(helper);
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        stand.setItemSlot(EquipmentSlot.CHEST, kitted.copy());
        ArmorStand loaded = new ArmorStand(EntityType.ARMOR_STAND, level);
        loaded.load(uninstalled(saved(stand)));
        same(helper, loaded.getItemBySlot(EquipmentSlot.CHEST), plain(kitted), "an armour stand");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aZombieKeepsItsPieceButNotTheKit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack kitted = kitted(helper);
        Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
        zombie.setItemSlot(EquipmentSlot.CHEST, kitted.copy());
        Zombie loaded = new Zombie(EntityType.ZOMBIE, level);
        loaded.load(uninstalled(saved(zombie)));
        same(helper, loaded.getItemBySlot(EquipmentSlot.CHEST), plain(kitted), "a zombie");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void aPlayersInventoryKeepsItsPiecesButNotTheKit(GameTestHelper helper) {
        ItemStack kitted = kitted(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Inventory inventory = player.getInventory();
        inventory.setItem(5, kitted.copy());
        player.setItemSlot(EquipmentSlot.CHEST, kitted.copy());
        ListTag saved = inventory.save(new ListTag());
        inventory.clearContent();
        inventory.load(uninstalled(saved));
        same(helper, inventory.getItem(5), plain(kitted), "a player's main inventory");
        same(helper, player.getItemBySlot(EquipmentSlot.CHEST), plain(kitted), "a player's chest slot");
        helper.succeed();
    }

    private static HolderLookup.Provider registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    /** effects: a kitted iron chestplate carrying what a player's own piece might: enchantments, wear, a name, a trim, an anvil cost */
    private static ItemStack kitted(GameTestHelper helper) {
        HolderLookup.Provider r = registries(helper);
        HolderLookup.RegistryLookup<Enchantment> enchantments = r.lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack piece = new ItemStack(Items.IRON_CHESTPLATE);
        piece.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 4);
        piece.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
        piece.setDamageValue(37);
        piece.set(DataComponents.CUSTOM_NAME, Component.literal("Dusty"));
        piece.set(DataComponents.TRIM, new ArmorTrim(r.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
                r.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.COAST)));
        piece.set(DataComponents.REPAIR_COST, 3);
        piece.set(SurvivalistContent.KIT.get(), SurvivalistArmor.id("roadplate"));
        return piece;
    }

    /** effects: {@code kitted} as it must load once the mod is gone: the same stack without the kit */
    private static ItemStack plain(ItemStack kitted) {
        ItemStack plain = kitted.copy();
        plain.remove(SurvivalistContent.KIT.get());
        return plain;
    }

    private static CompoundTag saved(Entity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }

    /**
     * effects: a copy of {@code tag} with our component's key renamed to {@link #GONE}, at any depth.
     * A compound comes back a compound and a list a list, so the cast is sound.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Tag> T uninstalled(T tag) {
        return (T) renamed(tag);
    }

    private static Tag renamed(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            CompoundTag out = new CompoundTag();
            for (String key : compound.getAllKeys()) {
                out.put(key.equals(KEY) ? GONE : key, renamed(compound.get(key)));
            }
            return out;
        }
        if (tag instanceof ListTag list) {
            ListTag out = new ListTag();
            for (Tag element : list) {
                out.add(renamed(element));
            }
            return out;
        }
        return tag.copy();
    }

    /** effects: fails the test unless {@code actual} is {@code expected}: the same item, count and every component */
    private static void same(GameTestHelper helper, ItemStack actual, ItemStack expected, String where) {
        helper.assertTrue(ItemStack.isSameItemSameComponents(actual, expected) && actual.getCount() == expected.getCount(),
                where + ": loaded " + actual + " " + actual.getComponentsPatch() + ", wanted " + expected + " " + expected.getComponentsPatch());
    }
}
