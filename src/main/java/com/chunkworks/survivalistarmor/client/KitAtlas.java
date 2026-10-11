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

import com.chunkworks.survivalistarmor.SurvivalistArmor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;

/**
 * The kits' own atlas (D-0008): every kit's garment and material sheets, stitched apart from the
 * block atlas, as the game keeps armour trims on theirs. On the block atlas, beside Pixlli's 128x,
 * the seven kits doubled it from 8192x8192 to 16384x8192; here they cost the blocks nothing. The
 * sprites are listed in {@code assets/survivalistarmor/atlases/kits.json}, which a pack adding a
 * kit extends. Iris reads a kit's {@code _n} and {@code _s} maps from it as from any atlas.
 *
 * <p>Stitched without mipmaps, as every holder of the game's is: the kits' render types never
 * sampled mipmaps on the block atlas either.
 */
public final class KitAtlas extends TextureAtlasHolder {
    public static final ResourceLocation LOCATION = SurvivalistArmor.id("textures/atlas/kits.png");
    /** A kit worn, and its sleeve in first person: the game's armour render type, on this atlas. */
    public static final RenderType WORN = RenderType.armorCutoutNoCull(LOCATION);
    /** A kitted piece as an item: the game's cutout item render type, on this atlas. */
    public static final RenderType ITEM = RenderType.entityCutout(LOCATION);

    private static KitAtlas instance;

    private KitAtlas(TextureManager textures) {
        super(textures, LOCATION, SurvivalistArmor.id("kits"));
    }

    /** effects: makes the atlas, registered with {@code textures}, and returns it for the resource reload */
    static KitAtlas create(TextureManager textures) {
        instance = new KitAtlas(textures);
        return instance;
    }

    /** effects: returns the atlas's sprite {@code name}, or the missing sprite if no pack stitched it */
    static TextureAtlasSprite sprite(ResourceLocation name) {
        return instance.getSprite(name);
    }
}
