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
package com.chunkworks.survivalistarmor.client;

import com.chunkworks.survivalistarmor.domain.KitMesh;
import com.chunkworks.survivalistarmor.domain.Part;
import com.chunkworks.survivalistarmor.domain.Piece;
import com.chunkworks.survivalistarmor.mixin.client.AgeableListModelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

/**
 * A kitted piece worn (D-0001), drawn in place of the game's own armour for that slot by the hook at
 * the head of the armour layer. Each part's quads ride on the wearer's posed body part, so the kit
 * walks, sneaks, swims and rides as the body does, and on a baby as a baby's armour is scaled.
 * Then the trim, on the trimmable quads at their projected coordinates, and the glint. Everything
 * goes through the buffers it is given, so an armour stand that never ticked, drawn into a buffer
 * that only measures (Set It Down's), is drawn the same.
 */
public final class KitWornRenderer {
    private KitWornRenderer() {}

    /**
     * effects: draws {@code entity}'s armour in {@code slot} with its kit and returns true, or draws
     * nothing and returns false if that piece is drawn as itself
     */
    public static boolean render(HumanoidModel<?> parent, HumanoidModel<?> armourModel, LivingEntity entity, EquipmentSlot slot, PoseStack pose,
                                 MultiBufferSource buffers, int light, TextureAtlas trims) {
        ItemStack stack = entity.getItemBySlot(slot);
        KitSkins.Skin skin = KitSkins.of(stack);
        if (skin == null || skin.piece() != KitSkins.piece(slot)) {
            return false;
        }
        AgeableListModelAccessor baby = parent.young ? (AgeableListModelAccessor) armourModel : null;
        VertexConsumer kit = buffers.getBuffer(KitAtlas.WORN);
        for (KitMesh.PartQuads pq : skin.mesh().worn(skin.piece())) {
            pose.pushPose();
            onPart(pose, parent, pq.part(), baby);
            KitDrawer.kit(pose.last(), kit, pq.quads(), skin.garment(), skin.material(), skin.materialColour(), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlasSprite sprite = trims.getSprite(skin.piece().layer() == Piece.Layer.INNER ? trim.innerTexture(skin.armour()) : trim.outerTexture(skin.armour()));
            VertexConsumer out = sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
            for (KitMesh.PartQuads pq : skin.mesh().worn(skin.piece())) {
                pose.pushPose();
                onPart(pose, parent, pq.part(), baby);
                KitDrawer.trim(pose.last(), out, pq.quads(), light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        if (stack.hasFoil()) {
            VertexConsumer out = buffers.getBuffer(RenderType.armorEntityGlint());
            for (KitMesh.PartQuads pq : skin.mesh().worn(skin.piece())) {
                pose.pushPose();
                onPart(pose, parent, pq.part(), baby);
                KitDrawer.glint(pose.last(), out, pq.quads(), light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        return true;
    }

    /**
     * effects: moves {@code pose} onto {@code part} of {@code parent} as posed this frame; for a baby
     * ({@code baby} not null), first as the game's ageable models scale a baby's head or body
     */
    static void onPart(PoseStack pose, HumanoidModel<?> parent, Part part, AgeableListModelAccessor baby) {
        if (baby != null) {
            if (part == Part.HEAD) {
                if (baby.survivalistarmor$scaleHead()) {
                    float f = 1.5F / baby.survivalistarmor$babyHeadScale();
                    pose.scale(f, f, f);
                }
                pose.translate(0.0F, baby.survivalistarmor$babyYHeadOffset() / 16.0F, baby.survivalistarmor$babyZHeadOffset() / 16.0F);
            } else {
                float f = 1.0F / baby.survivalistarmor$babyBodyScale();
                pose.scale(f, f, f);
                pose.translate(0.0F, baby.survivalistarmor$bodyYOffset() / 16.0F, 0.0F);
            }
        }
        model(parent, part).translateAndRotate(pose);
    }

    /** effects: returns the model part {@code part} names */
    static ModelPart model(HumanoidModel<?> m, Part part) {
        return switch (part) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case RIGHT_ARM -> m.rightArm;
            case LEFT_ARM -> m.leftArm;
            case RIGHT_LEG -> m.rightLeg;
            case LEFT_LEG -> m.leftLeg;
        };
    }
}
