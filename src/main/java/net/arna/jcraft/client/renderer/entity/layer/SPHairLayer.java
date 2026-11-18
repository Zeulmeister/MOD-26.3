package net.arna.jcraft.client.renderer.entity.layer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.arna.jcraft.client.util.JAzRenderAccess;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer;
import net.arna.jcraft.common.entity.stand.AbstractStarPlatinumEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class SPHairLayer extends AbstractRenderLayer<AbstractStarPlatinumEntity<?, ?>> {
    private static final List<Identifier> SKINS = IntStream.range(0, 4).mapToObj(
            i -> JCraft.id("textures/entity/stands/star_platinum/" + (i == 0 ? "default" : "skin" + i) + "_hair.png")).toList();

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, AbstractStarPlatinumEntity<?, ?>> pc) {
        final AbstractStarPlatinumEntity<?,?> sp = pc.animatable();
        if (StandEntityRenderer.standIsFirstPersonViewers(sp)) {
            return;
        }
        final RenderType cameo = RenderTypes.eyes(SKINS.get(sp.getSkin()));
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.setPackedLight(OverlayTexture.NO_OVERLAY);
        net.arna.jcraft.client.util.JAzRenderAccess.setRenderColorRgb(pc, 1f, 1f, 1f);
        JAzRenderAccess.setAlpha(pc, 1f);
        pc.rendererPipeline().reRender(pc);
    }

}
