package net.arna.jcraft.mixin;

import net.arna.jcraft.mixin_logic.EntityAddon;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void jcraft$keepAiWhenStandRidesMob(CallbackInfoReturnable<LivingEntity> cir) {
        Mob self = (Mob) (Object) this;
        if (JComponentPlatformUtils.getStandComponent(self).getStand() != null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void jcraft$markSpawnerOrigin(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (spawnReason == EntitySpawnReason.SPAWNER || spawnReason == EntitySpawnReason.TRIAL_SPAWNER) {
            ((EntityAddon) this).jcraft$setFromSpawner();
        }
    }
}
