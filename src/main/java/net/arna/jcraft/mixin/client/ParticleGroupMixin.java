package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.util.JClientUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void jcraft$freezeParticlesInTimeStop(Particle particle, CallbackInfo ci) {
        if (particle == null || JClientUtils.activeTimestops.isEmpty()) {
            return;
        }
        // Keep JCraft's own Time Stop aura/hitspark particles alive while freezing world particles
        if (particle.getClass().getName().startsWith("net.arna.jcraft.")) {
            return;
        }
        ParticleAccessor acc = (ParticleAccessor) particle;
        Vec3 pos = new Vec3(acc.jcraft$getX(), acc.jcraft$getY(), acc.jcraft$getZ());
        if (JClientUtils.isInTSRange(pos)) {
            acc.jcraft$setXo(pos.x);
            acc.jcraft$setYo(pos.y);
            acc.jcraft$setZo(pos.z);
            ci.cancel();
        }
    }
}
