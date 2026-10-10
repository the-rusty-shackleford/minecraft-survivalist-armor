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
package com.chunkworks.survivalistarmor.mixin;

import com.chunkworks.survivalistarmor.KitSmithingRecipe;
import com.chunkworks.survivalistarmor.SurvivalistConfig;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeping the kit (D-0001): the smithing table uses up all three of its inputs when the result is
 * taken, the template with them. Where the server keeps kits ({@code consumeTemplate = false}),
 * taking a kitted piece leaves the kit in its slot; every other recipe uses its template as ever.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @Shadow
    @Nullable
    private RecipeHolder<SmithingRecipe> selectedRecipe;

    @WrapWithCondition(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V", ordinal = 0))
    private boolean survivalistarmor$useTheTemplate(SmithingMenu menu, int slot) {
        return !(selectedRecipe != null && selectedRecipe.value() instanceof KitSmithingRecipe && !SurvivalistConfig.consumeTemplate());
    }
}
