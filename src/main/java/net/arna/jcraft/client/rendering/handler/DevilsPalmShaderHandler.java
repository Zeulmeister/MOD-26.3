package net.arna.jcraft.client.rendering.handler;

import net.minecraft.client.Minecraft;

public class DevilsPalmShaderHandler extends StandShaderHandler {
    public static final DevilsPalmShaderHandler INSTANCE = new DevilsPalmShaderHandler();

    @Override
    public void tick(Minecraft client) {
        // Handled natively by 26.3 Devil's Palm biome visual attributes
    }
}
