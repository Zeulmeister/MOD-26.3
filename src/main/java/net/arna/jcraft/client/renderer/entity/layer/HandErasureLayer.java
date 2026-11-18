package net.arna.jcraft.client.renderer.entity.layer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.stand.TheHandEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class HandErasureLayer extends AbstractRenderLayer<TheHandEntity> {
    private static final List<Identifier> SKINS = IntStream.range(0, 4).mapToObj(
            i -> JCraft.id("textures/entity/stands/the_hand/erase" + i + ".png")).toList();
    private static final List<Identifier> SKINS_OUTER = IntStream.range(0, 4).mapToObj(
            i -> JCraft.id("textures/entity/stands/the_hand/erase_outer" + i + ".png")).toList();

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, TheHandEntity> pc) {
        setDefaultGlow(pc);
        final int skin = pc.animatable().getSkin();
        RenderType cameo = net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SKINS.get(skin));
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.rendererPipeline().reRender(pc);
        cameo = RenderTypes.eyes(SKINS_OUTER.get(skin));
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.rendererPipeline().reRender(pc);
    }

}
