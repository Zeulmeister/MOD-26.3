package net.arna.jcraft.client.renderer.entity.layer;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer;
import net.arna.jcraft.client.util.JAzRenderAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Subtle surgical emissive pass for iconic Stands' eyes, cores, and metallic trim ornaments.
 */
@Environment(EnvType.CLIENT)
public class IconicStandGlowLayer<T extends StandEntity<?, ?>> extends AbstractRenderLayer<T> {
    private final Identifier glowTexture;
    private final float baseAlpha;
    private final boolean tintByAura;

    public IconicStandGlowLayer(final Identifier glowTexture, final float baseAlpha, final boolean tintByAura) {
        this.glowTexture = glowTexture;
        this.baseAlpha = baseAlpha;
        this.tintByAura = tintByAura;
    }

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, T> pc) {
        final T stand = pc.animatable();
        final float standAlpha = StandEntityRenderer.getAlpha(stand, pc.partialTick());
        if (standAlpha <= 0.02f) {
            return;
        }

        final RenderType cameo = RenderTypes.entityTranslucentEmissive(glowTexture);
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.setPackedLight(15728640);
        pc.setPackedOverlay(OverlayTexture.NO_OVERLAY);

        final float pulse = 0.88f + 0.12f * Mth.sin((stand.tickCount + pc.partialTick()) * 0.16f);
        final float alpha = Mth.clamp(baseAlpha * pulse * standAlpha, 0.0f, 1.0f);

        if (tintByAura) {
            final Vector3f aura = stand.getAuraColor();
            final float r = Mth.clamp(0.75f + 0.25f * aura.x(), 0.0f, 1.0f);
            final float g = Mth.clamp(0.75f + 0.25f * aura.y(), 0.0f, 1.0f);
            final float b = Mth.clamp(0.75f + 0.25f * aura.z(), 0.0f, 1.0f);
            JAzRenderAccess.setRenderColorRgb(pc, r, g, b);
        } else {
            JAzRenderAccess.setRenderColorRgb(pc, 1.0f, 1.0f, 1.0f);
        }
        JAzRenderAccess.setAlpha(pc, alpha);
        pc.rendererPipeline().reRender(pc);
    }
}
