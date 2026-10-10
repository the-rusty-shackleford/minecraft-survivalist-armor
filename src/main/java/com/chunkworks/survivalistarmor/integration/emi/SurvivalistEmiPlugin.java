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
package com.chunkworks.survivalistarmor.integration.emi;

import com.chunkworks.survivalistarmor.Kit;
import com.chunkworks.survivalistarmor.KitSmithingRecipe;
import com.chunkworks.survivalistarmor.SurvivalistArmor;
import com.chunkworks.survivalistarmor.SurvivalistContent;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * EMI shows a smithing recipe only when it is one of the game's own, so a kit at the smithing table, and
 * the shears that take one off, would be missing from the recipe viewer the pack carries. This plugin
 * shows both: each kit's recipe as the smithing table lays it out, the piece and the kitted piece turning
 * together through every piece the kit goes on (EMI's own trim recipes turn the same way), and one
 * crafting recipe for the removal. Loaded by EMI's entrypoint scan only when EMI is installed; nothing
 * else references it.
 */
@EmiEntrypoint
public final class SurvivalistEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        List<ItemStack> pieces = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof ArmorItem)
                .map(ItemStack::new)
                .toList();
        for (RecipeHolder<?> holder : registry.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)) {
            if (holder.value() instanceof KitSmithingRecipe recipe) {
                List<ItemStack> bases = pieces.stream().filter(recipe::isBaseIngredient).toList();
                if (!bases.isEmpty()) {
                    registry.addRecipe(new KitRecipe(holder.id(), recipe, bases));
                }
            }
        }
        registry.addRecipe(new RemovalRecipe());
    }

    /** effects: returns a copy of {@code piece} wearing {@code kit} */
    static ItemStack kitted(ItemStack piece, ResourceLocation kit) {
        ItemStack out = piece.copyWithCount(1);
        out.set(SurvivalistContent.KIT.get(), kit);
        return out;
    }

    /** A kit at the smithing table: template, piece and scrap, and the piece wearing the kit. */
    private record KitRecipe(ResourceLocation id, KitSmithingRecipe recipe, List<ItemStack> bases) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return VanillaEmiRecipeCategories.SMITHING;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return List.of(EmiIngredient.of(recipe.template()), EmiIngredient.of(bases.stream().map(EmiStack::of).toList()),
                    EmiIngredient.of(recipe.addition()));
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(EmiStack.of(kitted(bases.getFirst(), recipe.kit())));
        }

        @Override
        public int getDisplayWidth() {
            return 112;
        }

        @Override
        public int getDisplayHeight() {
            return 18;
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);
            widgets.addSlot(EmiIngredient.of(recipe.template()), 0, 0);
            // The piece and the kitted piece are drawn from the same seed, so they turn together.
            int unique = Seeds.next();
            widgets.addGeneratedSlot(r -> EmiStack.of(pick(r)), unique, 18, 0);
            widgets.addSlot(EmiIngredient.of(recipe.addition()), 36, 0);
            widgets.addGeneratedSlot(r -> EmiStack.of(kitted(pick(r), recipe.kit())), unique, 94, 0).recipeContext(this);
        }

        private ItemStack pick(Random random) {
            return bases.get(random.nextInt(bases.size()));
        }
    }

    /** The kit cut off: a kitted piece and shears in the grid give the piece as it was. */
    private static final class RemovalRecipe implements EmiRecipe {
        private static final ResourceLocation ID = SurvivalistArmor.id("/kit_removal");
        private static final List<Kit> KITS = List.of(Kit.values());
        private static final List<ItemStack> PIECES = List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.NETHERITE_CHESTPLATE));

        @Override
        public EmiRecipeCategory getCategory() {
            return VanillaEmiRecipeCategories.CRAFTING;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return List.of(EmiStack.of(kitted(PIECES.get(1), Kit.ROADPLATE.id())), EmiStack.of(Items.SHEARS));
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(EmiStack.of(PIECES.get(1)));
        }

        @Override
        public int getDisplayWidth() {
            return 118;
        }

        @Override
        public int getDisplayHeight() {
            return 18;
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            int unique = Seeds.next();
            widgets.addGeneratedSlot(r -> EmiStack.of(kitted(PIECES.get(r.nextInt(PIECES.size())), KITS.get(r.nextInt(KITS.size())).id())),
                    unique, 0, 0);
            widgets.addTexture(EmiTexture.PLUS, 22, 3);
            widgets.addSlot(EmiStack.of(Items.SHEARS), 36, 0);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 1);
            widgets.addGeneratedSlot(r -> EmiStack.of(PIECES.get(r.nextInt(PIECES.size()))), unique, 100, 0).recipeContext(this);
        }
    }

    /** Seeds for generated slots that must turn together: one per recipe drawn. */
    private static final class Seeds {
        private static int next = 0x5a17;

        static synchronized int next() {
            return next++;
        }
    }
}
