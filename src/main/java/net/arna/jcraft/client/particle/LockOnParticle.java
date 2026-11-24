package net.arna.jcraft.client.particle;

import net.arna.jcraft.common.particle.LockOnParticleOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LockOnParticle extends SingleQuadParticle {
    private final SpriteSet spriteProvider;
    private final Entity parent;

    LockOnParticle(final ClientLevel level, final double x, final double y, final double z,
                   final SpriteSet spriteProvider, final LockOnParticleOptions options) {
        super(level, x, y, z, 0, 0, 0, spriteProvider.first());
        this.spriteProvider = spriteProvider;
        this.parent = level.getEntity(options.entityId());
        this.alpha = 1f;
        this.quadSize = 1f;
        this.lifetime = 9;
        this.gravity = 0;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        followParent();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.setSpriteFromAge(spriteProvider);
    }

    private void followParent() {
        if (parent == null) return;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.setPos(parent.getX(), parent.getY(), parent.getZ());
    }

    @Override
    public void tick() {
        if (parent == null || parent.isRemoved()) {
            remove();
            return;
        }

        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }

        followParent();
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    protected int getLightCoords(float tint) {
        return 255;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<LockOnParticleOptions> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Nullable
        @Override
        public Particle createParticle(final @NotNull LockOnParticleOptions options, final @NotNull ClientLevel world,
                                       final double x, final double y, final double z,
                                       final double vx, final double vy, final double vz, final @NotNull RandomSource random) {
            return new LockOnParticle(world, x, y, z, this.spriteProvider, options);
        }
    }
}
