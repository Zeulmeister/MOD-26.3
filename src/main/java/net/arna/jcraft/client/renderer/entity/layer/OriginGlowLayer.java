package net.arna.jcraft.client.renderer.entity.layer;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.util.JAzRenderAccess;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Emissive fullbright rune, crown, wing-edge, and eye glow layer for {@link OriginEntity},
 * dynamically tinted by the active skin aura palette.
 */
@Environment(EnvType.CLIENT)
public class OriginGlowLayer extends AbstractRenderLayer<OriginEntity> {
    private static final Identifier GLOW_LAYER = JCraft.id("textures/entity/stands/origin/glow.png");

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, OriginEntity> pc) {
        final RenderType cameo = RenderTypes.eyes(GLOW_LAYER);
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.setPackedLight(15728640);
        pc.setPackedOverlay(OverlayTexture.NO_OVERLAY);

        final OriginEntity stand = pc.animatable();
        final int skin = stand.getSkin();
        final Vector3f aura = stand.getAuraColor();
        final float r;
        final float g;
        final float b;
        final float alpha;
        if (stand.hasCustomGlowRgb()) {
            final int rgb = stand.getCustomGlowRgb();
            r = ((rgb >> 16) & 0xFF) / 255.0f;
            g = ((rgb >> 8) & 0xFF) / 255.0f;
            b = (rgb & 0xFF) / 255.0f;
            alpha = 0.96f;
        } else if (skin == 4) {
            // Absolute Abyss: crisp icy-silver filigree over pitch-black void armor
            r = 0.88f; g = 0.91f; b = 1.00f; alpha = 0.88f;
        } else if (skin == 5) {
            // Celestial Divinity: pure divine solar gold glow over radiant white marble
            r = 1.00f; g = 0.84f; b = 0.22f; alpha = 0.96f;
        } else {
            r = Math.min(1.0f, aura.x() * 0.96f + 0.08f);
            g = Math.min(1.0f, aura.y() * 0.96f + 0.06f);
            b = Math.min(1.0f, aura.z() * 0.96f + 0.06f);
            alpha = 0.95f;
        }
        JAzRenderAccess.setRenderColorRgb(pc, r, g, b);
        JAzRenderAccess.setAlpha(pc, alpha);
        pc.rendererPipeline().reRender(pc);
    }
}
