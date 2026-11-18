package net.arna.jcraft.client.renderer.entity;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import lombok.NonNull;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.CarbonDioxideRadarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class CarbonDioxideRadarRenderer extends AbstractEntityRenderer<CarbonDioxideRadarEntity> {
    private static final Identifier MODEL = JCraft.id("geo/carbon_dioxide_radar.geo.json");
    private static final Identifier ANIM = JCraft.id("animations/carbon_dioxide_radar.animation.json");

    private static final Identifier TEX_DEF = JCraft.id("textures/entity/carbon_dioxide_radar/default.png");
    private static final Identifier TEX_SKIN1 = JCraft.id("textures/entity/carbon_dioxide_radar/skin1.png");
    private static final Identifier TEX_SKIN2 = JCraft.id("textures/entity/carbon_dioxide_radar/skin2.png");
    private static final Identifier TEX_SKIN3 = JCraft.id("textures/entity/carbon_dioxide_radar/skin3.png");

    private static final double HEAD_Y_OFFSET = 0.25;
    private static final double SWIM_HEAD_FWD = 0.7;

    public CarbonDioxideRadarRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.builder(e -> MODEL, CarbonDioxideRadarRenderer::texture)
                        .setAnimatorProvider(RadarAnimator::new)
                        .setAlpha(CarbonDioxideRadarRenderer::alpha)
                        .setRenderType(e -> RenderTypes.entityTranslucent(texture(e)))
                        .setPrerenderEntry(ctx -> {
                            CarbonDioxideRadarEntity entity = ctx.animatable();
                            if (entity != null && entity.getUser() != null) {
                                updateRotation(entity, ctx.partialTick(), ctx.poseStack(), entity.getUser());
                            }
                            return ctx;
                        })
                        .build(),
                context
        );
    }

    private static Identifier texture(final CarbonDioxideRadarEntity entity) {
        return switch (entity.getSkin()) {
            case 1  -> TEX_SKIN1;
            case 2  -> TEX_SKIN2;
            case 3  -> TEX_SKIN3;
            default -> TEX_DEF;
        };
    }

    private static float alpha(final CarbonDioxideRadarEntity entity) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == entity.getUser()) {
            return 0.1F;
        }
        return 1.0F;
    }

    private static void updateRotation(final CarbonDioxideRadarEntity entity, final float partialTick,
                                       final com.mojang.blaze3d.vertex.PoseStack poseStack, final LivingEntity user) {
        final double userX = Mth.lerp(partialTick, user.xo, user.getX());
        final double userY = Mth.lerp(partialTick, user.yo, user.getY());
        final double userZ = Mth.lerp(partialTick, user.zo, user.getZ());

        final double headX, headY, headZ;
        final boolean horizontal = user.isVisuallySwimming() || user.isFallFlying();
        if (horizontal) {
            final Vec3 look = user.getViewVector(partialTick);
            final double centerY = userY + user.getBbHeight() * 0.5;
            headX = userX + look.x * SWIM_HEAD_FWD;
            headY = centerY + look.y * SWIM_HEAD_FWD;
            headZ = userZ + look.z * SWIM_HEAD_FWD;
        } else {
            headX = userX;
            headY = userY + user.getEyeHeight() - HEAD_Y_OFFSET;
            headZ = userZ;
        }

        final double entLerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
        final double entLerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
        final double entLerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        poseStack.translate(headX - entLerpX, headY - entLerpY, headZ - entLerpZ);

        final float headYaw = Mth.lerp(partialTick, user.yHeadRotO, user.yHeadRot);
        entity.setYRot(headYaw);
        entity.yRotO = headYaw;
        final float headPitch = Mth.lerp(partialTick, user.xRotO, user.getXRot());
        entity.setXRot(headPitch);
        entity.xRotO = headPitch;
    }

    private static class RadarAnimator extends EntityAnimator<CarbonDioxideRadarEntity> {
        public RadarAnimator() {
            super(ANIM);
        }

        @Override
        public void setCustomAnimations(final @NonNull CarbonDioxideRadarEntity animatable, final float partialTicks) {
            super.setCustomAnimations(animatable, partialTicks);
            AzBakedModel model = context().boneCache().getBakedModel();
            if (model != null) {
                model.getBone("base").ifPresent((AzBone bone) -> bone.setRotX(-animatable.getXRot() * Mth.DEG_TO_RAD));
            }
        }
    }
}
