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
package com.chunkworks.survivalistarmor.mixin.client;

import com.chunkworks.survivalistarmor.client.KitItemModels;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A kitted piece as an item (D-0003). Each of the item renderer's three ways in is wrapped whole --
 * outside every other mod's injections into it, so Modefite's, which would draw the resource pack's
 * model for the item, never runs for a kitted stack -- and a kitted stack is answered and drawn by
 * its kit. Every other stack calls through untouched.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @WrapMethod(method = "getModel")
    private BakedModel survivalistarmor$kitModel(ItemStack stack, @Nullable Level level, @Nullable LivingEntity entity, int seed,
                                                 Operation<BakedModel> original) {
        BakedModel kit = KitItemModels.model(stack);
        return kit != null ? kit : original.call(stack, level, entity, seed);
    }

    @WrapMethod(method = "render")
    private void survivalistarmor$kitDraw(ItemStack stack, ItemDisplayContext view, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                          int light, int overlay, BakedModel model, Operation<Void> original) {
        if (!KitItemModels.render(stack, view, leftHand, pose, buffers, light, overlay)) {
            original.call(stack, view, leftHand, pose, buffers, light, overlay, model);
        }
    }

    @WrapMethod(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V")
    private void survivalistarmor$kitDrawStatic(@Nullable LivingEntity entity, ItemStack stack, ItemDisplayContext view, boolean leftHand, PoseStack pose,
                                                MultiBufferSource buffers, @Nullable Level level, int light, int overlay, int seed,
                                                Operation<Void> original) {
        if (!KitItemModels.render(stack, view, leftHand, pose, buffers, light, overlay)) {
            original.call(entity, stack, view, leftHand, pose, buffers, level, light, overlay, seed);
        }
    }
}
