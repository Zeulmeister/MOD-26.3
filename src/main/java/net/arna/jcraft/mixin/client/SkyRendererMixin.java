package net.arna.jcraft.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.arna.jcraft.client.rendering.skybox.SkyBoxManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Shadow @Final private RenderTarget renderTarget;
    @Shadow @Final private GpuBuffer endSkyBuffer;

    @Shadow protected abstract AbstractTexture getTexture(TextureManager textureManager, Identifier location);
    @Shadow protected abstract void renderStars(RenderPass renderPass, float starBrightness, PoseStack poseStack);
    @Shadow protected abstract void renderSun(RenderPass renderPass, float alpha, PoseStack poseStack);
    @Shadow protected abstract void renderMoon(RenderPass renderPass, MoonPhase moonPhase, float alpha, PoseStack poseStack);

    @Unique
    private AbstractTexture jcraft$timeEraseSkyTexture;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jcraft$initTimeEraseSkyTexture(TextureManager textureManager, AtlasManager atlasManager,
                                                RenderTarget renderTarget, CallbackInfo ci) {
        this.jcraft$timeEraseSkyTexture = this.getTexture(textureManager, SkyBoxManager.TIME_ERASE_SKY_TEXTURE);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jcraft$renderTimeEraseSkyBox(GpuBufferSlice fog, SkyRenderState state, CallbackInfo ci) {
        final SkyBoxManager manager = SkyBoxManager.getInstance();
        if (!manager.isTimeEraseSkyActive() || this.jcraft$timeEraseSkyTexture == null) {
            return;
        }

        RenderSystem.setShaderFog(fog);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "JCraft Time Erase Sky",
                        this.renderTarget.getColorTextureView(),
                        Optional.empty(),
                        this.renderTarget.getDepthTextureView(),
                        OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);

            // Slowly revolving crimson cosmic void behind the world
            final float phase = (float) ((Util.getMillis() % 120_000L) / 120_000.0 * Math.PI * 2.0);
            final Matrix4f rotatedModelView = RenderSystem.getModelViewMatrixCopy()
                    .rotateY(phase)
                    .rotateX(Mth.sin(phase * 2.0f) * 0.18f);

            final RenderSystem.AutoStorageIndexBuffer seqBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            final GpuBuffer indexBuffer = seqBuf.getBuffer(36);
            final GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(rotatedModelView);

            renderPass.pushDebugGroup(() -> "JCraft Crimson Skybox");
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.END_SKY));
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transform);
            renderPass.setUniform(
                    "Sampler0",
                    this.jcraft$timeEraseSkyTexture.getTextureView(),
                    this.jcraft$timeEraseSkyTexture.getSampler()
            );
            renderPass.setVertexBuffer(0, this.endSkyBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, seqBuf.type());
            renderPass.drawIndexed(36, 1, 0, 0, 0);
            renderPass.popDebugGroup();

            // Counter-rotating cosmic parallax starfield layered over the crimson tears
            final PoseStack starPose = new PoseStack();
            starPose.rotate(Axis.YP, -phase * 1.6f);
            starPose.rotate(Axis.XP, phase * 0.45f);
            this.renderStars(renderPass, 0.85f, starPose);

            SkyBoxManager.timeEraseSkyDraws++;
        }
        ci.cancel();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void jcraft$renderMihCelestialTrails(RenderPass renderPass, PoseStack poseStack,
                                                 float sunAngle, float moonAngle, float starAngle,
                                                 MoonPhase moonPhase, float rainAlpha, float starBrightness,
                                                 CallbackInfo ci) {
        final SkyBoxManager manager = SkyBoxManager.getInstance();
        if (!manager.isTimeAccelSkyActive()) {
            return;
        }

        final float intensity = manager.getTimeAccelIntensity();
        final float trailPhase = (float) ((Util.getMillis() % 6_000L) / 6_000.0 * Math.PI * 2.0);

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -90.0f);

        // Solar motion echoes streaking across the accelerating sky
        final float baseCelestialAlpha = Math.max(rainAlpha, 0.65f);
        for (int i = 1; i <= 5; i++) {
            final float echoAlpha = baseCelestialAlpha * (0.48f - i * 0.08f) * intensity;
            if (echoAlpha <= 0.02f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.rotate(Axis.XP, sunAngle - i * 0.085f);
            this.renderSun(renderPass, echoAlpha, poseStack);
            poseStack.popPose();
        }

        // Lunar motion echoes
        for (int i = 1; i <= 4; i++) {
            final float echoAlpha = baseCelestialAlpha * (0.42f - i * 0.08f) * intensity;
            if (echoAlpha <= 0.02f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.rotate(Axis.XP, moonAngle - i * 0.085f);
            this.renderMoon(renderPass, moonPhase, echoAlpha, poseStack);
            poseStack.popPose();
        }

        // Star trails arcing across the dome (visible even in daylight during MIH acceleration)
        final float baseStarGlow = Math.max(starBrightness, 0.82f * intensity);
        for (int step = 0; step < 12; step++) {
            final float stepAlpha = baseStarGlow * (1.0f - step / 13.0f);
            if (stepAlpha <= 0.03f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.rotate(Axis.XP, starAngle - step * 0.08f - trailPhase * 0.15f);
            this.renderStars(renderPass, stepAlpha, poseStack);
            poseStack.popPose();
        }

        poseStack.popPose();
        SkyBoxManager.timeAccelStarTrailDraws++;
    }
}
