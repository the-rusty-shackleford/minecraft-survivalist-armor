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
import com.chunkworks.survivalistarmor.SurvivalistContent;
import com.chunkworks.survivalistarmor.domain.Blacklist;
import com.chunkworks.survivalistarmor.domain.KitMesh;
import com.chunkworks.survivalistarmor.domain.Piece;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of armour as its kit draws it (D-0001): the kit's look, the piece it dresses, and the two
 * sprites it draws from on the kits' own atlas (D-0008) -- the kit's garment, and its material sheet in the
 * palette of the piece's own armour material. A piece is drawn as itself when it carries no kit, is
 * not armour, is refused by the server, or carries a kit no resource pack has a look for.
 *
 * <p>Everything looked up is kept until the next resource reload (or the next blacklist), so drawing
 * a kitted piece allocates nothing.
 */
public final class KitSkins {
    private KitSkins() {}

    /** The game's own undyed leather, as its armour layer tints it. */
    private static final int LEATHER = -6265536;

    /** A kitted piece, ready to draw: {@code materialColour} tints the material sheet (a dyed leather piece's dye). */
    public record Skin(ResourceLocation kit, KitMesh mesh, Piece piece, Holder<ArmorMaterial> armour, TextureAtlasSprite garment,
                       TextureAtlasSprite material, int materialColour) {}

    /** A kit's sprites: its garment, and its material sheet by palette (null where the pack has none). */
    private record Sprites(TextureAtlasSprite garment, Map<String, TextureAtlasSprite> byPalette) {}

    private static final Map<ResourceLocation, Sprites> SPRITES = new HashMap<>();
    private static final Map<Holder<ArmorMaterial>, String> PALETTES = new IdentityHashMap<>();
    private static final Map<Item, Boolean> REFUSED = new IdentityHashMap<>();
    private static Blacklist refusedBy = Blacklist.NONE;

    /** effects: returns how {@code stack} is drawn with its kit, or null if it is drawn as itself */
    @Nullable
    public static Skin of(ItemStack stack) {
        ResourceLocation kit = stack.get(SurvivalistContent.KIT.get());
        if (kit == null || !(stack.getItem() instanceof ArmorItem armour) || refused(stack.getItem())) {
            return null;
        }
        KitMesh mesh = KitLibrary.INSTANCE.kit(kit);
        Piece piece = piece(armour.getEquipmentSlot());
        if (mesh == null || piece == null || !mesh.dresses(piece)) {
            return null;
        }
        Sprites sprites = SPRITES.computeIfAbsent(kit, KitSkins::sprites);
        if (sprites.garment() == null) {
            return null;
        }
        String palette = PALETTES.computeIfAbsent(armour.getMaterial(), KitSkins::palette);
        TextureAtlasSprite material = sheet(sprites, kit, palette);
        if (material == null) {
            material = sheet(sprites, kit, KitLibrary.INSTANCE.palettes().fallback());
        }
        if (material == null) {
            material = sprites.garment();
        }
        int colour = stack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, LEATHER)) : -1;
        return new Skin(kit, mesh, piece, armour.getMaterial(), sprites.garment(), material, colour);
    }

    /** effects: returns the piece a slot's armour is, or null for a slot armour doesn't go in */
    @Nullable
    public static Piece piece(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Piece.HELMET;
            case CHEST -> Piece.CHESTPLATE;
            case LEGS -> Piece.LEGGINGS;
            case FEET -> Piece.BOOTS;
            default -> null;
        };
    }

    /** effects: forgets every sprite and palette looked up, for a resource reload */
    static void clear() {
        SPRITES.clear();
        PALETTES.clear();
        REFUSED.clear();
    }

    private static boolean refused(Item item) {
        Blacklist now = SurvivalistConfig.blacklist();
        if (now != refusedBy) {
            REFUSED.clear();
            refusedBy = now;
        }
        return REFUSED.computeIfAbsent(item, i -> now.refuses(BuiltInRegistries.ITEM.getKey(i).toString()));
    }

    private static Sprites sprites(ResourceLocation kit) {
        return new Sprites(sprite(kit, "garment"), new HashMap<>());
    }

    /** effects: returns the kit's material sheet in {@code palette}, or null if no pack has one; a miss is remembered too */
    @Nullable
    private static TextureAtlasSprite sheet(Sprites sprites, ResourceLocation kit, String palette) {
        Map<String, TextureAtlasSprite> byPalette = sprites.byPalette();
        if (byPalette.containsKey(palette)) {
            return byPalette.get(palette);
        }
        TextureAtlasSprite s = sprite(kit, "material_" + palette);
        byPalette.put(palette, s);
        return s;
    }

    /** effects: returns the kits' atlas's sprite {@code <ns>:kit/<kit>/<name>}, or null if no pack stitched it */
    @Nullable
    private static TextureAtlasSprite sprite(ResourceLocation kit, String name) {
        TextureAtlasSprite s = KitAtlas.sprite(ResourceLocation.fromNamespaceAndPath(kit.getNamespace(), "kit/" + kit.getPath() + "/" + name));
        return s.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : s;
    }

    private static String palette(Holder<ArmorMaterial> material) {
        return material.unwrapKey()
                .map(k -> KitLibrary.INSTANCE.palettes().paletteFor(k.location().toString()))
                .orElse(KitLibrary.INSTANCE.palettes().fallback());
    }
}
