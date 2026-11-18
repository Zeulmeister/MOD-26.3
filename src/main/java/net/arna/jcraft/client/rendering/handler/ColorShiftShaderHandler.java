package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.JCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Araki dramatic anime palette shift shader handler (triggered on Pose Taunt, Time Stop climax, or low HP).
 */
public class ColorShiftShaderHandler extends StandShaderHandler {
    public static final ColorShiftShaderHandler INSTANCE = new ColorShiftShaderHandler();
    public static final Identifier POST_EFFECT_ID = JCraft.id("color_shift");

    private int activeTicks = 0;
    private float intensity = 0.55f;

    public void trigger(int durationTicks, float shiftIntensity) {
        this.activeTicks = Math.max(this.activeTicks, durationTicks);
        this.intensity = shiftIntensity;
        this.shouldRender = true;
        this.renderingEffect = true;
    }

    public boolean isActive() {
        return activeTicks > 0 && renderingEffect;
    }

    public float getIntensity() {
        return intensity;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || !client.player.isAlive()) {
            activeTicks = 0;
            renderingEffect = false;
            shouldRender = false;
            return;
        }
        if (activeTicks > 0) {
            activeTicks--;
            renderingEffect = activeTicks > 0;
            shouldRender = renderingEffect;
        } else if (client.player.getHealth() <= client.player.getMaxHealth() * 0.25f) {
            // Dramatic low-HP JoJo anime color shift pulse
            renderingEffect = (client.player.tickCount / 20) % 4 == 0;
            shouldRender = renderingEffect;
        } else {
            renderingEffect = false;
            shouldRender = false;
        }
    }
}
