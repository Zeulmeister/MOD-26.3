package net.arna.jcraft.client.rendering.skybox;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.rendering.handler.CrimsonShaderHandler;
import net.arna.jcraft.common.network.s2c.TimeAccelStatePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Coordinates custom RenderPearl 26.3 skyboxes rendered through {@code SkyRendererMixin}:
 * - King Crimson Time Erase cosmic crimson skybox ({@code textures/environment/te_sky.png} + parallax starfield)
 * - Made in Heaven Time Acceleration celestial star-trails and solar/lunar motion streaks
 */
@Environment(EnvType.CLIENT)
public class SkyBoxManager {
    public static final Identifier TIME_ERASE_SKY_TEXTURE = JCraft.id("textures/environment/te_sky.png");

    private static final SkyBoxManager INSTANCE = new SkyBoxManager();

    public static volatile int timeEraseSkyDraws = 0;
    public static volatile int timeAccelStarTrailDraws = 0;

    private volatile boolean enabled = true;
    private volatile boolean forceTimeErase = false;
    private volatile boolean forceTimeAccel = false;

    private SkyBoxManager() {}

    public static SkyBoxManager getInstance() {
        return INSTANCE;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setForceTimeErase(boolean force) {
        this.forceTimeErase = force;
    }

    public boolean isForceTimeErase() {
        return forceTimeErase;
    }

    public void setForceTimeAccel(boolean force) {
        this.forceTimeAccel = force;
    }

    public boolean isForceTimeAccel() {
        return forceTimeAccel;
    }

    public boolean isTimeEraseSkyActive() {
        if (!enabled) {
            return false;
        }
        return forceTimeErase
                || (CrimsonShaderHandler.INSTANCE.renderingEffect && JClientConfig.getInstance().isTimeEraseShader());
    }

    public boolean isTimeAccelSkyActive() {
        if (!enabled || isTimeEraseSkyActive()) {
            return false;
        }
        return forceTimeAccel || TimeAccelStatePacket.hasActiveAcceleration();
    }

    public float getTimeAccelIntensity() {
        if (forceTimeAccel) {
            return 1.0f;
        }
        return Mth.clamp(TimeAccelStatePacket.getMaxAccelerationIntensity(), 0.35f, 1.0f);
    }
}
