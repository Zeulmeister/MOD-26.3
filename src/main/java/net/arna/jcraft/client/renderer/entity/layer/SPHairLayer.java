package net.arna.jcraft.client.renderer.entity.layer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.arna.jcraft.client.util.JAzRenderAccess;

import lombok.NonNull;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.renderer.entity.stands.StandEntityRenderer;
import net.arna.jcraft.common.entity.stand.AbstractStarPlatinumEntity;
import net.arna.jcraft.common.entity.stand.SPTWEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class SPHairLayer extends AbstractRenderLayer<AbstractStarPlatinumEntity<?, ?>> {
    private static final List<Identifier> SP_SKINS = IntStream.range(0, 4).mapToObj(
            i -> Identifier.fromNamespaceAndPath("jcraft", "textures/entity/stands/star_platinum/" + (i == 0 ? "default" : "skin" + i) + "_hair.png")).toList();
    private static final List<Identifier> SPTW_SKINS = IntStream.range(0, 4).mapToObj(
            i -> Identifier.fromNamespaceAndPath("jcraft", "textures/entity/stands/star_platinum_the_world/" + (i == 0 ? "default" : "skin" + i) + "_hair.png")).toList();

    public static Identifier getHairTexture(final boolean isSptw, final int skin) {
        final List<Identifier> skins = isSptw ? SPTW_SKINS : SP_SKINS;
        return skins.get(Mth.clamp(skin, 0, skins.size() - 1));
    }

    @Override
    public void render(final @NonNull AzRendererPipelineContext<UUID, AbstractStarPlatinumEntity<?, ?>> pc) {
        final AbstractStarPlatinumEntity<?,?> sp = pc.animatable();
        if (StandEntityRenderer.standIsFirstPersonViewers(sp)) {
            return;
        }
        final RenderType cameo = RenderTypes.eyes(getHairTexture(sp instanceof SPTWEntity, sp.getSkin()));
        pc.setRenderType(cameo);
        pc.setVertexConsumer(pc.multiBufferSource().getBuffer(cameo));
        pc.setPackedLight(LightCoordsUtil.FULL_BRIGHT);
        net.arna.jcraft.client.util.JAzRenderAccess.setRenderColorRgb(pc, 1f, 1f, 1f);
        JAzRenderAccess.setAlpha(pc, 1f);
        pc.rendererPipeline().reRender(pc);
    }

}
