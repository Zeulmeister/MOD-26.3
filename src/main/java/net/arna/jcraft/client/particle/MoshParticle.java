package net.arna.jcraft.client.particle;

import net.arna.jcraft.common.particle.MoshParticleOptions;
import net.arna.jcraft.common.util.JUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class MoshParticle extends SingleQuadParticle {
    private static final int VARIANTS = 5, FRAMES = 4, TOTAL_SPRITES = VARIANTS * FRAMES;
    protected final SpriteSet spriteProvider;
    private final Entity parent;
    private final int variant;

    MoshParticle(final ClientLevel level, final double x, final double y, final double z,
                 final SpriteSet spriteProvider, final MoshParticleOptions options) {
        super(level, x, y, z, 0, 0, 0, spriteProvider.first());
        this.spriteProvider = spriteProvider;
        this.parent = level.getEntity(options.entityId());
        variant = random.nextInt(VARIANTS);
        this.alpha = 1.00f;
        this.quadSize = 0.25f + random.nextFloat() * 0.25f;
        this.lifetime = 10 + random.nextInt(8);

        final Vector3f color = options.color();
        final float brightness = 0.65f + random.nextFloat() * 0.35f;
        final float maxComp = Math.max(color.x(), Math.max(color.y(), color.z()));
        if (maxComp > 0.01f) {
            final float scale = brightness / maxComp;
            this.setColor(color.x() * scale, color.y() * scale, color.z() * scale);
        } else {
            this.setColor(brightness, brightness, brightness);
        }

        this.xd = 0;
        this.yd = 0.005;
        this.zd = 0;
        this.gravity = 0;
        updateSprite();
    }

    private void tryMatchParent() {
        if (parent != null) {
            Vec3 deltaPos = JUtils.deltaPos(parent);
            setParticleSpeed(deltaPos.x, deltaPos.y + 0.005, deltaPos.z);
        }
    }

    @Override
    public void tick() {
        if (parent == null || parent.isRemoved()) {
            remove();
            return;
        }

        updateSprite();
        tryMatchParent();
        super.tick();
    }

    private void updateSprite() {
        if (age == lifetime) return;
        int offset = variant * FRAMES;
        int frame = age * FRAMES / lifetime;
        setSprite(spriteProvider.get(frame + offset, TOTAL_SPRITES - 1));
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<MoshParticleOptions> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(final @NotNull MoshParticleOptions options, final @NotNull ClientLevel world,
                                       final double x, final double y, final double z,
                                       final double vx, final double vy, final double vz, final @NotNull RandomSource random) {
            var out = new MoshParticle(world, x, y, z, this.spriteProvider, options);
            out.tryMatchParent();
            return out;
        }
    }
}
