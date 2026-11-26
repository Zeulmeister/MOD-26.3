package net.arna.jcraft.fabric.client;

import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.gui.hud.EpitaphOverlay;
import net.arna.jcraft.client.registry.JItemPropertiesRegistry;
import net.arna.jcraft.client.util.JPlayerAnimHelper;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class JCraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JPlayerAnimHelper.init();
        JCraftClient.init();
        net.arna.jcraft.client.registry.JEntityModelLayerRegistry.init(entry ->
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(entry.getKey(), entry.getValue()));
        net.arna.jcraft.client.registry.JEntityRendererRegister.registerEntityRenderers(
                net.arna.jcraft.client.registry.JEntityRendererRegister.RendererData::registerFabric);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                net.arna.jcraft.api.registry.JBlockEntityTypeRegistry.COFFIN_TILE.get(),
                context -> new net.arna.jcraft.client.renderer.block.CoffinTileRenderer());
        JCraftClient.registerParticleSpriteSets();
        JShaderRegistry.init();
        JItemPropertiesRegistry.registerItemProperties();
        JCraftClient.registerKeyBindings(null);
        Minecraft.getInstance().execute(EpitaphOverlay::preload);
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            final Minecraft client = Minecraft.getInstance();
            final net.minecraft.client.multiplayer.ClientLevel world = client.level;
            if (world == null) {
                return;
            }
            final var camState = context.levelState().cameraRenderState;
            final var camPos = camState.pos;
            final float tickDelta = camState.cameraEntityPartialTicks;
            final var matrices = context.poseStack();
            final var collector = context.submitNodeCollector();
            net.arna.jcraft.client.renderer.effects.ShockwaveEffectRenderer.render(matrices, camPos, world, tickDelta, collector);
            net.arna.jcraft.client.renderer.effects.SplatterEffectRenderer.render(matrices, camPos, world, tickDelta, collector);
            net.arna.jcraft.client.renderer.effects.TimeErasePredictionEffectRenderer.render(matrices, camPos, world, tickDelta, collector, camState);
            net.arna.jcraft.client.renderer.effects.TimeAccelerationEffectRenderer.render(world);
            net.arna.jcraft.client.renderer.effects.StandRunicCircleRenderer.render(matrices, camPos, world, tickDelta, collector);
            net.arna.jcraft.client.rendering.MihAfterimageTrail.renderAll(matrices, camPos, world, tickDelta, collector, camState);
            net.arna.jcraft.client.rendering.AuraAfterimageManager.renderAll(matrices, camPos, world, tickDelta, collector, camState);
        });
    }
}
