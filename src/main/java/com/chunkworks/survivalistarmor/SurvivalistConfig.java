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
package com.chunkworks.survivalistarmor;

import com.chunkworks.survivalistarmor.domain.Blacklist;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's settings (D-0001). The server's are sent to every player with the world, so a piece the
 * server refuses is drawn as itself on every screen too.
 */
public final class SurvivalistConfig {
    private SurvivalistConfig() {}

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.BooleanValue CONSUME_TEMPLATE;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_ARMS;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        BLACKLIST = server
                .comment("Armour no kit goes on: a mod's id refuses every piece of that mod (\"geckolib_armor\"),",
                        "an item's id that piece alone (\"create:copper_backtank\"). Refused pieces are turned away",
                        "at the smithing table, and a kit one carries from before is not drawn. Create's backtanks",
                        "are here by default: Create draws the tank on the back from a layer of its own, through any kit.")
                .defineListAllowEmpty("blacklist", List.of("create:copper_backtank", "create:netherite_backtank"), () -> "",
                        o -> o instanceof String);
        CONSUME_TEMPLATE = server
                .comment("Whether applying a kit uses the kit up, as a trim template is used up. False keeps it.")
                .define("consumeTemplate", true);
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        FIRST_PERSON_ARMS = client
                .comment("Whether a kitted chestplate's sleeve is drawn on your own arm in first person.",
                        "Turn it off if another mod draws your arms and the two fight.")
                .define("firstPersonArms", true);
        CLIENT_SPEC = client.build();
    }

    /** The blacklist, and the config list it was made from: remade only when the list is a new one. */
    private record Made(List<? extends String> from, Blacklist blacklist) {}

    private static volatile Made made = new Made(List.of(), Blacklist.NONE);

    /** effects: returns the server's blacklist as now configured (none before the server's config is loaded) */
    public static Blacklist blacklist() {
        if (!SERVER_SPEC.isLoaded()) {
            return Blacklist.NONE;
        }
        List<? extends String> now = BLACKLIST.get();
        Made m = made;
        if (m.from() != now) {
            m = new Made(now, Blacklist.of(List.copyOf(now)));
            made = m;
        }
        return m.blacklist();
    }

    /** effects: returns whether a kit is used up when applied; true while the server's config is not loaded */
    public static boolean consumeTemplate() {
        return !SERVER_SPEC.isLoaded() || CONSUME_TEMPLATE.get();
    }

    /** effects: returns whether a kitted sleeve is drawn on your own arm; true while the client's config is not loaded */
    public static boolean firstPersonArms() {
        return !CLIENT_SPEC.isLoaded() || FIRST_PERSON_ARMS.get();
    }
}
