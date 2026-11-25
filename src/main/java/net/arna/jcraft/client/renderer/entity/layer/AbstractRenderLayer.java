package net.arna.jcraft.client.renderer.entity.layer;
import net.arna.jcraft.client.util.JAzRenderAccess;

import lombok.NonNull;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public abstract class AbstractRenderLayer<T extends Entity> implements AzRenderLayer<UUID, T> {

    protected AbstractRenderLayer() {
        /* Left empty on purpose */
    }

    @Override
    public void preRender(final @NonNull AzRendererPipelineContext<UUID, T> pc) {
        /* Left empty on purpose */
    }

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, T> pc) {
        /* Left empty on purpose */
    }

    @Override
    public void renderForBone(final @NonNull AzRendererPipelineContext<UUID, T> pc, final @NonNull AzBone bone) {
        /* Left empty on purpose */
    }

    public static <T extends Entity> void setDefaultGlow(final @NonNull AzRendererPipelineContext<UUID, T> pc) {
        pc.setPackedLight(15728640);
        pc.setPackedOverlay(OverlayTexture.NO_OVERLAY);
        net.arna.jcraft.client.util.JAzRenderAccess.setRenderColorRgb(pc, 1f, 1f, 1f);
        JAzRenderAccess.setAlpha(pc, 1f);
    }
}
