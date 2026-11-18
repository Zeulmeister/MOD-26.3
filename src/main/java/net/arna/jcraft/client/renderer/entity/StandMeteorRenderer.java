package net.arna.jcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.model.entity.StandMeteorModel;
import net.arna.jcraft.common.entity.StandMeteorEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class StandMeteorRenderer extends MobRenderer<StandMeteorEntity, LivingEntityRenderState, StandMeteorModel> {
    public static final ModelLayerLocation STAND_METEOR_LAYER = new ModelLayerLocation(JCraft.id("stand_meteor"), "main");
    private static final Identifier TEXTURE = JCraft.id("textures/entity/stand_meteor.png");

    public StandMeteorRenderer(final EntityRendererProvider.Context context) {
        super(context, new StandMeteorModel(context.bakeLayer(STAND_METEOR_LAYER)), 0.2F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public void submit(@NonNull LivingEntityRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.rotate(Axis.XP.rotationDegrees(state.ageInTicks));
        poseStack.rotate(Axis.YP.rotationDegrees(state.ageInTicks));
        super.submit(state, poseStack, collector, cameraState);
        poseStack.popPose();
    }

    @Override
    public @NonNull Identifier getTextureLocation(@NonNull final LivingEntityRenderState state) {
        return TEXTURE;
    }
}
