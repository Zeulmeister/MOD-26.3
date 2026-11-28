package net.arna.jcraft.client.renderer.entity.stands;

import lombok.NonNull;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.renderer.entity.StandEntityModelRenderer;
import net.arna.jcraft.client.renderer.entity.layer.OriginGlowLayer;
import net.arna.jcraft.client.util.JAzRenderAccess;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

import java.util.UUID;
import java.util.function.Function;

/**
 * The {@link StandEntityRenderer} for {@link OriginEntity}.
 */
@Environment(EnvType.CLIENT)
public class OriginRenderer extends StandEntityRenderer<OriginEntity> {
    public static final float BARRAGE_PHANTOM_ARM_ALPHA = 0.58f;

    public OriginRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, b -> b
                        .setModelRenderer(OriginModelRenderer::new)
                        .setPrerenderEntry(originPreRenderEntry())
                        .addRenderLayer(new StandEntityRenderer.StandHandItemsRenderLayer<>())
                        .addRenderLayer(new OriginGlowLayer()),
                JStandTypeRegistry.ORIGIN.get(), 0f, 0f);
    }

    /**
     * Computes Origin's effective render alpha, enforcing first-person combat/barrage
     * translucency matching Star Platinum and other Stands even for borrowed long-reach moves.
     */
    public static float getEffectiveAlpha(final @NonNull OriginEntity origin, final float partialTick) {
        if (origin.isUiPreview()) {
            return 1.0f;
        }
        final float base = StandEntityRenderer.getAlpha(origin, partialTick);
        if (!StandEntityRenderer.shouldApplyAlpha(origin) || origin.hasAlphaOverride()) {
            return base;
        }
        final JClientConfig config = JClientConfig.getInstance();
        if (!config.isDynamicFirstPersonStandOpacity()) {
            return base;
        }
        final float mult = config.getFirstPersonStandOpacityMult();
        if (origin.getMoveStun() > 0) {
            final float combatCap = (origin.getState() == OriginEntity.State.BARRAGE)
                    ? (0.34f * mult)
                    : (0.48f * mult);
            return Math.min(base, combatCap);
        }
        if (origin.isBlocking()) {
            return Math.min(base, 0.55f * mult);
        }
        return base;
    }

    private static Function<AzRendererPipelineContext<UUID, OriginEntity>, AzRendererPipelineContext<UUID, OriginEntity>> originPreRenderEntry() {
        final Function<AzRendererPipelineContext<UUID, OriginEntity>, AzRendererPipelineContext<UUID, OriginEntity>> basePre =
                StandEntityRenderer.preRenderEntry();
        return pc -> {
            final AzRendererPipelineContext<UUID, OriginEntity> out = basePre.apply(pc);
            final OriginEntity origin = out.animatable();
            final float targetAlpha = getEffectiveAlpha(origin, out.partialTick());
            if (targetAlpha < JAzRenderAccess.getAlpha(out)) {
                JAzRenderAccess.setAlpha(out, Math.max(0.02f, targetAlpha));
            }
            return out;
        };
    }

    @Override
    public int getBlockLightLevel(final @NonNull OriginEntity entity, final @NonNull BlockPos pos) {
        return entity.isUiPreview() ? 15 : super.getBlockLightLevel(entity, pos);
    }

    private static final class OriginModelRenderer extends StandEntityModelRenderer<OriginEntity> {
        private OriginModelRenderer(
                final @NonNull AzRendererPipeline<UUID, OriginEntity> pipeline,
                final @NonNull AzLayerRenderer<UUID, OriginEntity> layerRenderer) {
            super(pipeline, layerRenderer);
        }

        @Override
        protected void renderCubesOfBone(
                final @NonNull AzRendererPipelineContext<UUID, OriginEntity> pc,
                final @NonNull AzBone bone) {
            if (bone.getName().startsWith("barrage_")) {
                final int prevColor = pc.renderColor();
                JAzRenderAccess.setAlpha(pc, JAzRenderAccess.getAlpha(pc) * BARRAGE_PHANTOM_ARM_ALPHA);
                try {
                    super.renderCubesOfBone(pc, bone);
                } finally {
                    pc.setRenderColor(prevColor);
                }
                return;
            }
            super.renderCubesOfBone(pc, bone);
        }
    }
}
