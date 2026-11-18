package net.arna.jcraft.client.renderer.entity.stands;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.arna.jcraft.client.util.JAzRenderAccess;

import com.mojang.math.Axis;
import lombok.NonNull;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.client.renderer.entity.StandEntityModelRenderer;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer.ANIMATION_STR_TEMPLATE;
import static net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer.MODEL_STR_TEMPLATE;
import static net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer.*;

/**
 * The {@link StandEntityRenderer} for {@link AerosmithEntity}.
 */
@Environment(EnvType.CLIENT)
public class AerosmithRenderer {
    public static StandEntityRenderer<AerosmithEntity> of(final @NonNull EntityRendererProvider.Context context) {
        final StandType type = JStandTypeRegistry.AEROSMITH.get();
        final var id = type.getId().getPath();
        final var animation = JCraft.id(ANIMATION_STR_TEMPLATE.formatted(id));
        final var model = type.getId().withPath(MODEL_STR_TEMPLATE.formatted(id));
        final var texture = getTextureLocation(type);

        return StandEntityRenderer.of(
                AzEntityRendererConfig
                        .<AerosmithEntity>builder(e -> model, StandEntityRenderer::getTextureLocation)
                        .setAnimatorProvider(() -> new AerosmithAnimator(animation))
                        .setModelRenderer((ctx, layerRenderer) ->
                                new AerosmithModelRenderer(ctx, layerRenderer))
                        .setRenderType(AerosmithRenderer::aerosmithRenderType)
                        .setPrerenderEntry(preRenderEntry())
                        .build(),
                context
        );
    }

    /**
     * Selects the render type for Aerosmith.
     * <p>
     * Normally defers to the standard stand logic (entityNoOutline in first-person to avoid
     * the stand obscuring the camera, entityTranslucent otherwise). However, when the local
     * player is Aerosmith's user AND the stand is flying remotely out of line-of-sight,
     * entityNoOutline is replaced with entityTranslucent so that the vanilla
     * OutlineBufferSource can capture the geometry and produce the glow effect.
     */
    private static RenderType aerosmithRenderType(AerosmithEntity stand) {
        final Identifier texture = StandEntityRenderer.getTextureLocation(stand);
        final Player player = Minecraft.getInstance().player;
        if (StandEntityRenderer.standIsFirstPersonViewers(stand)) {
            if (stand.isRemote() && !stand.isInLineOfSight() && stand.getUser() == player) {
                // Glow-outline case: use entityTranslucent so the outline buffer captures us.
                return RenderTypes.entityTranslucent(texture);
            }
            return RenderTypes.entityTranslucent(texture, false);
        }
        return RenderTypes.entityTranslucent(texture);
    }

    public static class AerosmithAnimator extends StandAnimator<AerosmithEntity> {
        public AerosmithAnimator(final @NonNull Identifier animation) {
            super(animation, false, false, 0f, 0f, 0f);
        }
    }

    public static class AerosmithModelRenderer extends StandEntityModelRenderer<AerosmithEntity> {
        
        public AerosmithModelRenderer(
                final @NonNull AzRendererPipeline<UUID, AerosmithEntity> entityRendererPipeline,
                final @NonNull AzLayerRenderer<UUID, AerosmithEntity> layerRenderer) {
            super(entityRendererPipeline, layerRenderer);
                    }

        @Override
        public void render(@NonNull AzRendererPipelineContext<UUID, AerosmithEntity> ctx, boolean isReRender) {
            final var animatable = ctx.animatable();
            final var partialTick = ctx.partialTick();

            if (StandEntityRenderer.getAlpha(animatable, partialTick) <= 0.0f) {
                return;
            }

            

            final var poseStack = ctx.poseStack();

            poseStack.pushPose();
            final float lerpBodyRot = getStandLerpRot(animatable, partialTick);
            final float nativeScale = animatable.getScale();
            final float ageInTicks = animatable.tickCount + partialTick;

            poseStack.scale(nativeScale, nativeScale, nativeScale);

            if (animatable.isRemote()) {
                final Vec3 vel = animatable.getDeltaMovement();

                final double horizontalDistance = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                final float targetYaw = (float) (Math.atan2(-vel.x, vel.z) * (180D / Math.PI)) + 180.0f;
                final float targetPitch = (float) (Math.atan2(-vel.y, horizontalDistance) * (180D / Math.PI));

                final float lerpSpeed = 0.15f;

                animatable.yaw = Mth.rotLerp(lerpSpeed, animatable.yaw, targetYaw);
                animatable.pitch = Mth.rotLerp(lerpSpeed, animatable.pitch, targetPitch);

                poseStack.rotate(Axis.YN.rotationDegrees(Mth.rotLerp(partialTick, animatable.oldYaw, animatable.yaw)));
                poseStack.rotate(Axis.XN.rotationDegrees(Mth.rotLerp(partialTick, animatable.oldPitch, animatable.pitch)));
                poseStack.rotate(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTick, animatable.oldRoll, animatable.roll)));

                animatable.oldPitch = animatable.pitch;
                animatable.oldYaw = animatable.yaw;
                animatable.oldRoll = animatable.roll;
            } else {
                applyRotations(animatable, poseStack, ageInTicks, lerpBodyRot, partialTick, nativeScale);
            }

            if (!isReRender) {
                final var animator = entityRendererPipeline.getRenderer().getAnimator();

                if (animator != null) {
                    handleAnimation(animator, animatable, ctx.partialTick());
                }
            }

            JAzRenderAccess.setModelRenderTranslations(entityRendererPipeline, poseStack.last().pose());

            if (ctx.vertexConsumer() != null) { // actually render
                ctx.rendererPipeline().updateAnimatedTextureFrame(animatable);

                for (var bone : ctx.bakedModel().getTopLevelBones()) {
                    renderRecursively(ctx, bone, isReRender);
                }

                var config = ctx.rendererPipeline().config();
                config.renderEntry(ctx);
            }

            poseStack.popPose();
        }
    }
}
