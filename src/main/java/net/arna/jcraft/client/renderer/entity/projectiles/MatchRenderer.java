package net.arna.jcraft.client.renderer.entity.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.projectile.MatchProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class MatchRenderer extends EntityRenderer<MatchProjectile, MatchRenderer.MatchRenderState> {
    private static final Identifier TEXTURE = JCraft.id("textures/entity/projectiles/match.png");

    public static class MatchRenderState extends EntityRenderState {
        public int frame;
        public boolean skipClose;
    }

    public MatchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public MatchRenderState createRenderState() {
        return new MatchRenderState();
    }

    @Override
    protected int getBlockLightLevel(@NotNull MatchProjectile entity, @NotNull BlockPos pos) {
        return 15;
    }

    @Override
    public void extractRenderState(@NonNull MatchProjectile entity, @NonNull MatchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.frame = entity.tickCount % 4;
        state.skipClose = entity.tickCount < 2 && state.distanceToCameraSq < 12.25;
    }

    @Override
    public void submit(@NonNull MatchRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        if (state.skipClose) return;
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Matrix4f().rotation(cameraState.orientation));
        poseStack.mulPose(new org.joml.Matrix4f().rotation(Axis.YP.rotationDegrees(180.0F)));
        float v0 = state.frame * 0.25f;
        float v1 = v0 + 0.25f;
        float s = 0.25f;
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, vc) -> {
            vc.addVertex(pose, -s, -s, 0).setColor(255, 255, 255, 255).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            vc.addVertex(pose,  s, -s, 0).setColor(255, 255, 255, 255).setUv(1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            vc.addVertex(pose,  s,  s, 0).setColor(255, 255, 255, 255).setUv(1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            vc.addVertex(pose, -s,  s, 0).setColor(255, 255, 255, 255).setUv(0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
