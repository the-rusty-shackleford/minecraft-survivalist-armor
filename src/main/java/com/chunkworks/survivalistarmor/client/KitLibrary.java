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
import com.chunkworks.survivalistarmor.domain.KitMesh;
import com.chunkworks.survivalistarmor.domain.KitModel;
import com.chunkworks.survivalistarmor.domain.PaletteTable;
import com.chunkworks.survivalistarmor.domain.Piece;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every kit's look in every resource pack, read once per reload, F3+T included, so a kit is changed
 * without a recompile (D-0001, nfx's §8). {@code assets/<ns>/survivalistarmor/kits/<name>.bbmodel}
 * is the look of kit {@code <ns>:<name>}. Beside the kits:
 * {@code survivalistarmor/palettes.json}, which palette an armour material takes, layered pack over
 * pack; and {@code survivalistarmor/display/<piece>.json}, how a kitted piece stands as an item in
 * each view (an item model's {@code display} block).
 *
 * <p>A project that is not a kit is logged with its pack and the reason, and its pieces are drawn as
 * themselves. Parsing runs on the reload's worker; drawing reads the result.
 */
public final class KitLibrary extends SimplePreparableReloadListener<KitLibrary.Loaded> {
    private static final Logger LOG = LoggerFactory.getLogger("Survivalist Armor");
    public static final KitLibrary INSTANCE = new KitLibrary();

    private static final String KITS = "survivalistarmor/kits";
    private static final String BBMODEL = ".bbmodel";
    private static final ResourceLocation PALETTES = SurvivalistArmor.id("survivalistarmor/palettes.json");
    /** An item model's {@code display} block, read as the game reads it: translations in pixels, clamped as the game clamps them. */
    private static final Gson DISPLAY_GSON = new GsonBuilder()
            .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer() {})
            .registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer() {})
            .create();

    /** What a reload found. */
    public record Loaded(Map<ResourceLocation, KitMesh> kits, PaletteTable palettes, Map<Piece, ItemTransforms> display) {}

    private volatile Loaded loaded = new Loaded(Map.of(), PaletteTable.EMPTY, Map.of());

    private KitLibrary() {}

    /** effects: returns kit {@code id}'s look, or null if no pack has it */
    @Nullable
    public KitMesh kit(ResourceLocation id) {
        return loaded.kits().get(id);
    }

    /** effects: returns the palette table */
    public PaletteTable palettes() {
        return loaded.palettes();
    }

    /** effects: returns how a kitted {@code piece} stands as an item */
    public ItemTransforms display(Piece piece) {
        return loaded.display().getOrDefault(piece, ItemTransforms.NO_TRANSFORMS);
    }

    @Override
    protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, KitMesh> kits = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources(KITS, id -> id.getPath().endsWith(BBMODEL)).entrySet()) {
            ResourceLocation file = e.getKey();
            String path = file.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path.substring(KITS.length() + 1, path.length() - BBMODEL.length()));
            try (Reader reader = e.getValue().openAsReader()) {
                KitModel.Parsed parsed = KitModel.parse(readAll(reader));
                for (String warning : parsed.warnings()) {
                    LOG.warn("Survivalist Armor: kit {} ({}): {}", id, e.getValue().sourcePackId(), warning);
                }
                kits.put(id, KitMesh.of(parsed.model()));
            } catch (IOException | RuntimeException ex) {
                LOG.error("Survivalist Armor: kit {} ({}) is not a kit, its pieces are drawn as themselves: {}", id, e.getValue().sourcePackId(), ex.toString());
            }
        }
        PaletteTable palettes = PaletteTable.EMPTY;
        try {
            List<String> files = new ArrayList<>();
            for (Resource r : manager.getResourceStack(PALETTES)) {
                try (Reader reader = r.openAsReader()) {
                    files.add(readAll(reader));
                }
            }
            palettes = PaletteTable.layered(files);
        } catch (IOException | RuntimeException ex) {
            LOG.error("Survivalist Armor: the palette table does not read, every material takes the default: {}", ex.toString());
        }
        Map<Piece, ItemTransforms> display = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            ResourceLocation file = SurvivalistArmor.id("survivalistarmor/display/" + piece.folder() + ".json");
            Optional<Resource> r = manager.getResource(file);
            if (r.isEmpty()) {
                continue;
            }
            try (Reader reader = r.get().openAsReader()) {
                display.put(piece, DISPLAY_GSON.fromJson(reader, ItemTransforms.class));
            } catch (IOException | RuntimeException ex) {
                LOG.error("Survivalist Armor: {} does not read: {}", file, ex.toString());
            }
        }
        LOG.info("Survivalist Armor: {} kits: {}", kits.size(), kits.keySet());
        return new Loaded(Map.copyOf(kits), palettes, display);
    }

    @Override
    protected void apply(Loaded prepared, ResourceManager manager, ProfilerFiller profiler) {
        loaded = prepared;
        KitItemModels.clear();
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
