package net.arna.jcraft.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BreathParticle extends RisingParticle {

    public BreathParticle(ClientLevel clientLevel, double x, double y, double z, double vx, double vy, double vz,
                          final SpriteSet spriteSet, final Options options) {
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

    public static class Factory implements ParticleProvider<Options> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(final @NotNull BreathParticle.Options options, final @NotNull ClientLevel world,
                                       final double x, final double y, final double z, final double velocityX,
                                       final double velocityY, final double velocityZ, final @NotNull RandomSource random) {
            return new BreathParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteSet, options);
        }
    }

    public static class Type extends ParticleType<Options> {
        public static final Type INSTANCE = new Type();

        private Type() {
            super(true);
        }

        @Override
        public @NotNull MapCodec<Options> codec() {
            return Options.CODEC;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, Options> streamCodec() {
            return Options.STREAM_CODEC;
        }
    }

    public record Options(float scale) implements ParticleOptions {
        public static final MapCodec<Options> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(Codec.FLOAT.fieldOf("scale").forGetter(Options::scale))
                        .apply(instance, Options::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Options> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, Options::scale, Options::new);

        @Override
        public @NotNull ParticleType<?> getType() {
            return Type.INSTANCE;
        }
    }
}
