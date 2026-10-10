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
import com.chunkworks.survivalistarmor.SurvivalistArmor;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A kit changes how a piece looks and nothing else (D-0001, nfx's §2), and travels with the piece to
 * every player.
 *
 * <p>Partitions: every armour material's piece, every slot; what could differ (the item, its name,
 * its durability, its enchantability, its attribute modifiers -- armour, toughness, knockback
 * resistance); the stack sent to a client and back.
 */
@GameTestHolder(SurvivalistArmor.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CosmeticOnlyGameTests {
    public CosmeticOnlyGameTests() {}

    private static final String TEMPLATE = "plot";
    private static final List<Item> ARMOUR = List.of(Items.LEATHER_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.IRON_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.DIAMOND_CHESTPLATE, Items.NETHERITE_HELMET, Items.TURTLE_HELMET);

    @GameTest(template = TEMPLATE)
    public void aKitChangesNoStatAndNoName(GameTestHelper helper) {
        for (Item item : ARMOUR) {
            ItemStack plain = Pieces.worn(helper, item);
            ItemStack kitted = Pieces.kitted(plain, Kit.HAZMAT);
            String what = item.toString();
            helper.assertTrue(kitted.getItem() == plain.getItem(), what + ": the same item");
            helper.assertValueEqual(kitted.getHoverName().getString(), plain.getHoverName().getString(), what + ": the name");
            helper.assertValueEqual(kitted.getMaxDamage(), plain.getMaxDamage(), what + ": durability");
            helper.assertValueEqual(kitted.getDamageValue(), plain.getDamageValue(), what + ": wear");
            helper.assertValueEqual(kitted.getEnchantmentValue(), plain.getEnchantmentValue(), what + ": enchantability");
            EquipmentSlot slot = ((ArmorItem) item).getEquipmentSlot();
            helper.assertValueEqual(modifiers(kitted, slot), modifiers(plain, slot), what + ": attribute modifiers");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public void theKitTravelsWithTheStack(GameTestHelper helper) {
        ItemStack kitted = Pieces.kitted(Pieces.worn(helper, Items.IRON_CHESTPLATE), Kit.ROADPLATE);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, kitted);
        Pieces.same(helper, ItemStack.STREAM_CODEC.decode(buf), kitted, "the stack a client receives");
        helper.succeed();
    }

    /** effects: returns the attribute modifiers {@code stack} gives in {@code slot}, as text */
    private static List<String> modifiers(ItemStack stack, EquipmentSlot slot) {
        List<String> out = new ArrayList<>();
        stack.forEachModifier(slot, (attribute, modifier) -> out.add(attribute.getRegisteredName() + " " + modifier));
        return out;
    }
}
