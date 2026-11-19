package net.arna.jcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.common.util.IJExplosion;
import net.arna.jcraft.common.util.JExplosionModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin implements IJExplosion {

    @Unique
    @Nullable
    private JExplosionModifier jcraft$modifier;

    @Override
    public void jcraft$setModifier(JExplosionModifier modifier) {
        this.jcraft$modifier = modifier;
    }

    @WrapOperation(
            method = "hurtEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;shouldDamageEntity(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean jcraft$applyHurtFilter(ExplosionDamageCalculator instance, Explosion explosion, Entity entity, Operation<Boolean> original) {
        if (jcraft$modifier != null && jcraft$modifier.getHurtFilter() != null && !jcraft$modifier.getHurtFilter().test(entity)) {
            return false;
        }
        return original.call(instance, explosion, entity);
    }
}
