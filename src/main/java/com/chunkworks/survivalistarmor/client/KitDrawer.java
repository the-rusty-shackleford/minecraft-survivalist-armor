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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Writes a kit's quads to a vertex buffer, as the game's model parts write theirs: each corner
 * through the pose, each normal through its normal matrix. Three ways: the kit itself, from its
 * sprites on the kits' own atlas; the trim, at each trimmable quad's trim coordinates (the buffer is the
 * trim sprite's own, which places them on its atlas); the glint, at coordinates laid by position at
 * the density the game's armour glint has. A lit quad is drawn at full brightness.
 *
 * <p>Only ever called on the render thread: the scratch vectors are shared.
 */
final class KitDrawer {
    private KitDrawer() {}

    /** A block's worth of the game's 64-by-32 armour layout: sixteen units, a quarter across and half down. */
    private static final float GLINT_U_PER_BLOCK = 0.25F;
    private static final float GLINT_V_PER_BLOCK = 0.5F;

    private static final Vector3f POSITION = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();

    /** effects: writes q's quads, each from the garment or the material sheet (the latter tinted by {@code materialColour}) */
    static void kit(PoseStack.Pose pose, VertexConsumer out, KitMesh.Quads q, TextureAtlasSprite garment, TextureAtlasSprite material,
                    int materialColour, int light, int overlay) {
        Matrix4f m = pose.pose();
        float[] p = q.positions(), uv = q.uvs();
        for (int i = 0; i < q.count(); i++) {
            boolean sheet = q.material()[i];
            TextureAtlasSprite s = sheet ? material : garment;
            int colour = sheet ? materialColour : -1;
            int l = q.lit()[i] ? LightTexture.FULL_BRIGHT : light;
            pose.transformNormal(q.normals()[i * 3], q.normals()[i * 3 + 1], q.normals()[i * 3 + 2], NORMAL);
            for (int k = 0; k < 4; k++) {
                int c = i * 12 + k * 3;
                m.transformPosition(p[c], p[c + 1], p[c + 2], POSITION);
                out.addVertex(POSITION.x, POSITION.y, POSITION.z, colour, s.getU(uv[i * 8 + k * 2]), s.getV(uv[i * 8 + k * 2 + 1]),
                        overlay, l, NORMAL.x, NORMAL.y, NORMAL.z);
            }
        }
    }

    /** effects: writes q's trimmable quads at their trim coordinates, for a buffer the trim's sprite wraps */
    static void trim(PoseStack.Pose pose, VertexConsumer out, KitMesh.Quads q, int light, int overlay) {
        Matrix4f m = pose.pose();
        float[] p = q.positions(), t = q.trimUvs();
        for (int i = 0; i < q.count(); i++) {
            if (Float.isNaN(t[i * 8])) {
                continue;
            }
            pose.transformNormal(q.normals()[i * 3], q.normals()[i * 3 + 1], q.normals()[i * 3 + 2], NORMAL);
            for (int k = 0; k < 4; k++) {
                int c = i * 12 + k * 3;
                m.transformPosition(p[c], p[c + 1], p[c + 2], POSITION);
                out.addVertex(POSITION.x, POSITION.y, POSITION.z, -1, t[i * 8 + k * 2], t[i * 8 + k * 2 + 1], overlay, light,
                        NORMAL.x, NORMAL.y, NORMAL.z);
            }
        }
    }

    /** effects: writes all of q's quads for the glint, laid by position */
    static void glint(PoseStack.Pose pose, VertexConsumer out, KitMesh.Quads q, int light, int overlay) {
        Matrix4f m = pose.pose();
        float[] p = q.positions();
        for (int i = 0; i < q.count(); i++) {
            pose.transformNormal(q.normals()[i * 3], q.normals()[i * 3 + 1], q.normals()[i * 3 + 2], NORMAL);
            for (int k = 0; k < 4; k++) {
                int c = i * 12 + k * 3;
                m.transformPosition(p[c], p[c + 1], p[c + 2], POSITION);
                out.addVertex(POSITION.x, POSITION.y, POSITION.z, -1, (p[c] + p[c + 2]) * GLINT_U_PER_BLOCK, p[c + 1] * GLINT_V_PER_BLOCK,
                        overlay, light, NORMAL.x, NORMAL.y, NORMAL.z);
            }
        }
    }
}
