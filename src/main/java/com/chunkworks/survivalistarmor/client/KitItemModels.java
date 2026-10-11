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

import com.chunkworks.survivalistarmor.domain.Piece;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Kitted pieces as items (D-0003). The item renderer is wrapped, outside every other mod's hooks into
 * it (Modefite's included, which would otherwise draw the resource pack's model for the item), so
 * that a kitted stack's model is its kit's in every view -- the inventory, a hand, the ground, an
 * item frame -- and is drawn here: posed for its view, its quads, its trim with its real pattern, its
 * glint. Every other stack goes on to the game as before.
 *
 * <p>Models are baked the first time a kit, piece and palette is drawn, and kept until the next
 * resource reload.
 */
public final class KitItemModels {
    private KitItemModels() {}

    private record Key(ResourceLocation kit, Piece piece, TextureAtlasSprite material, Item item) {}

    private static final Map<Key, KitItemModel> MODELS = new HashMap<>();

    /** effects: returns {@code stack}'s kit model, or null if it is drawn as itself */
    @Nullable
    public static BakedModel model(ItemStack stack) {
        KitSkins.Skin skin = KitSkins.of(stack);
        return skin == null ? null : model(skin, stack.getItem());
    }

    /**
     * effects: if {@code stack} is drawn with its kit, draws it for {@code view} and returns true;
     * else draws nothing and returns false
     */
    public static boolean render(ItemStack stack, ItemDisplayContext view, boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light,
                                 int overlay) {
        KitSkins.Skin skin = stack.isEmpty() ? null : KitSkins.of(stack);
        if (skin == null) {
            return false;
        }
        KitItemModel model = model(skin, stack.getItem());
        pose.pushPose();
        ClientHooks.handleCameraTransforms(pose, model, view, leftHand);
        pose.translate(-0.5F, -0.5F, -0.5F);
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        VertexConsumer out = ItemRenderer.getFoilBufferDirect(buffers, KitAtlas.ITEM, true, stack.hasFoil());
        items.renderModelLists(model, stack, light, overlay, pose, out);
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                    .getSprite(skin.piece() == Piece.LEGGINGS ? trim.innerTexture(skin.armour()) : trim.outerTexture(skin.armour()));
            KitDrawer.trim(pose.last(), sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))), model.source(), light, overlay);
        }
        pose.popPose();
        return true;
    }

    /** effects: forgets every baked model, for a resource reload or a new atlas */
    static void clear() {
        MODELS.clear();
    }

    private static KitItemModel model(KitSkins.Skin skin, Item item) {
        return MODELS.computeIfAbsent(new Key(skin.kit(), skin.piece(), skin.material(), item),
                k -> KitItemModel.bake(skin.mesh().item(skin.piece()), skin.garment(), skin.material(),
                        Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item).getParticleIcon(),
                        KitLibrary.INSTANCE.display(skin.piece())));
    }
}
