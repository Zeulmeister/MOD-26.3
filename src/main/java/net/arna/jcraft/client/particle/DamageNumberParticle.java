package net.arna.jcraft.client.particle;

import net.arna.jcraft.client.JClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

public class DamageNumberParticle extends Particle {
    private static final List<Float> POSITIONS = List.of(0f, -0.25f, 0.12f, -0.12f, 0.25f);
    private static final DecimalFormat DF = new DecimalFormat("0.0");

    private final Component text;
    private final int color;

    public DamageNumberParticle(ClientLevel clientLevel, double x, double y, double z,
                                double damageAmount, double ySpeed, double animationPos) {
        super(clientLevel, x, y, z);
        this.lifetime = 35;
        this.color = JClientConfig.getInstance().getDummyDamageIndicatorColor();
        this.text = Component.literal(DF.format(Math.abs(damageAmount)));
        this.yd = 0.8;
        int posIndex = Math.floorMod((int) animationPos, POSITIONS.size());
        this.xd = POSITIONS.get(posIndex);
    }

    @Override
    public @NotNull ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }

    public Component getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        public Factory(SpriteSet spriteSet) {}

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, @NotNull RandomSource random) {
            return new DamageNumberParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
