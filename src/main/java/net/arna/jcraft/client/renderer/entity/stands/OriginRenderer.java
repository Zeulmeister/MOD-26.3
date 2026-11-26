package net.arna.jcraft.client.renderer.entity.stands;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.client.renderer.entity.layer.OriginGlowLayer;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

/**
 * The {@link StandEntityRenderer} for {@link OriginEntity}.
 */
@Environment(EnvType.CLIENT)
public class OriginRenderer extends StandEntityRenderer<OriginEntity> {
    public OriginRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, b -> b
                        .addRenderLayer(new StandEntityRenderer.StandHandItemsRenderLayer<>())
                        .addRenderLayer(new OriginGlowLayer()),
                JStandTypeRegistry.ORIGIN.get(), 0f, 0f);
    }

    @Override
    public int getBlockLightLevel(final @NonNull OriginEntity entity, final @NonNull BlockPos pos) {
        return entity.isUiPreview() ? 15 : super.getBlockLightLevel(entity, pos);
    }
}
