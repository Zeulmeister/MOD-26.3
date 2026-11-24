package net.arna.jcraft.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double jcraft$getX();

    @Accessor("y")
    double jcraft$getY();

    @Accessor("z")
    double jcraft$getZ();

    @Accessor("xo")
    void jcraft$setXo(double xo);

    @Accessor("yo")
    void jcraft$setYo(double yo);

    @Accessor("zo")
    void jcraft$setZo(double zo);
}
