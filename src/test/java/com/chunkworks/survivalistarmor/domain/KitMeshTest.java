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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chunkworks.survivalistarmor.domain.Projects.Cube;
import com.chunkworks.survivalistarmor.domain.Projects.Folder;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A kit's quads, worn and as an item.
 *
 * <p>Partitions:
 * <ul>
 *   <li>a face: free; covered by another cube on its part; under a cube on another part; on or
 *       under a turned cube; under chain-link;</li>
 *   <li>its material: trimmable, hardware, lit; garment or material sheet;</li>
 *   <li>the piece: dressed, not dressed; one part, several.</li>
 * </ul>
 */
class KitMeshTest {
    private static final List<String> TEXTURES = List.of("garment", "material");
    private static final double EPS = 1e-6;

    @Test
    void aFaceAnotherCubeOnItsPartCoversIsLeftOut() {
        // A plate lying flat on the chest: its back face is inside the shell and is left out; the
        // shell's front, only partly covered, stays.
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        Cube plate = new Cube("plate.chest", -3, 15, 3, 3, 22, 4);
        KitMesh mesh = KitMesh.of(model(Projects.chestBody(shell, plate)));
        assertEquals(11, quads(mesh, Piece.CHESTPLATE).count());
    }

    @Test
    void aCubeOnAnotherPartCoversNothing() {
        // The arm's shoulder pad overlaps the body at rest, but the arm swings: nothing is left out.
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        Cube pad = new Cube("plate.pauldron", -9, 22, -3, -4, 26, 3);
        KitMesh mesh = KitMesh.of(model(new Folder("chestplate", new Folder("body", shell), new Folder("right_arm", pad))));
        int total = mesh.worn(Piece.CHESTPLATE).stream().mapToInt(p -> p.quads().count()).sum();
        assertEquals(12, total);
    }

    @Test
    void chainLinkCoversNothing() {
        // A strap under a mesh panel shows through its holes: nothing is left out.
        Cube strap = new Cube("strap.under", -2, 15, 3, 2, 16, 3.2);
        Cube mesh = new Cube("mesh.panel", -3, 14, 2.9, 3, 17, 3.4);
        KitMesh kit = KitMesh.of(model(Projects.chestBody(strap, mesh)));
        assertEquals(12, quads(kit, Piece.CHESTPLATE).count());
    }

    @Test
    void aTurnedCubeNeitherCoversNorIsCovered() {
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        Cube inside = new Cube("plate.inner", -1, 15, -1, 1, 17, 1).turned(0, 16, 0, 0, 10, 0);
        KitMesh mesh = KitMesh.of(model(Projects.chestBody(shell, inside)));
        assertEquals(12, quads(mesh, Piece.CHESTPLATE).count());
    }

    @Test
    void wornQuadsAreInBlocksInTheirPartsFrame() {
        // The body's front face at the project's z 3 is the model's z -3, in blocks -3/16.
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        KitMesh.Quads q = quads(KitMesh.of(model(Projects.chestBody(shell))), Piece.CHESTPLATE);
        boolean found = false;
        for (int i = 0; i < q.count(); i++) {
            if (q.normals()[i * 3 + 2] < -0.999) {
                found = true;
                for (int k = 0; k < 4; k++) {
                    assertEquals(-3 / 16.0, q.positions()[i * 12 + k * 3 + 2], EPS);
                }
            }
        }
        assertTrue(found, "a face toward the model's -z");
    }

    @Test
    void trimmableFacesCarryTrimCoordinatesAndHardwareNone() {
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        Cube buckle = new Cube("buckle.belt", -1, 12, 3, 1, 14, 4);
        KitMesh.Quads q = quads(KitMesh.of(model(Projects.chestBody(shell, buckle))), Piece.CHESTPLATE);
        int trimmed = 0, bare = 0;
        for (int i = 0; i < q.count(); i++) {
            if (Float.isNaN(q.trimUvs()[i * 8])) {
                bare++;
            } else {
                trimmed++;
            }
        }
        assertEquals(6, trimmed, "the shell's six faces");
        assertEquals(5, bare, "the buckle's five showing faces");
    }

    @Test
    void theSlotAndTheGlowAreCarried() {
        Cube lens = new Cube("lens_lit.left", 1, 28, 5, 3, 30, 6).texture(0);
        Cube plate = new Cube("plate.helm", -5, 23, -5, 5, 33, 5).texture(1);
        KitMesh.Quads q = quads(KitMesh.of(model(new Folder("helmet", new Folder("head", lens, plate)))), Piece.HELMET);
        int lit = 0, material = 0;
        for (int i = 0; i < q.count(); i++) {
            lit += q.lit()[i] ? 1 : 0;
            material += q.material()[i] ? 1 : 0;
        }
        assertEquals(5, lit, "the lens's showing faces (its back is in the helmet)");
        assertEquals(6, material, "the helmet plate's faces");
    }

    @Test
    void anItemIsTheWholePieceCentredAndFillingABlock() {
        Cube shell = new Cube("canvas.shell", -5, 11, -3, 5, 25, 3);
        Cube arm = new Cube("canvas.sleeve", -9, 11, -3, -3, 25, 3);
        KitMesh mesh = KitMesh.of(model(new Folder("chestplate", new Folder("body", shell), new Folder("right_arm", arm))));
        KitMesh.Quads item = mesh.item(Piece.CHESTPLATE);
        assertEquals(12, item.count());
        float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < item.count() * 4; i++) {
            for (int a = 0; a < 3; a++) {
                lo[a] = Math.min(lo[a], item.positions()[i * 3 + a]);
                hi[a] = Math.max(hi[a], item.positions()[i * 3 + a]);
            }
        }
        // The piece is 14 wide (x -9..5), 14 tall, 6 deep: its longest side fills the block.
        assertEquals(0.0, lo[0], EPS);
        assertEquals(1.0, hi[0], EPS);
        assertEquals(0.5, (lo[1] + hi[1]) / 2, EPS);
        assertEquals(0.5, (lo[2] + hi[2]) / 2, EPS);
    }

    @Test
    void aPieceTheKitDoesNotDressHasNothing() {
        KitMesh mesh = KitMesh.of(model(Projects.chestBody(new Cube("canvas.shell", -5, 11, -3, 5, 25, 3))));
        assertTrue(mesh.dresses(Piece.CHESTPLATE));
        assertFalse(mesh.dresses(Piece.BOOTS));
        assertTrue(mesh.worn(Piece.BOOTS).isEmpty());
        assertNull(mesh.item(Piece.BOOTS));
    }

    private static KitModel model(Object... top) {
        return KitModel.parse(Projects.project(64, 64, TEXTURES, top)).model();
    }

    private static KitMesh.Quads quads(KitMesh mesh, Piece piece) {
        List<KitMesh.PartQuads> parts = mesh.worn(piece);
        assertEquals(1, parts.size());
        return parts.get(0).quads();
    }
}
