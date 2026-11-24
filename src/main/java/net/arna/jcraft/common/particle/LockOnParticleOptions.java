package net.arna.jcraft.common.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

public record LockOnParticleOptions(int entityId) implements ParticleOptions {
    public static final MapCodec<LockOnParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("entity_id").forGetter(LockOnParticleOptions::entityId)
    ).apply(instance, LockOnParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LockOnParticleOptions> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, LockOnParticleOptions::entityId, LockOnParticleOptions::new);

    @Override
    public @NotNull ParticleType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type extends ParticleType<LockOnParticleOptions> {
        public static final Type INSTANCE = new Type();

        private Type() {
            super(true);
        }

        @Override
        public @NotNull MapCodec<LockOnParticleOptions> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, LockOnParticleOptions> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
