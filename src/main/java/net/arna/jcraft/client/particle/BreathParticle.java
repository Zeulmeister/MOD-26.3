package net.arna.jcraft.client.particle;

import net.arna.jcraft.common.particle.BreathParticleOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BreathParticle extends RisingParticle {

    public BreathParticle(ClientLevel clientLevel, double x, double y, double z, double vx, double vy, double vz,
                          final SpriteSet spriteSet, final BreathParticleOptions options) {
        super(clientLevel, x, y, z, vx, vy, vz, spriteSet.first());
        quadSize = options.scale();
        setSprite(spriteSet.get(this.random));
    }

    @Override
    protected int getLightCoords(float tint) {
        return 255;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<BreathParticleOptions> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(final @NotNull BreathParticleOptions options, final @NotNull ClientLevel world,
                                       final double x, final double y, final double z, final double velocityX,
                                       final double velocityY, final double velocityZ, final @NotNull RandomSource random) {
            return new BreathParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteSet, options);
        }
    }
}
