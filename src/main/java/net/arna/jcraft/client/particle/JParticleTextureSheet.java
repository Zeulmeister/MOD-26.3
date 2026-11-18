package net.arna.jcraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.SingleQuadParticle;

@Environment(EnvType.CLIENT)
public class JParticleTextureSheet {
    public static final SingleQuadParticle.Layer PARTICLE_SHEET_AURA = SingleQuadParticle.Layer.TRANSLUCENT;
    public static final SingleQuadParticle.Layer INVERSION_SHEET = SingleQuadParticle.Layer.TRANSLUCENT;
}
