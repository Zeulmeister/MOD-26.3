package net.arna.jcraft.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.util.JAzRenderAccess;
import net.arna.jcraft.common.entity.stand.TheSunEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Unshaded additive emissive corona layer for {@link TheSunEntity}.
 * Renders a primary radiant photosphere pass plus a subtly pulsing outer halo.
 */
@Environment(EnvType.CLIENT)
public class SunGlowLayer extends AbstractRenderLayer<TheSunEntity> {
    private static final List<Identifier> SKINS = IntStream.range(0, 4).mapToObj(
            i -> JCraft.id("textures/entity/stands/the_sun/glow_" + i + ".png")).toList();

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, TheSunEntity> pc) {
        final TheSunEntity sun = pc.animatable();
        final int skinIdx = Math.floorMod(sun.getSkin(), SKINS.size());
        final RenderType additiveGlow = RenderTypes.eyes(SKINS.get(skinIdx));

        final float age = sun.tickCount + pc.partialTick();
        final float pulse = 0.88f + 0.12f * Mth.sin(age * 0.22f);

        // Pass 1: crisp fullbright unshaded emissive core overlay
        pc.setRenderType(additiveGlow);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(additiveGlow));
        setDefaultGlow(pc);
        JAzRenderAccess.setRenderColorRgb(pc, 1.0f, 0.96f, 0.88f);
        JAzRenderAccess.setAlpha(pc, pulse);
        pc.rendererPipeline().reRender(pc);

        // Pass 2: soft expanding solar corona halo
        final PoseStack poseStack = pc.poseStack();
        final float haloScale = 1.055f + 0.025f * Mth.sin(age * 0.17f + 1.1f);
        poseStack.pushPose();
        poseStack.scale(haloScale, haloScale, haloScale);
        pc.setRenderType(additiveGlow);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(additiveGlow));
        setDefaultGlow(pc);
        JAzRenderAccess.setRenderColorRgb(pc, 1.0f, 0.82f, 0.45f);
        JAzRenderAccess.setAlpha(pc, 0.42f * pulse);
        pc.rendererPipeline().reRender(pc);
        poseStack.popPose();
    }
}
