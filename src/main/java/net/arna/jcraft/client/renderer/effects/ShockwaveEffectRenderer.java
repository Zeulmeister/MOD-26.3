package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent;
import net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent.Shockwave;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders expanding 3D punch/block air shockwave rings from {@link CommonShockwaveHandlerComponent}.
 */
public class ShockwaveEffectRenderer {
    private static final Map<Shockwave.Type, List<Identifier>> TEXTURES = new EnumMap<>(Shockwave.Type.class);
    private static final List<Shockwave> toRender = new ArrayList<>();

    static {
        for (Shockwave.Type type : Shockwave.Type.values()) {
            List<Identifier> frames = new ArrayList<>(Shockwave.MAX_AGE);
            for (int i = 0; i < Shockwave.MAX_AGE; i++) {
                frames.add(JCraft.id("textures/effect/shockwave/" + type.getName() + "_" + i + ".png"));
            }
            TEXTURES.put(type, List.copyOf(frames));
        }
    }

    public static void render(final PoseStack matrices, final Vec3 camPos, final ClientLevel world, final float tickDelta) {
        // Legacy overload preserved; 26.3 rendering submits geometry through SubmitNodeCollector.
    }

    public static void render(final PoseStack matrices, final Vec3 camPos, final ClientLevel world,
                              final float tickDelta, final SubmitNodeCollector collector) {
        if (world == null || collector == null) {
            return;
        }
        final CommonShockwaveHandlerComponent handler = JComponentPlatformUtils.getShockwaveHandler(world);
        if (handler == null) {
            return;
        }
        final List<Shockwave> active = handler.getShockwaves();
        if (active.isEmpty()) {
            return;
        }

        toRender.clear();
        toRender.addAll(active);

        for (final Shockwave shockwave : toRender) {
            final int frame = shockwave.getFrame();
            final List<Identifier> typeTextures = TEXTURES.get(shockwave.getType());
            if (typeTextures == null || frame < 0 || frame >= typeTextures.size()) {
                continue;
            }
            final Identifier texture = typeTextures.get(frame);
            final float progress = Mth.clamp((shockwave.getAge() + tickDelta) / (float) Shockwave.MAX_AGE, 0.0f, 1.0f);
            final float half = 0.5f * shockwave.getScale() * (0.85f + 0.30f * progress);
            final float forwardDrift = (shockwave.getType() == Shockwave.Type.DEFAULT ? 0.35f : 0.15f)
                    * shockwave.getScale() * progress;
            final int alpha = Mth.clamp(Math.round(255.0f * (1.0f - progress * 0.38f)), 48, 255);
            final int light = LightCoordsUtil.lightCoordsWithEmission(
                    LightCoordsUtil.getLightCoords(world, shockwave.getBlockPos()),
                    8
            );

            matrices.pushPose();
            matrices.translate(
                    shockwave.getX() - camPos.x,
                    shockwave.getY() - camPos.y,
                    shockwave.getZ() - camPos.z
            );
            matrices.rotateDegrees(Axis.YP, -shockwave.getYaw());
            matrices.rotateDegrees(Axis.XP, shockwave.getPitch());

            collector.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(texture), (pose, vertexConsumer) -> {
                final Matrix4f mat = pose.pose();
                vertex(vertexConsumer, mat, -half, -half, forwardDrift, 0.0f, 0.0f, alpha, light);
                vertex(vertexConsumer, mat, half, -half, forwardDrift, 0.0f, 1.0f, alpha, light);
                vertex(vertexConsumer, mat, half, half, forwardDrift, 1.0f, 1.0f, alpha, light);
                vertex(vertexConsumer, mat, -half, half, forwardDrift, 1.0f, 0.0f, alpha, light);
            });

            matrices.popPose();
        }
    }

    private static void vertex(final VertexConsumer consumer, final Matrix4f matrix,
                               final float x, final float y, final float z,
                               final float u, final float v,
                               final int alpha, final int light) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);
    }
}
