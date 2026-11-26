package net.arna.jcraft.client.renderer.entity.layer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.arna.jcraft.client.util.JAzRenderAccess;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.stand.TheWorldOverHeavenEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class TWOHEyesLayer extends AbstractRenderLayer<TheWorldOverHeavenEntity> {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(JCraft.MOD_ID, "textures/entity/stands/the_world_over_heaven/eyes.png");
    private static final Map<Integer, Vector3f> OVERWRITE_COLORS =
            Map.ofEntries(
                    Map.entry(0, new Vector3f(1f, 1f, 1f)), // Default, WHITE

                    Map.entry(1, new Vector3f(1f, 0.2f, 0.2f)),  // Unwatchable, RED
                    Map.entry(2, new Vector3f(0.6f, 0.2f, 1f)),  // DoT, PURPLE
                    Map.entry(3, new Vector3f(0.2f, 1f, 0.2f)),  // Heal, GREEN

                    Map.entry(4, new Vector3f(1f, 0.8f, 0)) // Heavy, YELLOW
            );

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, TheWorldOverHeavenEntity> pc) {
        final RenderType cameo = RenderTypes.eyes(LAYER);
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.setPackedLight(15728640);
        pc.setPackedOverlay(OverlayTexture.NO_OVERLAY);
        final Vector3f color = OVERWRITE_COLORS.get(pc.animatable().getOverwriteType());
        net.arna.jcraft.client.util.JAzRenderAccess.setRenderColorRgb(pc, color.x(), color.y(), color.z());
        JAzRenderAccess.setAlpha(pc, 1f);
        pc.rendererPipeline().reRender(pc);
    }

}
