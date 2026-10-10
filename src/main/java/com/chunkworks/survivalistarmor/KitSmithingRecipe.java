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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A kit applied at the smithing table (D-0001): the kit in the template slot, a piece of armour in
 * the base slot, the kit's scrap in the addition slot. Out comes the same piece, every component of
 * it kept -- enchantments, wear, name, trim, dye -- with the kit set on it. A piece that already
 * wears this kit makes nothing; one wearing another kit takes this one instead.
 *
 * <p>AF: applies kit {@code kit}, taking a template {@code template} matches, a base piece
 * {@code base} matches that is armour the server does not refuse, and an addition {@code addition}
 * matches.
 */
public record KitSmithingRecipe(ResourceLocation kit, Ingredient template, Ingredient base, Ingredient addition) implements SmithingRecipe {
    public static final MapCodec<KitSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("kit").forGetter(KitSmithingRecipe::kit),
            Ingredient.CODEC_NONEMPTY.fieldOf("template").forGetter(KitSmithingRecipe::template),
            Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(KitSmithingRecipe::base),
            Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(KitSmithingRecipe::addition)
    ).apply(i, KitSmithingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, KitSmithingRecipe::kit,
            Ingredient.CONTENTS_STREAM_CODEC, KitSmithingRecipe::template,
            Ingredient.CONTENTS_STREAM_CODEC, KitSmithingRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, KitSmithingRecipe::addition,
            KitSmithingRecipe::new);

    public static final RecipeSerializer<KitSmithingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<KitSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KitSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return template.test(input.template()) && isBaseIngredient(input.base()) && addition.test(input.addition())
                && !kit.equals(input.base().get(SurvivalistContent.KIT.get()));
    }

    /** effects: returns the base piece with this kit set on it, everything else of it kept */
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().copyWithCount(1);
        out.set(SurvivalistContent.KIT.get(), kit);
        return out;
    }

    /** effects: returns an iron chestplate wearing this kit, the recipe's picture */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack shown = new ItemStack(Items.IRON_CHESTPLATE);
        shown.set(SurvivalistContent.KIT.get(), kit);
        return shown;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    /** effects: returns whether a kit can go on {@code stack}: armour the base ingredient takes and the server does not refuse */
    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem && base.test(stack) && !refused(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(template, base, addition).anyMatch(Ingredient::hasNoItems);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    /** effects: returns whether the server's blacklist refuses {@code stack}'s item */
    static boolean refused(ItemStack stack) {
        return SurvivalistConfig.blacklist().refuses(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
