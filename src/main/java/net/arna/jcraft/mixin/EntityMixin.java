package net.arna.jcraft.mixin;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.effects.FlammableEffect;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.common.events.EntityTickEvent;
import net.arna.jcraft.common.events.JEntityEvents;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.EntityAddon;
import net.arna.jcraft.mixin_logic.EntityMixinLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAddon {

    @Unique
    private boolean jcraft$fromSpawner = false;

    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jcraft$updatePassengerPosition(Entity passenger, Entity.MoveFunction callback, CallbackInfo ci) {
        EntityMixinLogic.jcraft$updatePassengerPosition((Entity) (Object) this, passenger, callback, ci);
    }

    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void jcraft$preventExtinguish(int ticks, CallbackInfo ci) {
        if (ticks <= 0 && (Object) this instanceof LivingEntity living) {
            if (FlammableEffect.isFlammable(living) && living.isOnFire()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void jcraft$shouldSpawnSprintingParticles(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            StandEntity<?, ?> stand = JUtils.getStand(living);
            if (stand instanceof KingCrimsonEntity kc && kc.getTETime() > 0) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void jcraft$preTick(CallbackInfo ci) {
        EntityTickEvent.ENTITY_PRE.invoker().tick((Entity) (Object) this);
    }

    @Inject(method = "isInvulnerable", at = @At("HEAD"), cancellable = true)
    private void jcraft$invulnerableIfCreaming(CallbackInfoReturnable<Boolean> cir) {
        if (CreamEntity.isCreaming((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$ignorePushIfCreamingOrTimestopped(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (CreamEntity.isCreaming(self) || JUtils.isAffectedByTimeStop(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void jcraft$fireRemovedEvent(Entity.RemovalReason reason, CallbackInfo ci) {
        JEntityEvents.REMOVE.invoker().remove((Entity) (Object) this, reason);
    }

    @Inject(method = "teleport", at = @At("HEAD"))
    private void jcraft$silenceStandDesummonOnTeleport(TeleportTransition transition, CallbackInfoReturnable<Entity> cir) {
        EntityMixinLogic.jcraft$doNotPlayDesummonSoundWhenMovingWorld((Entity) (Object) this);
    }

    @Override
    public boolean jcraft$setFromSpawner() {
        return this.jcraft$fromSpawner = true;
    }

    @Override
    public boolean jcraft$isFromSpawner() {
        return this.jcraft$fromSpawner;
    }
}
