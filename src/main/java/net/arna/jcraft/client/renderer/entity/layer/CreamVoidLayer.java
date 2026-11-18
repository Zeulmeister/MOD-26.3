package net.arna.jcraft.client.renderer.entity.layer;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class CreamVoidLayer extends AbstractRenderLayer<CreamEntity> {
    private static final List<Identifier> SKINS = IntStream.range(0, 4).mapToObj(
            i -> JCraft.id("textures/entity/stands/cream/void" + i + ".png")).toList();

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, CreamEntity> pc) {
        final CreamEntity cream = pc.animatable();
        if (cream.tickCount == 0 || !JServerConfig.CREAM_VOID_VISIBLE.getValue()) {
            return;
        }
        final RenderType cameo = net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SKINS.get(cream.getSkin()));
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        setDefaultGlow(pc);
        pc.rendererPipeline().reRender(pc);
    }

}
