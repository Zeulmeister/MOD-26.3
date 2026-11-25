package net.arna.jcraft.common.gravity.config;

/**
 * Runtime mirror of directional-gravity settings.
 * All fields are unified into and automatically synchronized from
 * {@link net.arna.jcraft.common.config.JServerConfig#syncGravityConfig()}.
 */
public class GravityChangerConfig {
    public boolean keepWorldLook = false;
    public int rotationTime = 500;
    public boolean worldVelocity = false;
    public boolean resetGravityOnDimensionChange = true;
    public boolean resetGravityOnRespawn = true;
    public boolean voidDamageAboveWorld = false;
}
