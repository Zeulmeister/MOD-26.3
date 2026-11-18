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
        JShaderRegistry.init();
        JItemPropertiesRegistry.registerItemProperties();
        JCraftClient.registerKeyBindings(null);
        Minecraft.getInstance().execute(EpitaphOverlay::preload);
    }
}
