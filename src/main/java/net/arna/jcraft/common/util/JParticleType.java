package net.arna.jcraft.common.util;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.function.Supplier;

public enum JParticleType {
    BOOM(JParticleTypeRegistry.BOOM_1),
    BITES_THE_DUST(JParticleTypeRegistry.BITES_THE_DUST),
    SWEEP_ATTACK(ParticleTypes.SWEEP_ATTACK),
    BACK_STAB(JParticleTypeRegistry.BACKSTAB),
    FLASH(() -> ColorParticleOption.create(ParticleTypes.FLASH, -1)),
    COMBO_BREAK(JParticleTypeRegistry.COMBO_BREAK),
    COOLDOWN_CANCEL(JParticleTypeRegistry.COOLDOWN_CANCEL),
    HIT_SPARK_1(JParticleTypeRegistry.HITSPARK_1),
    HIT_SPARK_2(JParticleTypeRegistry.HITSPARK_2),
    HIT_SPARK_3(JParticleTypeRegistry.HITSPARK_3),
    BLOOD_SPARK_2(JParticleTypeRegistry.BLOOD_HITSPARK_2),
    INVERTED_HIT_SPARK_3(JParticleTypeRegistry.INVERTED_HITSPARK_3),
    PIXEL(JParticleTypeRegistry.PIXEL),
    BLOCK_SPARK(JParticleTypeRegistry.BLOCKSPARK),
    GO(JParticleTypeRegistry.GO),
    AURA_ARC(JParticleTypeRegistry.AURA_ARC),
    AURA_BLOB(JParticleTypeRegistry.AURA_BLOB),
    STUN_SLASH(JParticleTypeRegistry.STUN_SLASH),
    STUN_PIERCE(JParticleTypeRegistry.STUN_PIERCE),
    LEMON(JParticleTypeRegistry.LEMON);

    public static final Codec<JParticleType> CODEC = JCodecUtils.createEnumCodec(JParticleType.class);
    private final Supplier<? extends ParticleOptions> particleType;

    JParticleType(ParticleOptions particleOptions) {
        this.particleType = () -> particleOptions;
    }

    JParticleType(Supplier<? extends ParticleOptions> supplier) {
        this.particleType = supplier;
    }

    public ParticleOptions getParticleType() {
        return particleType.get();
    }
}
