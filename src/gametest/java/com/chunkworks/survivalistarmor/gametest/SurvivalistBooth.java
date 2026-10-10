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
package com.chunkworks.survivalistarmor.gametest;

import com.chunkworks.survivalistarmor.Kit;
import com.chunkworks.survivalistarmor.SurvivalistArmor;
import com.chunkworks.survivalistarmor.SurvivalistContent;
import com.chunkworks.survivalistarmor.client.KitLibrary;
import com.chunkworks.survivalistarmor.client.KitSkins;
import com.chunkworks.survivalistarmor.domain.KitMesh;
import com.chunkworks.survivalistarmor.domain.Piece;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.EmiApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kits on film, in a flat world at noon, among the mods that fight over armour and hands
 * (devtools/booth/fetch.py fills the game directory). Scenes: a row of armour stands in kits on
 * every palette, one with a trim and one with a glint, beside a plain set Armored Legacy draws;
 * mobs in kits, a baby among them, and a villager (Serfdom draws villagers' armour); kitted pieces
 * in item frames and on the ground; a kitted chestplate set down by Set It Down; your own arm; the
 * inventory; the smithing table's preview.
 *
 * <p>The checks draw what they judge into a {@link Capture} and count what each render type was
 * given, so they say whose model was drawn without reading a pixel: the kit's quads, as many as its
 * mesh has, or the game's own armour. One {@code booth: PASS} or {@code booth: FAIL} line per check;
 * the Gradle task reads them. Client only, active only under {@code survivalistarmor.booth}:
 * {@code checks} (the automated booth) or {@code photos} (the GPU's, shaders on).
 */
@EventBusSubscriber(modid = GameTestMod.MOD_ID, value = Dist.CLIENT)
public final class SurvivalistBooth {
    private SurvivalistBooth() {}

    private static final Logger LOG = LoggerFactory.getLogger("Survivalist Armor booth");
    private static final String MODE = System.getProperty("survivalistarmor.booth", "");
    /** The kit on film and under the checks: Roadplate, or what -PboothKit names (-Dsurvivalistarmor.booth.kit). */
    private static final ResourceLocation KIT = ResourceLocation.parse(System.getProperty("survivalistarmor.booth.kit", "survivalistarmor:roadplate"));

    private static final RenderType WORN = RenderType.armorCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    private enum Phase { TITLE, LOADING, PLACING, RUNNING, DONE }

    private record Step(int at, Runnable action) {}

    private static final int HOLD = 100;
    private static final int SETTLE = 40;

    private static boolean muted = false;
    private static Phase phase = Phase.TITLE;
    private static int tick = 0;
    private static List<Step> steps;
    private static int ground;
    private static volatile boolean dressed = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (MODE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!muted) {
            // Silent from the first tick: Rusty listens to music while these run.
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            muted = true;
        }
        // No advancement or recipe toasts in the photographs.
        mc.getToasts().clear();
        switch (phase) {
            case TITLE -> {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                    phase = Phase.LOADING;
                    createWorld(mc);
                }
            }
            case LOADING -> {
                MinecraftServer server = mc.getSingleplayerServer();
                if (mc.level != null && mc.player != null && mc.screen == null && server != null && mc.level.hasChunkAt(mc.player.blockPosition())) {
                    phase = Phase.PLACING;
                    mc.options.hideGui = true;
                    steps = plan(mc);
                    onServer(mc, SurvivalistBooth::setUp);
                }
            }
            case PLACING -> {
                if (dressed) {
                    phase = Phase.RUNNING;
                    tick = 0;
                }
            }
            case RUNNING -> {
                for (Step step : steps) {
                    if (step.at() == tick) {
                        step.action().run();
                    }
                }
                tick++;
            }
            case DONE -> { }
        }
    }

    private static void createWorld(Minecraft mc) {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
        LevelSettings settings = new LevelSettings("Survivalist Armor booth", GameType.SURVIVAL, false, Difficulty.EASY, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("survivalistarmor-booth", settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
    }

    // --- the pieces ----------------------------------------------------------------

    /** effects: returns a piece of {@code item} wearing the kit on film */
    static ItemStack kitted(Item item) {
        return kitted(item, KIT);
    }

    /** effects: returns a piece of {@code item} wearing {@code kit} */
    static ItemStack kitted(Item item, ResourceLocation kit) {
        ItemStack s = new ItemStack(item);
        s.set(SurvivalistContent.KIT.get(), kit);
        return s;
    }

    /** The placeholder's twin with loud LabPBR maps, beside the placeholder in the photographs (spike S4). */
    private static final ResourceLocation LOUD = ResourceLocation.parse("survivalistarmor_gametest:placeholder_pbr");
    private static final boolean PHOTOS = MODE.equals("photos");

    private static ItemStack trimmed(ServerLevel level, ItemStack piece) {
        var r = level.registryAccess();
        piece.set(DataComponents.TRIM, new ArmorTrim(r.registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.GOLD),
                r.registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.COAST)));
        return piece;
    }

    private static ItemStack enchanted(ServerLevel level, ItemStack piece) {
        piece.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 4);
        return piece;
    }

    private static ItemStack dyed(ItemStack piece, int rgb) {
        piece.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, true));
        return piece;
    }

    private static final Item[][] SETS = {
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
            {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
            {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
            {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS},
            {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS}};

    /** A kit in the lineup and the armour it wears there. */
    private record Member(String kit, Item[] set) {}

    private static final Member[] LINEUP = {
            new Member("roadplate", SETS[0]), new Member("magpie", SETS[3]), new Member("bluecoat", SETS[1]),
            new Member("blacklung", SETS[4]), new Member("hazmat", SETS[0]), new Member("duster", SETS[2]),
            new Member("scrapper", SETS[5])};

    // --- the scenes ----------------------------------------------------------------

    /** Noon; the stands, the mobs, the frames, the ground and the bench, all dressed. */
    private static void setUp(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        // The photographs' sun is low in the west, so relief and metal catch it.
        level.setDayTime(PHOTOS ? 10500L : 6000L);
        ground = level.getMinBuildHeight() + 4;
        sp.getInventory().clearContent();
        sp.setInvulnerable(true);
        sp.getAbilities().mayfly = true;
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        // The stands, facing south at the camera: a kitted set on each palette (iron with a gold coast
        // trim on the chest, leather dyed blue, netherite enchanted), then a plain iron set, Armored Legacy's.
        for (int i = 0; i < SETS.length; i++) {
            ItemStack[] pieces = new ItemStack[4];
            for (int k = 0; k < 4; k++) {
                pieces[k] = kitted(SETS[i][k]);
            }
            if (i == 0) {
                trimmed(level, pieces[1]);
            }
            if (i == 2) {
                for (ItemStack p : pieces) {
                    dyed(p, 0x3B5BA6);
                }
            }
            if (i == 4) {
                enchanted(level, pieces[1]);
            }
            stand(level, -7.5 + i * 2.5, 0.5, pieces);
        }
        stand(level, 8.5, 0.5, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS));
        // The side-on stand, alone to the east, for the photograph the kits are judged by.
        stand(level, 14.5, 0.5, kitted(Items.IRON_HELMET), trimmed(level, kitted(Items.IRON_CHESTPLATE)), kitted(Items.IRON_LEGGINGS),
                kitted(Items.IRON_BOOTS));
        // The placeholder and its loud twin side by side, iron on both, for the shader's reading of the maps.
        for (int i = 0; i < 2; i++) {
            ResourceLocation kit = i == 0 ? ResourceLocation.parse("survivalistarmor_gametest:placeholder") : LOUD;
            stand(level, 20.5 + i * 2.0, 0.5, kitted(Items.IRON_HELMET, kit), kitted(Items.IRON_CHESTPLATE, kit), kitted(Items.IRON_LEGGINGS, kit),
                    kitted(Items.IRON_BOOTS, kit));
        }
        // The mobs, north of the stands: a zombie and a baby, a skeleton, a piglin, a villager.
        mob(level, EntityType.ZOMBIE.create(level), -5.5, -4.5, false);
        mob(level, EntityType.ZOMBIE.create(level), -3.0, -4.5, true);
        mob(level, EntityType.SKELETON.create(level), -0.5, -4.5, false);
        Piglin piglin = EntityType.PIGLIN.create(level);
        if (piglin != null) {
            piglin.setImmuneToZombification(true);
        }
        mob(level, piglin, 2.0, -4.5, false);
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.moveTo(4.5, ground, -4.5, 0.0F, 0.0F);
            villager.setNoAi(true);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setItemSlot(EquipmentSlot.CHEST, kitted(Items.IRON_CHESTPLATE));
            level.addFreshEntity(villager);
        }
        // A wall to the north with item frames, kitted pieces on the ground before the stands.
        fill(level, -4, 0, -9, 4, 3, -9, Blocks.STONE_BRICKS);
        Item[] framed = {Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.NETHERITE_BOOTS};
        for (int i = 0; i < framed.length; i++) {
            ItemFrame frame = new ItemFrame(level, new BlockPos(-3 + i * 2, ground + 1, -8), Direction.SOUTH);
            frame.setItem(kitted(framed[i]), false);
            level.addFreshEntity(frame);
        }
        for (int i = 0; i < framed.length; i++) {
            ItemEntity drop = new ItemEntity(level, -3.5 + i * 2, ground, 3.5, kitted(framed[i]));
            drop.setNeverPickUp();
            drop.setUnlimitedLifetime();
            drop.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(drop);
        }
        // The crew: the seven kits in a row south of everything, each in the armour it is designed toward first, as
        // nfx's reference image lines its figures up. Photographed in both booths: the software booth's pictures carry
        // no Pixlli pixel and are the ones published (the wiki; D-0002), the GPU booth's are for review.
        for (int i = 0; i < LINEUP.length; i++) {
            ResourceLocation kit = ResourceLocation.fromNamespaceAndPath("survivalistarmor", LINEUP[i].kit());
            Item[] set = LINEUP[i].set();
            stand(level, -7.5 + i * 2.5, 14.5, kitted(set[0], kit), kitted(set[1], kit), kitted(set[2], kit), kitted(set[3], kit));
        }
        // The cave: a closed stone room east of everything, one lantern, a stand in the booth's kit (nfx's §17: side-on
        // photographs in daylight and in a cave).
        fill(level, 28, 0, -2, 33, 4, 3, Blocks.STONE);
        fill(level, 29, 0, -1, 32, 3, 2, Blocks.AIR);
        level.setBlockAndUpdate(new BlockPos(32, ground + 3, -1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        stand(level, 30.5, -0.5, kitted(Items.IRON_HELMET), trimmed(level, kitted(Items.IRON_CHESTPLATE)), kitted(Items.IRON_LEGGINGS),
                kitted(Items.IRON_BOOTS));
        if (PHOTOS) {
        }
        // The bench Set It Down sets a kitted chestplate on.
        fill(level, 10, 0, 4, 12, 0, 4, Blocks.SMOOTH_STONE);
        setDown(sp, trimmed(level, kitted(Items.IRON_CHESTPLATE)), new BlockPos(11, ground, 4));
        // The player wears a kitted chestplate, for the first-person check and photograph; kitted
        // pieces fill the hotbar, for the inventory's.
        sp.setItemSlot(EquipmentSlot.CHEST, trimmed(level, kitted(Items.IRON_CHESTPLATE)));
        for (int i = 0; i < 4; i++) {
            sp.getInventory().setItem(i + 1, kitted(SETS[i][i]));
        }
        sp.getInventory().setItem(5, new ItemStack(Items.IRON_CHESTPLATE));
        // Every kit as an item, in the inventory's first row: each chestplate in the armour its kit is designed toward.
        for (int i = 0; i < LINEUP.length; i++) {
            sp.getInventory().setItem(9 + i, kitted(LINEUP[i].set()[1], ResourceLocation.fromNamespaceAndPath("survivalistarmor", LINEUP[i].kit())));
        }
        aim(sp, new Vec3(0.5, ground + 2.2, 8.0), new Vec3(0.5, ground + 1.0, 0.5));
        dressed = true;
    }

    private static void stand(ServerLevel level, double x, double z, ItemStack... pieces) {
        ArmorStand stand = new ArmorStand(level, x, ground, z);
        stand.setYRot(0.0F);
        stand.setYBodyRot(0.0F);
        stand.setYHeadRot(0.0F);
        for (ItemStack p : pieces) {
            stand.setItemSlot(stand.getEquipmentSlotForItem(p), p);
        }
        level.addFreshEntity(stand);
    }

    private static void mob(ServerLevel level, Mob mob, double x, double z, boolean baby) {
        if (mob == null) {
            return;
        }
        mob.moveTo(x, ground, z, 0.0F, 0.0F);
        mob.setYHeadRot(0.0F);
        mob.setYBodyRot(0.0F);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setInvulnerable(true);
        mob.setSilent(true);
        if (baby && mob instanceof Zombie zombie) {
            zombie.setBaby(true);
        }
        Item[] set = SETS[0];
        for (int k = 0; k < 4; k++) {
            ItemStack p = kitted(set[k]);
            mob.setItemSlot(((net.minecraft.world.item.ArmorItem) set[k]).getEquipmentSlot(), p);
        }
        level.addFreshEntity(mob);
    }

    /** effects: sets {@code stack} down on top of {@code at} through Set It Down's own placing, if Set It Down is loaded */
    private static void setDown(ServerPlayer sp, ItemStack stack, BlockPos at) {
        try {
            Class<?> placing = Class.forName("com.chunkworks.setitdown.Placing");
            sp.teleportTo(at.getX() + 0.5, ground + 1.0, at.getZ() + 2.5);
            sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Object result = placing.getMethod("setDown", ServerPlayer.class, BlockPos.class, Direction.class, Vec3.class)
                    .invoke(null, sp, at, Direction.UP, new Vec3(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5));
            LOG.info("booth: Set It Down placed: {}", result);
        } catch (ReflectiveOperationException e) {
            LOG.info("booth: Set It Down not here ({})", e.toString());
        }
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (BlockPos p : BlockPos.betweenClosed(x0, ground + y0, z0, x1, ground + y1, z1)) {
            level.setBlockAndUpdate(p, block.defaultBlockState());
        }
    }

    /** effects: puts the booth's player's eye at {@code from}, looking at {@code at} */
    private static void aim(ServerPlayer sp, Vec3 from, Vec3 at) {
        Vec3 d = at.subtract(from);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        sp.teleportTo(sp.serverLevel(), from.x, from.y - sp.getEyeHeight(), from.z, yaw, pitch);
    }

    // --- the film and the checks ------------------------------------------------------

    private static List<Step> plan(Minecraft mc) {
        List<Step> s = new ArrayList<>();
        int[] t = {HOLD};
        s.add(new Step(t[0], () -> checks(mc)));
        t[0] += 2;
        shot(s, t, mc, "booth-stands", new Vec3(0.5, 2.0, 7.5), new Vec3(0.5, 1.0, 0.5));
        shot(s, t, mc, "booth-stands-close", new Vec3(-5.0, 1.6, 3.5), new Vec3(-5.0, 1.1, 0.5));
        shot(s, t, mc, "booth-side", new Vec3(17.5, 1.5, 0.5), new Vec3(14.5, 1.1, 0.5));
        shot(s, t, mc, "booth-side-front", new Vec3(14.5, 1.5, 3.6), new Vec3(14.5, 1.1, 0.5));
        shot(s, t, mc, "booth-back", new Vec3(14.5, 1.5, -2.6), new Vec3(14.5, 1.1, 0.5));
        shot(s, t, mc, "booth-mobs", new Vec3(-0.5, 1.8, -1.0), new Vec3(-0.5, 0.9, -4.5));
        shot(s, t, mc, "booth-maps", new Vec3(21.5, 1.6, 3.6), new Vec3(21.5, 1.1, 0.5));
        shot(s, t, mc, "booth-maps-grazing", new Vec3(25.0, 1.3, 2.4), new Vec3(21.5, 1.1, 0.5));
        shot(s, t, mc, "booth-frames", new Vec3(0.0, 1.9, -6.2), new Vec3(0.0, 1.5, -8.5));
        shot(s, t, mc, "booth-ground", new Vec3(0.0, 1.6, 6.0), new Vec3(0.0, 0.1, 3.5));
        shot(s, t, mc, "booth-setdown", new Vec3(11.5, 2.0, 6.5), new Vec3(11.5, 1.2, 4.5));
        {
            shot(s, t, mc, "booth-lineup", new Vec3(0.0, 2.2, 23.5), new Vec3(0.0, 1.1, 14.5));
            shot(s, t, mc, "booth-lineup-quarter", new Vec3(9.5, 2.0, 21.0), new Vec3(-0.5, 1.1, 14.5));
            shot(s, t, mc, "booth-lineup-left", new Vec3(-5.0, 1.6, 18.6), new Vec3(-5.0, 1.1, 14.5));
            shot(s, t, mc, "booth-lineup-mid", new Vec3(0.0, 1.6, 18.6), new Vec3(0.0, 1.1, 14.5));
            shot(s, t, mc, "booth-lineup-right", new Vec3(5.0, 1.6, 18.6), new Vec3(5.0, 1.1, 14.5));
            shot(s, t, mc, "booth-lineup-back", new Vec3(0.5, 2.0, 7.8), new Vec3(0.0, 1.1, 14.5));
            // The side-on stand with the low sun behind the camera, and in the cave by lantern light.
            shot(s, t, mc, "booth-side-lit", new Vec3(11.2, 1.5, 0.5), new Vec3(14.5, 1.1, 0.5));
            shot(s, t, mc, "booth-side-lit-quarter", new Vec3(11.8, 1.6, 3.0), new Vec3(14.5, 1.1, 0.5));
            shot(s, t, mc, "booth-cave", new Vec3(31.7, 1.6, 2.4), new Vec3(30.5, 1.1, -0.5));
            shot(s, t, mc, "booth-cave-side", new Vec3(29.3, 1.5, 0.9), new Vec3(30.5, 1.1, -0.5));
        }
        // First person: standing, empty-handed, the kitted chestplate on, the hotbar in view.
        s.add(new Step(t[0], () -> {
            mc.options.hideGui = false;
            onServer(mc, sp -> {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.getInventory().selected = 0;
                aim(sp, new Vec3(0.5, ground + sp.getEyeHeight(), 10.5), new Vec3(0.5, ground + 1.0, 0.5));
            });
        }));
        s.add(new Step(t[0] += SETTLE, () -> shoot(mc, "booth-first-person")));
        s.add(new Step(t[0] += 4, () -> mc.setScreen(new InventoryScreen(mc.player))));
        s.add(new Step(t[0] += 20, () -> shoot(mc, "booth-inventory")));
        s.add(new Step(t[0] += 2, () -> {
            mc.setScreen(null);
            onServer(mc, SurvivalistBooth::smithing);
        }));
        s.add(new Step(t[0] += 30, () -> shoot(mc, "booth-smithing")));
        s.add(new Step(t[0] += 2, () -> {
            mc.setScreen(null);
            emiChecks();
            showInEmi("roadplate_smithing");
        }));
        s.add(new Step(t[0] += 30, () -> shoot(mc, "booth-emi")));
        s.add(new Step(t[0] += 2, () -> showInEmi("/kit_removal")));
        s.add(new Step(t[0] += 30, () -> shoot(mc, "booth-emi-removal")));
        s.add(new Step(t[0] += 2, () -> {
            mc.setScreen(null);
            mc.options.hideGui = true;
        }));
        s.add(new Step(t[0] += 10, () -> {
            verdict("all checks ran", () -> null);
            phase = Phase.DONE;
            mc.stop();
        }));
        return s;
    }

    /** effects: opens EMI's recipe screen on the plugin's recipe {@code path}, if EMI has it */
    private static void showInEmi(String path) {
        var recipe = EmiApi.getRecipeManager().getRecipe(SurvivalistArmor.id(path));
        if (recipe != null) {
            EmiApi.displayRecipe(recipe);
        }
    }

    /** EMI shows a smithing recipe only when it is the game's own: the plugin's recipes must be in its manager. */
    private static void emiChecks() {
        verdict("EMI shows every kit's smithing recipe and the removal", () -> {
            List<String> missing = new ArrayList<>();
            for (Kit kit : Kit.values()) {
                ResourceLocation id = SurvivalistArmor.id(kit.id().getPath() + "_smithing");
                if (EmiApi.getRecipeManager().getRecipe(id) == null) {
                    missing.add(id.toString());
                }
            }
            if (EmiApi.getRecipeManager().getRecipe(SurvivalistArmor.id("/kit_removal")) == null) {
                missing.add("the removal");
            }
            return missing.isEmpty() ? null : "not in EMI: " + missing;
        });
    }

    /** The smithing table, with the kit, an iron chestplate and the placeholder's scrap: its preview stand wears the result. */
    private static void smithing(ServerPlayer sp) {
        BlockPos at = new BlockPos(0, ground, 12);
        sp.serverLevel().setBlockAndUpdate(at, Blocks.SMITHING_TABLE.defaultBlockState());
        sp.openMenu(new SimpleMenuProvider((id, inventory, player) -> new SmithingMenu(id, inventory, ContainerLevelAccess.create(sp.serverLevel(), at)),
                Component.literal("Smithing")));
        if (sp.containerMenu instanceof SmithingMenu menu) {
            menu.getSlot(0).set(new ItemStack(SurvivalistContent.kitItem(Kit.ROADPLATE)));
            menu.getSlot(1).set(new ItemStack(Items.IRON_CHESTPLATE));
            menu.getSlot(2).set(new ItemStack(Items.IRON_NUGGET));
            menu.createResult();
            menu.broadcastChanges();
        }
    }

    /**
     * Whose model was drawn: each scene drawn into a {@link Capture}, the kit's render types' vertices
     * counted against its mesh, the game's own armour's against nothing.
     */
    private static void checks(Minecraft mc) {
        KitMesh mesh = KitLibrary.INSTANCE.kit(KIT);
        verdict("the kit on film is loaded (" + KIT + ")", () -> mesh != null ? null : "no kit " + KIT);
        if (mesh == null) {
            return;
        }
        verdict("its sprites are on the block atlas", () -> KitSkins.of(kitted(Items.IRON_CHESTPLATE)) != null ? null : "no skin");
        int chest = worn(mesh, Piece.CHESTPLATE), helmet = worn(mesh, Piece.HELMET), legs = worn(mesh, Piece.LEGGINGS), boots = worn(mesh, Piece.BOOTS);
        // Stands.
        Capture kittedStand = draw(mc, dress(new ArmorStand(EntityType.ARMOR_STAND, mc.level), kitted(Items.IRON_HELMET), kitted(Items.IRON_CHESTPLATE),
                kitted(Items.IRON_LEGGINGS), kitted(Items.IRON_BOOTS)));
        verdict("a kitted set on a stand is drawn by its kit, every quad",
                () -> kittedStand.of(WORN) == chest + helmet + legs + boots ? null : "kit " + kittedStand.of(WORN) + " of " + (chest + helmet + legs + boots) + ": " + kittedStand);
        verdict("and the game's own armour not at all", () -> kittedStand.mentioning("textures/models/armor") == 0 ? null : kittedStand.toString());
        Capture plainStand = draw(mc, dress(new ArmorStand(EntityType.ARMOR_STAND, mc.level), new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE)));
        verdict("a plain piece is drawn by the game (Armored Legacy), the kit not at all",
                () -> plainStand.mentioning("textures/models/armor") > 0 && plainStand.of(WORN) == 0 ? null : plainStand.toString());
        ItemStack trimmedChest = kitted(Items.IRON_CHESTPLATE);
        var r = mc.level.registryAccess();
        trimmedChest.set(DataComponents.TRIM, new ArmorTrim(r.registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.GOLD),
                r.registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.COAST)));
        Capture trimmed = draw(mc, dress(new ArmorStand(EntityType.ARMOR_STAND, mc.level), trimmedChest));
        int trimQuads = trimmable(mesh, Piece.CHESTPLATE);
        verdict("a trim on a kit is drawn on its trimmable quads",
                () -> trimmed.of(Sheets.armorTrimsSheet(false)) + trimmed.of(Sheets.armorTrimsSheet(true)) == trimQuads ? null
                        : "trim " + (trimmed.of(Sheets.armorTrimsSheet(false)) + trimmed.of(Sheets.armorTrimsSheet(true))) + " of " + trimQuads + ": " + trimmed);
        ItemStack shiny = kitted(Items.IRON_CHESTPLATE);
        shiny.enchant(r.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 1);
        Capture glinting = draw(mc, dress(new ArmorStand(EntityType.ARMOR_STAND, mc.level), shiny));
        verdict("an enchanted kit glints on every quad", () -> glinting.of(RenderType.armorEntityGlint()) == chest ? null : glinting.toString());
        ItemStack refused = kitted(Items.IRON_CHESTPLATE);
        refused.set(SurvivalistContent.KIT.get(), ResourceLocation.parse("survivalistarmor_gametest:no_such_kit"));
        Capture unknown = draw(mc, dress(new ArmorStand(EntityType.ARMOR_STAND, mc.level), refused));
        verdict("a kit no pack has a look for is drawn as the piece itself",
                () -> unknown.of(WORN) == 0 && unknown.mentioning("textures/models/armor") > 0 ? null : unknown.toString());
        // Mobs: a zombie and a baby; a villager, if Serfdom draws villagers' armour.
        Zombie adult = new Zombie(EntityType.ZOMBIE, mc.level);
        Zombie baby = new Zombie(EntityType.ZOMBIE, mc.level);
        baby.setBaby(true);
        Capture big = draw(mc, dress(adult, kitted(Items.IRON_HELMET), kitted(Items.IRON_CHESTPLATE), kitted(Items.IRON_LEGGINGS), kitted(Items.IRON_BOOTS)));
        Capture small = draw(mc, dress(baby, kitted(Items.IRON_HELMET), kitted(Items.IRON_CHESTPLATE), kitted(Items.IRON_LEGGINGS), kitted(Items.IRON_BOOTS)));
        verdict("a zombie wears the kit", () -> big.of(WORN) == chest + helmet + legs + boots ? null : big.toString());
        verdict("a baby wears it scaled as the game scales a baby's armour", () -> small.of(WORN) == big.of(WORN) && small.height() < big.height() * 0.75
                ? null : "baby " + small.height() + " adult " + big.height());
        Capture villager = draw(mc, dress(new Villager(EntityType.VILLAGER, mc.level), kitted(Items.IRON_CHESTPLATE)));
        verdict("a villager wears it (Serfdom's armour layer)", () -> villager.of(WORN) == chest ? null : villager.toString());
        // Items, in each view; a plain piece stays the pack's.
        for (ItemDisplayContext view : List.of(ItemDisplayContext.GUI, ItemDisplayContext.FIXED, ItemDisplayContext.GROUND,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            Capture item = new Capture();
            mc.getItemRenderer().renderStatic(kitted(Items.IRON_CHESTPLATE), view, LIGHT, OverlayTexture.NO_OVERLAY, new PoseStack(), item, mc.level, 0);
            int want = mesh.item(Piece.CHESTPLATE).count() * 4;
            verdict("a kitted chestplate is its kit's model in " + view.getSerializedName(),
                    () -> item.of(Sheets.cutoutBlockSheet()) == want ? null : "kit " + item.of(Sheets.cutoutBlockSheet()) + " of " + want + ": " + item);
        }
        verdict("the item renderer answers a kitted stack with the kit's model",
                () -> mc.getItemRenderer().getModel(kitted(Items.IRON_CHESTPLATE), mc.level, null, 0).getClass().getSimpleName().equals("KitItemModel")
                        ? null : mc.getItemRenderer().getModel(kitted(Items.IRON_CHESTPLATE), mc.level, null, 0).getClass().getName());
        verdict("and a plain one with the pack's", () -> !mc.getItemRenderer().getModel(new ItemStack(Items.IRON_CHESTPLATE), mc.level, null, 0)
                .getClass().getSimpleName().equals("KitItemModel") ? null : "the kit's");
        // First person: your own arm, the kitted chestplate on.
        PlayerRenderer hands = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        ItemStack wearing = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        mc.player.setItemSlot(EquipmentSlot.CHEST, kitted(Items.IRON_CHESTPLATE));
        Capture right = new Capture(), left = new Capture();
        hands.renderRightHand(new PoseStack(), right, LIGHT, mc.player);
        hands.renderLeftHand(new PoseStack(), left, LIGHT, mc.player);
        int rightArm = part(mesh, Piece.CHESTPLATE, com.chunkworks.survivalistarmor.domain.Part.RIGHT_ARM);
        int leftArm = part(mesh, Piece.CHESTPLATE, com.chunkworks.survivalistarmor.domain.Part.LEFT_ARM);
        // The sleeve alone (D-0007): fewer quads than the arm worn, exactly the sleeve's.
        KitMesh.Quads rightSleeve = mesh.sleeve(com.chunkworks.survivalistarmor.domain.Part.RIGHT_ARM);
        KitMesh.Quads leftSleeve = mesh.sleeve(com.chunkworks.survivalistarmor.domain.Part.LEFT_ARM);
        verdict("first person: the kit's sleeve alone on your right arm", () -> rightSleeve != null
                && rightSleeve.count() * 4 < rightArm && right.of(WORN) == rightSleeve.count() * 4 ? null : right + " (worn arm " + rightArm + ")");
        verdict("and on your left", () -> leftSleeve != null
                && leftSleeve.count() * 4 < leftArm && left.of(WORN) == leftSleeve.count() * 4 ? null : left + " (worn arm " + leftArm + ")");
        mc.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        Capture bare = new Capture();
        hands.renderRightHand(new PoseStack(), bare, LIGHT, mc.player);
        verdict("a plain chestplate puts nothing on your arm, as ever", () -> bare.of(WORN) == 0 ? null : bare.toString());
        mc.player.setItemSlot(EquipmentSlot.CHEST, wearing);
        // Set It Down: a kitted chestplate set down is its kit, worn by the display's stand.
        Entity display = setItDown(mc);
        verdict("a kitted chestplate set down (Set It Down) is drawn by its kit", () -> {
            if (display == null) {
                return "no display";
            }
            Capture c = draw(mc, display);
            return c.of(WORN) == chest ? null : c.toString();
        });
        // And it rests as itself: Set It Down measures a set-down piece's box once and keeps it, which must be per piece,
        // not per item, or the kitted chestplate takes the plain one's box and sinks into the table (Set It Down's D-0008).
        verdict("Set It Down measures a kitted chestplate apart from a plain one", () -> {
            try {
                var armour = Class.forName("com.chunkworks.setitdown.client.BoundsCache").getMethod("armour", ItemStack.class,
                        net.minecraft.world.level.Level.class);
                Object plain = armour.invoke(null, new ItemStack(Items.IRON_CHESTPLATE), mc.level);
                Object kitted = armour.invoke(null, kitted(Items.IRON_CHESTPLATE), mc.level);
                return plain.equals(kitted) ? "the kitted chestplate was given the plain one's box " + kitted : null;
            } catch (ReflectiveOperationException e) {
                return e.toString();
            }
        });
    }

    /** effects: returns the client's Set It Down display on the bench, or null */
    private static Entity setItDown(Minecraft mc) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getClass().getName().equals("com.chunkworks.setitdown.DisplayEntity") && Math.abs(e.getX() - 11.5) < 1.0 && Math.abs(e.getZ() - 4.5) < 1.0) {
                return e;
            }
        }
        return null;
    }

    private static <E extends LivingEntity> E dress(E entity, ItemStack... pieces) {
        for (ItemStack p : pieces) {
            entity.setItemSlot(entity.getEquipmentSlotForItem(p), p);
        }
        return entity;
    }

    /** effects: returns what drawing {@code entity} at the origin gives, through its renderer alone */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Capture draw(Minecraft mc, Entity entity) {
        Capture c = new Capture();
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        renderer.render(entity, 0.0F, 1.0F, new PoseStack(), c, LIGHT);
        return c;
    }

    private static int worn(KitMesh mesh, Piece piece) {
        return mesh.worn(piece).stream().mapToInt(pq -> pq.quads().count() * 4).sum();
    }

    private static int part(KitMesh mesh, Piece piece, com.chunkworks.survivalistarmor.domain.Part part) {
        return mesh.worn(piece).stream().filter(pq -> pq.part() == part).mapToInt(pq -> pq.quads().count() * 4).sum();
    }

    private static int trimmable(KitMesh mesh, Piece piece) {
        int n = 0;
        for (KitMesh.PartQuads pq : mesh.worn(piece)) {
            for (int i = 0; i < pq.quads().count(); i++) {
                n += Float.isNaN(pq.quads().trimUvs()[i * 8]) ? 0 : 4;
            }
        }
        return n;
    }

    /** One photograph: the player's eye moved to {@code from} (relative to the ground), looking at {@code at}. */
    private static void shot(List<Step> s, int[] t, Minecraft mc, String name, Vec3 from, Vec3 at) {
        s.add(new Step(t[0], () -> onServer(mc, sp -> aim(sp, from.add(0, ground, 0), at.add(0, ground, 0)))));
        s.add(new Step(t[0] += SETTLE, () -> {
            // A photograph framed from anywhere else is the wrong photograph: say so in the log.
            Vec3 want = from.add(0, ground, 0);
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            if (eye.distanceTo(want) > 0.05) {
                LOG.warn("booth: {} taken from {} looking {} {}, not from {}", name, eye, mc.gameRenderer.getMainCamera().getYRot(),
                        mc.gameRenderer.getMainCamera().getXRot(), want);
            }
            shoot(mc, name);
        }));
        t[0] += 2;
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (sp != null) {
                action.accept(sp);
            }
        });
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> LOG.info("booth: {}", message.getString()));
    }

    /** Runs {@code check}; null is a pass, anything else the failure's detail. */
    private static void verdict(String what, Supplier<String> check) {
        String detail;
        try {
            detail = check.get();
        } catch (RuntimeException e) {
            LOG.error("booth: check threw", e);
            detail = e.toString();
        }
        if (detail == null) {
            LOG.info("booth: PASS {}", what);
        } else {
            LOG.error("booth: FAIL {} -- {}", what, detail.toLowerCase(Locale.ROOT).isEmpty() ? "?" : detail);
        }
    }
}
