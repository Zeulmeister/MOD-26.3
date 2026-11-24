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
import org.joml.Vector3f;

public record MoshParticleOptions(int entityId, Vector3f color) implements ParticleOptions {
    public static final MapCodec<MoshParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("entity_id").forGetter(MoshParticleOptions::entityId),
            ExtraCodecs.VECTOR3F.xmap(Vector3f::new, v -> v).fieldOf("color").forGetter(MoshParticleOptions::color)
    ).apply(instance, MoshParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoshParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, MoshParticleOptions::entityId,
                    ByteBufCodecs.VECTOR3F.map(Vector3f::new, v -> v), MoshParticleOptions::color,
                    MoshParticleOptions::new
            );

    @Override
    public @NotNull ParticleType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type extends ParticleType<MoshParticleOptions> {
        public static final Type INSTANCE = new Type();

        private Type() {
            super(true);
        }

        @Override
        public @NotNull MapCodec<MoshParticleOptions> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, MoshParticleOptions> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
