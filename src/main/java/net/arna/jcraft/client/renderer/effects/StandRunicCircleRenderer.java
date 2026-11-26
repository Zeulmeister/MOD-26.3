package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.common.util.JVisualFeedback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Projects an expanding, counter-rotating runic sigil ring on the ground beneath
 * Stands while charging Heavy/Ultimate skills or long windups.
 */
@Environment(EnvType.CLIENT)
public final class StandRunicCircleRenderer {
    private static final Identifier RUNIC_TEXTURE = JCraft.id("textures/effect/stand_runic_circle.png");
    public static volatile int renderedCirclesThisSession = 0;

    private StandRunicCircleRenderer() {}

    public static void render(final PoseStack matrices, final Vec3 camPos, final ClientLevel world,
                              final float tickDelta, final SubmitNodeCollector collector) {
        if (world == null || collector == null || !JClientConfig.getInstance().isRunicChargeCirclesEnabled()) {
            return;
        }
        for (final Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof StandEntity<?, ?> stand) || !stand.isAlive() || stand.isInvisible()) {
                continue;
            }
            final int rem = stand.getChargeRingTicks();
            final int max = stand.getChargeRingMax();
            if (rem <= 0 || max <= 0) {
                continue;
            }

            final float progress = Mth.clamp(1.0f - (rem - tickDelta) / (float) max, 0.0f, 1.0f);
            final LivingEntity anchor = (!stand.isRemote() && stand.getUser() != null) ? stand.getUser() : stand;
            final double gx = anchor.getX();
            final double gz = anchor.getZ();
            final double gy = resolveGroundY(world, gx, anchor.getY(), gz) + 0.035;

            final int rgb = JVisualFeedback.resolveAuraRgb(stand.getUser(), stand);
            final boolean abyssDark = (rgb == 0x0C0C16);

            final float outerHalf = 1.15f + progress * 0.85f;
            final float innerHalf = outerHalf * 0.64f;
            final int outerAlpha = Mth.clamp(Math.round((0.68f + 0.32f * Mth.sin(progress * Mth.PI)) * 245.0f), 115, 250);
            final int innerAlpha = Mth.clamp(Math.round(outerAlpha * 0.90f), 95, 245);
            final float spinDeg = (stand.tickCount + tickDelta) * 5.2f;

            final int outR = abyssDark ? 12 : ((rgb >> 16) & 0xFF);
            final int outG = abyssDark ? 12 : ((rgb >> 8) & 0xFF);
            final int outB = abyssDark ? 20 : (rgb & 0xFF);

            final int inR = abyssDark ? 220 : Math.min(255, outR + 55);
            final int inG = abyssDark ? 230 : Math.min(255, outG + 55);
            final int inB = abyssDark ? 255 : Math.min(255, outB + 55);

            // Outer rotating circle
            matrices.pushPose();
            matrices.translate(gx - camPos.x, gy - camPos.y, gz - camPos.z);
            matrices.rotateDegrees(Axis.YP, spinDeg);
            final var outerType = abyssDark
                    ? RenderTypes.entityTranslucent(RUNIC_TEXTURE)
                    : RenderTypes.entityTranslucentEmissive(RUNIC_TEXTURE);
            collector.submitCustomGeometry(matrices, outerType, (pose, vc) -> {
                Matrix4f mat = pose.pose();
                emitVertex(vc, mat, -outerHalf, 0.0f, -outerHalf, 0.0f, 0.0f, outR, outG, outB, outerAlpha);
                emitVertex(vc, mat, -outerHalf, 0.0f, outerHalf, 0.0f, 1.0f, outR, outG, outB, outerAlpha);
                emitVertex(vc, mat, outerHalf, 0.0f, outerHalf, 1.0f, 1.0f, outR, outG, outB, outerAlpha);
                emitVertex(vc, mat, outerHalf, 0.0f, -outerHalf, 1.0f, 0.0f, outR, outG, outB, outerAlpha);
            });
            matrices.popPose();

            // Inner counter-rotating sigil ring
            matrices.pushPose();
            matrices.translate(gx - camPos.x, gy + 0.008 - camPos.y, gz - camPos.z);
            matrices.rotateDegrees(Axis.YP, -spinDeg * 1.55f);
            collector.submitCustomGeometry(matrices, RenderTypes.entityTranslucentEmissive(RUNIC_TEXTURE), (pose, vc) -> {
                Matrix4f mat = pose.pose();
                emitVertex(vc, mat, -innerHalf, 0.0f, -innerHalf, 0.0f, 0.0f, inR, inG, inB, innerAlpha);
                emitVertex(vc, mat, -innerHalf, 0.0f, innerHalf, 0.0f, 1.0f, inR, inG, inB, innerAlpha);
                emitVertex(vc, mat, innerHalf, 0.0f, innerHalf, 1.0f, 1.0f, inR, inG, inB, innerAlpha);
                emitVertex(vc, mat, innerHalf, 0.0f, -innerHalf, 1.0f, 0.0f, inR, inG, inB, innerAlpha);
            });
            matrices.popPose();

            renderedCirclesThisSession++;
        }
    }

    private static double resolveGroundY(ClientLevel world, double x, double baseY, double z) {
        BlockPos pos = BlockPos.containing(x, baseY - 0.05, z);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos check = pos.below(dy);
            var shape = world.getBlockState(check).getCollisionShape(world, check);
            if (!shape.isEmpty()) {
                return check.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
            }
        }
        return baseY;
    }

    private static void emitVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                   float u, float v, int r, int g, int b, int a) {
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728640)
                .setNormal(0.0f, 1.0f, 0.0f);
    }
}
