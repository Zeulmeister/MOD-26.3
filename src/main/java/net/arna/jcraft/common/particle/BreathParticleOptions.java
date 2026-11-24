package net.arna.jcraft.common.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record BreathParticleOptions(float scale) implements ParticleOptions {
    public static final MapCodec<BreathParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.FLOAT.fieldOf("scale").forGetter(BreathParticleOptions::scale))
                    .apply(instance, BreathParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BreathParticleOptions> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, BreathParticleOptions::scale, BreathParticleOptions::new);

    @Override
    public @NotNull ParticleType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type extends ParticleType<BreathParticleOptions> {
        public static final Type INSTANCE = new Type();

        private Type() {
            super(true);
        }

        @Override
        public @NotNull MapCodec<BreathParticleOptions> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, BreathParticleOptions> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
