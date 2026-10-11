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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * A kitted piece as an item (D-0003): the worn piece's own quads, standing in a one-block box, baked
 * as real quads on the kits' own atlas (D-0008) -- so anything that measures or draws an item's model
 * finds the piece's geometry. The material sheet's quads carry tint index 0, which the game's item
 * colours give a leather piece's dye. A lit lens is baked at full brightness. Its particle icon is
 * the armour item's own, on the block atlas: a breaking piece's particles are drawn from there.
 */
final class KitItemModel implements BakedModel {
    private final List<BakedQuad> quads;
    private final ItemTransforms transforms;
    private final TextureAtlasSprite particle;
    private final KitMesh.Quads source;

    private KitItemModel(List<BakedQuad> quads, ItemTransforms transforms, TextureAtlasSprite particle, KitMesh.Quads source) {
        this.quads = List.copyOf(quads);
        this.transforms = transforms;
        this.particle = particle;
        this.source = source;
    }

    /**
     * requires: {@code particle} is on the block atlas
     * effects: returns {@code q} baked from {@code garment} and {@code material}, standing as {@code transforms} says
     */
    static KitItemModel bake(KitMesh.Quads q, TextureAtlasSprite garment, TextureAtlasSprite material, TextureAtlasSprite particle,
                             ItemTransforms transforms) {
        List<BakedQuad> out = new ArrayList<>(q.count());
        float[] p = q.positions(), uv = q.uvs(), n = q.normals();
        for (int i = 0; i < q.count(); i++) {
            boolean sheet = q.material()[i];
            TextureAtlasSprite s = sheet ? material : garment;
            QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
            b.setSprite(s);
            b.setDirection(Direction.getNearest(n[i * 3], n[i * 3 + 1], n[i * 3 + 2]));
            b.setTintIndex(sheet ? 0 : -1);
            b.setShade(true);
            b.setHasAmbientOcclusion(false);
            int light = q.lit()[i] ? LightTexture.FULL_BRIGHT : 0;
            for (int k = 0; k < 4; k++) {
                int c = i * 12 + k * 3;
                b.addVertex(p[c], p[c + 1], p[c + 2]);
                b.setColor(-1);
                b.setUv(s.getU(uv[i * 8 + k * 2]), s.getV(uv[i * 8 + k * 2 + 1]));
                b.setLight(light);
                b.setNormal(n[i * 3], n[i * 3 + 1], n[i * 3 + 2]);
            }
            out.add(b.bakeQuad());
        }
        return new KitItemModel(out, transforms, particle, q);
    }

    /** effects: returns the quads the model was baked from, for drawing the trim and the glint over it */
    KitMesh.Quads source() {
        return source;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return side == null ? quads : List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return List.of(KitAtlas.ITEM);
    }
}
