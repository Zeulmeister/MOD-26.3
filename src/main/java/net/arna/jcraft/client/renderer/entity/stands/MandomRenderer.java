package net.arna.jcraft.client.renderer.entity.stands;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.common.entity.stand.MandomEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class MandomRenderer extends StandEntityRenderer<MandomEntity> {

    public MandomRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, b -> b.setRenderType(renderType(RenderTypes::entityTranslucentCull)), JStandTypeRegistry.MANDOM.get(), 0f, 0f);
    }

}