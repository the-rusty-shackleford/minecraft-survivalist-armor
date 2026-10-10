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

import static com.chunkworks.survivalistarmor.domain.KitModelTest.assertUv;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Where a trim lands on a kit's face: exactly where the game puts it on its own armour.
 *
 * <p>The expected numbers are the game's: ModelPart.Cube textures a body box from texture offset
 * (16, 16) with its north face (the front, toward -z) from u 20 to 28 and v 20 to 32, its corner at
 * (max x, min y) taking (28, 20); an arm box from (40, 16) with its west face from u 40 to 44; a
 * left limb is the same box mirrored.
 *
 * <p>Partitions:
 * <ul>
 *   <li>the face: on the game's box face exactly; smaller; standing out past it; facing off the axes;</li>
 *   <li>the box: unmirrored (right arm, body), mirrored (left arm); outer layer, inner layer;</li>
 *   <li>the face's direction: front, side, top.</li>
 * </ul>
 */
class TrimProjectionTest {
    private static final double EPS = 1e-9;

    @Test
    void aFaceOnTheBodysFrontTakesTheGamesOwnCoordinates() {
        // The outer body box grown one: x -5..5, y -1..13, front at z -3.
        List<Vec> front = List.of(new Vec(5, -1, -3), new Vec(-5, -1, -3), new Vec(-5, 13, -3), new Vec(5, 13, -3));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.BODY, front, new Vec(0, 0, -1));
        assertUv(28 / 64.0, 20 / 32.0, uv.get(0));
        assertUv(20 / 64.0, 20 / 32.0, uv.get(1));
        assertUv(20 / 64.0, 32 / 32.0, uv.get(2));
        assertUv(28 / 64.0, 32 / 32.0, uv.get(3));
    }

    @Test
    void aSmallerFaceTakesTheCoordinatesUnderIt() {
        // A plate covering the upper left quarter of the chest (the figure's left is +x).
        List<Vec> plate = List.of(new Vec(0, -1, -4), new Vec(5, -1, -4), new Vec(5, 6, -4), new Vec(0, 6, -4));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.BODY, plate, new Vec(0, 0, -1));
        assertUv(24 / 64.0, 20 / 32.0, uv.get(0));
        assertUv(28 / 64.0, 20 / 32.0, uv.get(1));
        assertUv(28 / 64.0, 26 / 32.0, uv.get(2));
    }

    @Test
    void aFaceStandingOutPastTheBoxRunsOnIntoItsLayoutAndNoFurther() {
        // Twice as wide as the body: its far corners run into the side faces' columns of the body's
        // layout, and stop at the layout's edges (u 16 and 40).
        List<Vec> wide = List.of(new Vec(30, 0, -4), new Vec(-30, 0, -4), new Vec(-30, 12, -4), new Vec(30, 12, -4));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.BODY, wide, new Vec(0, 0, -1));
        assertEquals(40 / 64.0, uv.get(0).u(), EPS);
        assertEquals(16 / 64.0, uv.get(1).u(), EPS);
    }

    @Test
    void theRightArmsOuterSideTakesItsWestFace() {
        // The right arm grown one: x -4..2 in its frame; its outer side is west (-x), u 40 to 44, the
        // front edge (z -3) taking u 44.
        List<Vec> outer = List.of(new Vec(-4, -3, -3), new Vec(-4, -3, 3), new Vec(-4, 11, 3), new Vec(-4, 11, -3));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.RIGHT_ARM, outer, new Vec(-1, 0, 0));
        assertUv(44 / 64.0, 20 / 32.0, uv.get(0));
        assertUv(40 / 64.0, 20 / 32.0, uv.get(1));
        assertUv(40 / 64.0, 32 / 32.0, uv.get(2));
    }

    @Test
    void theLeftArmIsTheRightMirrored() {
        // The left arm grown one: x -2..4 in its frame; mirrored, its outer side (+x) takes the same
        // columns, the front edge still at u 44, so the picture reads reversed, as on the game's armour.
        List<Vec> outer = List.of(new Vec(4, -3, -3), new Vec(4, -3, 3), new Vec(4, 11, 3), new Vec(4, 11, -3));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.LEFT_ARM, outer, new Vec(1, 0, 0));
        assertUv(44 / 64.0, 20 / 32.0, uv.get(0));
        assertUv(40 / 64.0, 20 / 32.0, uv.get(1));
    }

    @Test
    void aShouldersTopTakesTheTopFace() {
        // The top of the right arm (toward -y in the model's frame): u 44 to 48, v 16 to 20.
        List<Vec> top = List.of(new Vec(-4, -3, -3), new Vec(2, -3, -3), new Vec(2, -3, 3), new Vec(-4, -3, 3));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.RIGHT_ARM, top, new Vec(0, -1, 0));
        for (Uv u : uv) {
            assertEquals(true, u.u() >= 44 / 64.0 - EPS && u.u() <= 48 / 64.0 + EPS, "u in the top face's columns: " + u);
            assertEquals(true, u.v() >= 16 / 32.0 - EPS && u.v() <= 20 / 32.0 + EPS, "v in the top face's rows: " + u);
        }
    }

    @Test
    void aFaceOffTheAxesTakesTheNearestBoxFace() {
        // Facing mostly forward, a little up: the front face's columns.
        List<Vec> tilted = List.of(new Vec(2, 0, -4), new Vec(-2, 0, -4), new Vec(-2, 4, -3.5), new Vec(2, 4, -3.5));
        List<Uv> uv = TrimProjection.project(Piece.Layer.OUTER, Part.BODY, tilted, new Vec(0, -0.2, -0.98).unit());
        for (Uv u : uv) {
            assertEquals(true, u.u() >= 20 / 64.0 - EPS && u.u() <= 28 / 64.0 + EPS, "u in the front's columns: " + u);
        }
    }

    @Test
    void theInnerLayerIsGrownHalfAsMuch() {
        // Leggings: the body box grown 0.5, x -4.5..4.5, front at z -2.5.
        List<Vec> front = List.of(new Vec(4.5, -0.5, -2.5), new Vec(-4.5, -0.5, -2.5), new Vec(-4.5, 12.5, -2.5), new Vec(4.5, 12.5, -2.5));
        List<Uv> uv = TrimProjection.project(Piece.Layer.INNER, Part.BODY, front, new Vec(0, 0, -1));
        assertUv(28 / 64.0, 20 / 32.0, uv.get(0));
        assertUv(20 / 64.0, 32 / 32.0, uv.get(2));
    }
}
