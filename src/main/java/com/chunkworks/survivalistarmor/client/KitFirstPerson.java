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

import com.chunkworks.survivalistarmor.SurvivalistConfig;
import com.chunkworks.survivalistarmor.domain.KitMesh;
import com.chunkworks.survivalistarmor.domain.Part;
import com.chunkworks.survivalistarmor.domain.Piece;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

/**
 * A kitted chestplate's sleeve on your own arm in first person (D-0001, nfx's §13). The game draws
 * your arm and its sleeve and no armour; this draws the chestplate's arm part after the sleeve, on
 * the same pose, so it follows the swing: the same quads everyone else sees on you, trim and glint
 * included. Nothing when the chestplate has no kit, or when the player turns it off.
 */
public final class KitFirstPerson {
    private KitFirstPerson() {}

    private static final RenderType KIT = RenderType.armorCutoutNoCull(InventoryMenu.BLOCK_ATLAS);

    /** effects: draws the sleeve of {@code player}'s kitted chestplate on {@code arm}, if it has one */
    public static void render(PlayerModel<?> model, ModelPart arm, AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!SurvivalistConfig.firstPersonArms()) {
            return;
        }
        Part part = arm == model.rightArm ? Part.RIGHT_ARM : arm == model.leftArm ? Part.LEFT_ARM : null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        KitSkins.Skin skin = part == null ? null : KitSkins.of(chest);
        if (skin == null || skin.piece() != Piece.CHESTPLATE) {
            return;
        }
        for (KitMesh.PartQuads pq : skin.mesh().worn(Piece.CHESTPLATE)) {
            if (pq.part() != part) {
                continue;
            }
            pose.pushPose();
            arm.translateAndRotate(pose);
            KitDrawer.kit(pose.last(), buffers.getBuffer(KIT), pq.quads(), skin.garment(), skin.material(), skin.materialColour(), light,
                    OverlayTexture.NO_OVERLAY);
            ArmorTrim trim = chest.get(DataComponents.TRIM);
            if (trim != null) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(trim.outerTexture(skin.armour()));
                KitDrawer.trim(pose.last(), sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))), pq.quads(), light,
                        OverlayTexture.NO_OVERLAY);
            }
            if (chest.hasFoil()) {
                KitDrawer.glint(pose.last(), buffers.getBuffer(RenderType.armorEntityGlint()), pq.quads(), light, OverlayTexture.NO_OVERLAY);
            }
            pose.popPose();
        }
    }
}
