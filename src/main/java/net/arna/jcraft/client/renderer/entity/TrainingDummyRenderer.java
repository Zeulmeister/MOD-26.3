package net.arna.jcraft.client.renderer.entity;

import lombok.NonNull;
import net.arna.jcraft.common.entity.TrainingDummyEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class TrainingDummyRenderer extends AbstractEntityRenderer<TrainingDummyEntity> {
    public static final String ID = "training_dummy";

    public TrainingDummyRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, () -> new EntityAnimator<>(ID), b -> b.setAlpha(e -> e.isInvisible() ? 0f : 1f), ID);
    }

    @Override
    public boolean shouldRender(TrainingDummyEntity entity, @NonNull Frustum frustum,
                                double camX, double camY, double camZ, float partialTick) {
        return !entity.isInvisible() && super.shouldRender(entity, frustum, camX, camY, camZ, partialTick);
    }

    @Override
    protected boolean shouldShowName(TrainingDummyEntity entity, double distSq) {
        return false;
    }
}
