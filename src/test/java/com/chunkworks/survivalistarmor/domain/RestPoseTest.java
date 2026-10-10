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

import static com.chunkworks.survivalistarmor.domain.KitModelTest.assertVec;

import com.chunkworks.survivalistarmor.domain.Projects.Cube;
import com.chunkworks.survivalistarmor.domain.Projects.Folder;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A kit's cube drawn where the game draws its own armour: a project cube placed over the game's
 * outer armour box for a part lands, in that part's frame, exactly on the box the game builds with
 * {@code addBox} and a grow of one unit (HumanoidModel.createMesh's numbers). And the figure's front
 * is the game's face.
 *
 * <p>Partitions: the part (head, body, each arm, each leg); a point vs a direction.
 */
class RestPoseTest {
    private static final List<String> TEXTURES = List.of("garment");

    @Test
    void overTheGamesBodyBox() {
        // addBox(-4, 0, -2, 8, 12, 4) grown 1 at pivot (0, 0, 0).
        lands(Piece.CHESTPLATE, Part.BODY, new Cube("canvas", -5, 11, -3, 5, 25, 3), new Vec(-5, -1, -3), new Vec(5, 13, 3));
    }

    @Test
    void overTheGamesHeadBox() {
        // addBox(-4, -8, -4, 8, 8, 8) grown 1 at pivot (0, 0, 0).
        lands(Piece.HELMET, Part.HEAD, new Cube("plate", -5, 23, -5, 5, 33, 5), new Vec(-5, -9, -5), new Vec(5, 1, 5));
    }

    @Test
    void overTheGamesRightArmBox() {
        // addBox(-3, -2, -2, 4, 12, 4) grown 1 at pivot (-5, 2, 0).
        lands(Piece.CHESTPLATE, Part.RIGHT_ARM, new Cube("canvas", -9, 11, -3, -3, 25, 3), new Vec(-4, -3, -3), new Vec(2, 11, 3));
    }

    @Test
    void overTheGamesLeftArmBox() {
        // addBox(-1, -2, -2, 4, 12, 4) grown 1 at pivot (5, 2, 0).
        lands(Piece.CHESTPLATE, Part.LEFT_ARM, new Cube("canvas", 3, 11, -3, 9, 25, 3), new Vec(-2, -3, -3), new Vec(4, 11, 3));
    }

    @Test
    void overTheGamesLegBoxes() {
        // addBox(-2, 0, -2, 4, 12, 4) grown 1 at pivots (-1.9, 12, 0) and (1.9, 12, 0).
        lands(Piece.BOOTS, Part.RIGHT_LEG, new Cube("hide", -4.9, -1, -3, 1.1, 13, 3), new Vec(-3, -1, -3), new Vec(3, 13, 3));
        lands(Piece.BOOTS, Part.LEFT_LEG, new Cube("hide", -1.1, -1, -3, 4.9, 13, 3), new Vec(-3, -1, -3), new Vec(3, 13, 3));
    }

    @Test
    void theFiguresFrontIsTheGamesFace() {
        // The project's figure faces +z; the game's model has its face toward -z, its head up toward -y.
        assertVec(new Vec(0, 0, -1), RestPose.directionToModel(new Vec(0, 0, 1)));
        assertVec(new Vec(0, -1, 0), RestPose.directionToModel(new Vec(0, 1, 0)));
        assertVec(new Vec(1, 0, 0), RestPose.directionToModel(new Vec(1, 0, 0)));
    }

    /** effects: asserts the cube, on {@code part} of {@code piece}, spans {@code min} to {@code max} in the part's frame */
    private static void lands(Piece piece, Part part, Cube cube, Vec min, Vec max) {
        KitModel m = KitModel.parse(Projects.project(64, 64, TEXTURES, new Folder(piece.folder(), new Folder(part.folder(), cube)))).model();
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (KitFace f : m.faces()) {
            for (Vec c : f.corners()) {
                Vec p = RestPose.toPart(part, c);
                lo[0] = Math.min(lo[0], p.x());
                lo[1] = Math.min(lo[1], p.y());
                lo[2] = Math.min(lo[2], p.z());
                hi[0] = Math.max(hi[0], p.x());
                hi[1] = Math.max(hi[1], p.y());
                hi[2] = Math.max(hi[2], p.z());
            }
        }
        assertVec(min, new Vec(lo[0], lo[1], lo[2]));
        assertVec(max, new Vec(hi[0], hi[1], hi[2]));
    }
}
