package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arna.jcraft.api.splatter.JSplatterManager;
import net.arna.jcraft.api.splatter.Splatter;
import net.arna.jcraft.common.splatter.SplatterSection;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SplatterEffectRenderer {

    public static void render(final PoseStack matrices, final Vec3 camPos, final ClientLevel world, final float tickDelta) {
        // Legacy overload preserved; MC 26.3 renders via SubmitNodeCollector.
    }

    public static void render(final PoseStack matrices, final Vec3 camPos, final ClientLevel world,
                              final float tickDelta, final SubmitNodeCollector collector) {
        if (world == null || collector == null) {
            return;
        }
        final JSplatterManager manager = JUtils.getSplatterManager(world);
        if (manager == null) {
            return;
        }

        manager.iterateSplatters(splatter -> renderSplatter(splatter, matrices, camPos, world, tickDelta, collector));
    }

    private static void renderSplatter(final Splatter splatter, final PoseStack matrices, final Vec3 camPos,
                                       final ClientLevel world, final float tickDelta, final SubmitNodeCollector collector) {
        if (splatter == null || splatter.isRemoved()) {
            return;
        }
        final float strength = splatter.getStrength(tickDelta);
        if (strength <= 0.01f) {
            return;
        }
        final int alpha = Mth.clamp(Math.round(235.0f * strength), 0, 255);
        final float eps = splatter.getOffset() + 0.004f;

        for (final SplatterSection section : splatter.getSections()) {
            if (section == null || section.isRemoved()) {
                continue;
            }
            final Direction dir = section.getDirection();
            final Vector3f min = section.getMinPos();
            final Vector3f max = section.getMaxPos();
            final Vec2 minUv = section.getMinUv();
            final Vec2 maxUv = section.getMaxUv();
            final int light = LightCoordsUtil.getLightCoords(world, section.getBlockPos());

            final float nx = dir.getStepX();
            final float ny = dir.getStepY();
            final float nz = dir.getStepZ();

            final float x0 = (float) (min.x() - camPos.x) + nx * eps;
            final float y0 = (float) (min.y() - camPos.y) + ny * eps;
            final float z0 = (float) (min.z() - camPos.z) + nz * eps;
            final float x1 = (float) (max.x() - camPos.x) + nx * eps;
            final float y1 = (float) (max.y() - camPos.y) + ny * eps;
            final float z1 = (float) (max.z() - camPos.z) + nz * eps;

            final float u0 = minUv.x, v0 = minUv.y, u1 = maxUv.x, v1 = maxUv.y;

            collector.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(splatter.getTexture()), (pose, vc) -> {
                final Matrix4f mat = pose.pose();
                switch (dir.getAxis()) {
                    case Y -> {
                        if (dir == Direction.UP) {
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y0, z1, u0, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z1, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z0, u1, v0, alpha, light, nx, ny, nz);
                        } else {
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z0, u1, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z1, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y0, z1, u0, v1, alpha, light, nx, ny, nz);
                        }
                    }
                    case X -> {
                        if (dir == Direction.EAST) {
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y1, z0, u0, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y1, z1, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y0, z1, u1, v0, alpha, light, nx, ny, nz);
                        } else {
                            vtx(vc, mat, x0, y0, z1, u1, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y1, z1, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y1, z0, u0, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                        }
                    }
                    case Z -> {
                        if (dir == Direction.SOUTH) {
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z0, u1, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y1, z0, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y1, z0, u0, v1, alpha, light, nx, ny, nz);
                        } else {
                            vtx(vc, mat, x0, y1, z0, u0, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y1, z0, u1, v1, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x1, y0, z0, u1, v0, alpha, light, nx, ny, nz);
                            vtx(vc, mat, x0, y0, z0, u0, v0, alpha, light, nx, ny, nz);
                        }
                    }
                }
            });
        }
    }

    private static void vtx(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                            float u, float v, int alpha, int light, float nx, float ny, float nz) {
        vc.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
