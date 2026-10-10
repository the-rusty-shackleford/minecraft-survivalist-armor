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
package com.chunkworks.survivalistarmor.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chunkworks.survivalistarmor.domain.Projects.Cube;
import com.chunkworks.survivalistarmor.domain.Projects.Folder;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Reading a kit's Blockbench project.
 *
 * <p>Partitions:
 * <ul>
 *   <li>where a cube sits: in a piece's part folder; deeper in it; in a piece with no part; in a
 *       part the piece doesn't ride on; in an unknown folder; outside every folder;</li>
 *   <li>the project's layout: format 5 (folders in a list), older (folders named in the outliner);</li>
 *   <li>texture coordinates: per face; box UV; a turned face; a face without a texture;</li>
 *   <li>the texture: garment, material, anything else;</li>
 *   <li>the cube: square; inflated; turned itself; in a turned folder; flat; hidden; not a cube;</li>
 *   <li>the whole: no elements; nothing bound to a part.</li>
 * </ul>
 */
class KitModelTest {
    private static final List<String> TEXTURES = List.of("garment.png", "material.png");
    private static final double EPS = 1e-9;

    @Test
    void aCubeInAPiecesPartIsBoundToBoth() {
        KitModel.Parsed p = KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(new Cube("canvas.shell", -5, 11, -3, 5, 25, 3))));
        assertEquals(6, p.model().faces().size());
        for (KitFace f : p.model().faces()) {
            assertEquals(Piece.CHESTPLATE, f.piece());
            assertEquals(Part.BODY, f.part());
            assertEquals("canvas", f.material());
        }
        assertTrue(p.warnings().isEmpty(), p.warnings().toString());
    }

    @Test
    void aDeeperFolderStillRidesOnItsPart() {
        Folder straps = new Folder("straps", new Cube("strap.bandolier", -4, 14, 3, 4, 15, 4));
        KitModel.Parsed p = KitModel.parse(Projects.project(64, 64, TEXTURES, new Folder("chestplate", new Folder("left_arm", straps))));
        assertEquals(Part.LEFT_ARM, p.model().faces().get(0).part());
    }

    @Test
    void cubesOutsideAPiecesPartAreSkippedAndNamed() {
        Cube good = new Cube("plate.helm", -5, 23, -5, 5, 33, 5);
        Cube noPart = new Cube("loose", 0, 0, 0, 1, 1, 1);
        Cube wrongPart = new Cube("odd", 0, 0, 0, 1, 1, 1);
        Cube unknown = new Cube("lost", 0, 0, 0, 1, 1, 1);
        Cube outside = new Cube("orphan", 0, 0, 0, 1, 1, 1);
        KitModel.Parsed p = KitModel.parse(Projects.project(64, 64, TEXTURES,
                new Folder("helmet", new Folder("head", good), noPart, new Folder("body", wrongPart)),
                new Folder("cape", new Folder("body", unknown)), outside));
        assertEquals(6, p.model().faces().size());
        assertTrue(p.model().faces().stream().allMatch(f -> f.piece() == Piece.HELMET && f.part() == Part.HEAD));
        assertEquals(4, p.warnings().size(), p.warnings().toString());
        for (String name : List.of("loose", "odd", "lost", "orphan")) {
            assertTrue(p.warnings().stream().anyMatch(w -> w.startsWith(name + ":")), name + " named in " + p.warnings());
        }
    }

    @Test
    void anOlderProjectNamesItsFoldersInTheOutliner() {
        KitModel.Parsed p = KitModel.parse(Projects.legacy(64, 64, TEXTURES, new Folder("boots", new Folder("right_leg", new Cube("hide.boot", -5, 0, -3, 1, 4, 3)))));
        assertEquals(Piece.BOOTS, p.model().faces().get(0).piece());
        assertEquals(Part.RIGHT_LEG, p.model().faces().get(0).part());
    }

    @Test
    void perFaceCoordinatesAreFractionsCountedFromTheTop() {
        Cube c = new Cube("plate.chest", -4, 12, -2, 4, 24, 2).uv("south", 8, 16, 24, 40);
        KitFace south = face(KitModel.parse(Projects.project(64, 128, TEXTURES, Projects.chestBody(c))).model(), new Vec(0, 0, 1));
        // Corners run top-left, bottom-left, bottom-right, top-right seen from outside.
        assertUv(8 / 64.0, 16 / 128.0, south.uvs().get(0));
        assertUv(8 / 64.0, 40 / 128.0, south.uvs().get(1));
        assertUv(24 / 64.0, 40 / 128.0, south.uvs().get(2));
        assertUv(24 / 64.0, 16 / 128.0, south.uvs().get(3));
        assertVec(new Vec(-4, 24, 2), south.corners().get(0));
        assertVec(new Vec(-4, 12, 2), south.corners().get(1));
        assertVec(new Vec(4, 12, 2), south.corners().get(2));
        assertVec(new Vec(4, 24, 2), south.corners().get(3));
    }

    @Test
    void aTurnedFaceTurnsItsTextureRectangle() {
        Cube c = new Cube("plate.chest", -4, 12, -2, 4, 24, 2).uv("south", 0, 0, 16, 16).turn("south", 90);
        KitFace south = face(KitModel.parse(Projects.project(16, 16, TEXTURES, Projects.chestBody(c))).model(), new Vec(0, 0, 1));
        // Turned a quarter clockwise, as the game's FaceBakery turns it: the top-left corner shows the
        // texture's bottom-left, the bottom-left its bottom-right.
        assertUv(0, 1, south.uvs().get(0));
        assertUv(1, 1, south.uvs().get(1));
        assertUv(1, 0, south.uvs().get(2));
        assertUv(0, 0, south.uvs().get(3));
    }

    @Test
    void boxUvLaysTheClassicEntityLayoutFromItsOffset() {
        Cube c = new Cube("canvas.shell", -4, 12, -2, 4, 24, 2).boxUv(16, 16);
        KitModel m = KitModel.parse(Projects.project(64, 32, TEXTURES, Projects.chestBody(c))).model();
        KitFace north = face(m, new Vec(0, 0, -1));
        // north: u from 16+4 to 16+4+8, v from 16+4 to 16+4+12, as the game's own box UV lays a body.
        double us = north.uvs().stream().mapToDouble(Uv::u).min().orElseThrow();
        double ue = north.uvs().stream().mapToDouble(Uv::u).max().orElseThrow();
        double vs = north.uvs().stream().mapToDouble(Uv::v).min().orElseThrow();
        double ve = north.uvs().stream().mapToDouble(Uv::v).max().orElseThrow();
        assertEquals(20 / 64.0, us, EPS);
        assertEquals(28 / 64.0, ue, EPS);
        assertEquals(20 / 32.0, vs, EPS);
        assertEquals(32 / 32.0, ve, EPS);
    }

    @Test
    void aFaceWithoutATextureIsNotDrawn() {
        Cube c = new Cube("canvas.shell", -4, 12, -2, 4, 24, 2).untextured("down").untextured("up");
        assertEquals(4, KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(c))).model().faces().size());
    }

    @Test
    void theTexturesNameTheSlots() {
        Cube g = new Cube("canvas.shell", -4, 12, -2, 4, 24, 2).texture(0);
        Cube m = new Cube("plate.chest", -3, 14, 2, 3, 22, 3).texture(1);
        Cube o = new Cube("tape.x", -1, 14, 3, 1, 15, 3.5).texture(2);
        KitModel.Parsed p = KitModel.parse(Projects.project(64, 64, List.of("garment", "material.png", "decal.png"), Projects.chestBody(g, m, o)));
        assertTrue(p.model().faces().stream().filter(f -> f.material().equals("canvas")).allMatch(f -> f.slot() == KitFace.Slot.GARMENT));
        assertTrue(p.model().faces().stream().filter(f -> f.material().equals("plate")).allMatch(f -> f.slot() == KitFace.Slot.MATERIAL));
        assertTrue(p.model().faces().stream().filter(f -> f.material().equals("tape")).allMatch(f -> f.slot() == KitFace.Slot.GARMENT));
        assertEquals(1, p.warnings().size(), p.warnings().toString());
    }

    @Test
    void everyFacePointsOutOfItsCubeAndRunsCounterClockwise() {
        Cube c = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3).inflate(0.5);
        KitModel m = KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(c))).model();
        Vec centre = new Vec(0, 18, 0);
        for (KitFace f : m.faces()) {
            Vec middle = f.corners().get(0).plus(f.corners().get(2)).times(0.5);
            assertTrue(f.normal().dot(middle.minus(centre)) > 0, "outward: " + f);
            // Counter-clockwise seen from outside: the corners' winding agrees with the normal.
            Vec wound = f.corners().get(1).minus(f.corners().get(0)).cross(f.corners().get(2).minus(f.corners().get(1)));
            assertTrue(wound.dot(f.normal()) > 0, "winding: " + f);
        }
    }

    @Test
    void inflateGrowsTheCubeAndItsBox() {
        Cube c = new Cube("canvas.shell", -4, 12, -2, 4, 24, 2).inflate(1);
        KitFace f = KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(c))).model().faces().get(0);
        assertNotNull(f.box());
        assertVec(new Vec(-5, 11, -3), f.box().min());
        assertVec(new Vec(5, 25, 3), f.box().max());
    }

    @Test
    void aTurnedCubeIsTurnedAboutItsOriginAndHasNoBox() {
        // A quarter turn about Y at the origin carries the south face (+z) to the east (+x).
        Cube c = new Cube("plate.chest", -1, 12, -2, 1, 14, 2).turned(0, 0, 0, 0, 90, 0);
        KitModel m = KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(c))).model();
        KitFace east = face(m, new Vec(1, 0, 0));
        assertNull(east.box());
        for (Vec corner : east.corners()) {
            assertEquals(2, corner.x(), EPS);
        }
    }

    @Test
    void aTurnedFolderTurnsWhatItHolds() {
        Folder tilted = new Folder("pouch", new Cube("pouch.a", -1, 12, -2, 1, 14, 2)).turned(0, 0, 0, 0, 90, 0);
        KitModel m = KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(tilted))).model();
        KitFace east = face(m, new Vec(1, 0, 0));
        assertNull(east.box());
        for (Vec corner : east.corners()) {
            assertEquals(2, corner.x(), EPS);
        }
    }

    @Test
    void aFlatCubeDrawsOnlyItsTwoBroadFaces() {
        Cube c = new Cube("mesh.panel", -4, 12, 3, 4, 24, 3);
        assertEquals(2, KitModel.parse(Projects.project(64, 64, TEXTURES, Projects.chestBody(c))).model().faces().size());
    }

    @Test
    void aHiddenCubeAndAMeshAreSkipped() {
        Cube hidden = new Cube("canvas.ghost", -4, 12, -2, 4, 24, 2).hidden();
        Cube shown = new Cube("canvas.shell", -4, 12, -2, 4, 24, 2);
        String mesh = "{\"name\":\"blob\",\"type\":\"mesh\",\"uuid\":\"m0\",\"vertices\":{},\"faces\":{}}";
        KitModel.Parsed p = KitModel.parse(Projects.withRaw(64, 64, TEXTURES, List.of(mesh), Projects.chestBody(hidden, shown)));
        assertEquals(6, p.model().faces().size());
        assertTrue(p.warnings().stream().anyMatch(w -> w.startsWith("blob:")), p.warnings().toString());
    }

    @Test
    void aProjectWithoutElementsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> KitModel.parse("{\"elements\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> KitModel.parse("not json"));
    }

    @Test
    void aProjectWithNothingOnAPartIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> KitModel.parse(Projects.project(64, 64, TEXTURES, new Folder("cape", new Cube("x", 0, 0, 0, 1, 1, 1)))));
    }

    @Test
    void aTextureWithItsOwnUvSizeCountsItsFacesInIt() {
        // The material sheet is 32 by 16 where the project is 64 by 64: a face on it at (8, 4)-(16, 8)
        // is a quarter across and a quarter down the sheet.
        String text = "{\"resolution\":{\"width\":64,\"height\":64},"
                + "\"textures\":[{\"name\":\"garment\"},{\"name\":\"material\",\"uv_width\":32,\"uv_height\":16}],"
                + "\"elements\":[{\"name\":\"plate.chest\",\"type\":\"cube\",\"uuid\":\"c\",\"from\":[-4,12,2],\"to\":[4,24,3],"
                + "\"faces\":{\"south\":{\"uv\":[8,4,16,8],\"texture\":1}}}],"
                + "\"outliner\":[{\"name\":\"chestplate\",\"children\":[{\"name\":\"body\",\"children\":[\"c\"]}]}]}";
        KitFace south = KitModel.parse(text).model().faces().get(0);
        assertEquals(KitFace.Slot.MATERIAL, south.slot());
        assertUv(8 / 32.0, 4 / 16.0, south.uvs().get(0));
        assertUv(16 / 32.0, 8 / 16.0, south.uvs().get(2));
    }

    @Test
    void theTextureSizeIsTheProjectsResolution() {
        KitModel m = KitModel.parse(Projects.project(1024, 512, TEXTURES, Projects.chestBody(new Cube("canvas.shell", -4, 12, -2, 4, 24, 2)))).model();
        assertEquals(1024, m.textureWidth());
        assertEquals(512, m.textureHeight());
    }

    static KitFace face(KitModel m, Vec normal) {
        return m.faces().stream().filter(f -> f.normal().dot(normal) > 0.999).findFirst().orElseThrow();
    }

    static void assertVec(Vec want, Vec got) {
        assertEquals(want.x(), got.x(), EPS, "x of " + got);
        assertEquals(want.y(), got.y(), EPS, "y of " + got);
        assertEquals(want.z(), got.z(), EPS, "z of " + got);
    }

    static void assertUv(double u, double v, Uv got) {
        assertEquals(u, got.u(), EPS, "u of " + got);
        assertEquals(v, got.v(), EPS, "v of " + got);
    }
}
