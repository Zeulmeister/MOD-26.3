package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.common.spec.RangerSpec;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class RangerFocusShaderHandler extends StandShaderHandler {
    public static final RangerFocusShaderHandler INSTANCE = new RangerFocusShaderHandler();
    private float fade = 0f;

    @Override
    public void tick(Minecraft client) {
        final boolean focusing = client.player != null &&
                JUtils.getSpec(client.player) instanceof RangerSpec &&
                JComponentPlatformUtils.getGunslinger(client.player).isFocusActive();
        fade = Mth.clamp(fade + (focusing ? 0.1f : -0.1f), 0f, 1f);
        renderingEffect = fade > 0.05f;
        shouldRender = renderingEffect;
    }
}
