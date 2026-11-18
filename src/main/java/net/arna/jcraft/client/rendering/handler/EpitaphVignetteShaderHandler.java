package net.arna.jcraft.client.rendering.handler;

import net.minecraft.client.Minecraft;

public class EpitaphVignetteShaderHandler extends StandShaderHandler {
    public static final EpitaphVignetteShaderHandler INSTANCE = new EpitaphVignetteShaderHandler();
    public int duration = 0;

    @Override
    public void tick(final Minecraft client) {
        if (!shouldRender) {
            renderingEffect = false;
            return;
        }
        if (!renderingEffect) {
            ticks = 0;
            renderingEffect = true;
        }
        ticks++;
        if (ticks > duration) {
            renderingEffect = false;
            shouldRender = false;
        }
    }
}
