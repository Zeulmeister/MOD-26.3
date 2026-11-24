package net.arna.jcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.effects.FlammableEffect;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.common.events.EntityTickEvent;
import net.arna.jcraft.common.events.JEntityEvents;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.EntityAddon;
import net.arna.jcraft.mixin_logic.EntityMixinLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAddon {

    @Shadow
    private int id;

    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    public boolean horizontalCollision;

    @Shadow
    public boolean verticalCollision;

    @Shadow
    public boolean verticalCollisionBelow;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract Vec3 getPosition(float partialTick);

    @Shadow
    public abstract float getEyeHeight();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 motion);

    @Unique
    private boolean jcraft$constructed = false;

    @Unique
    private boolean jcraft$fromSpawner = false;

    @Unique
    private Vec3 jcraft$preMoveWantedLocal = null;

    @Unique
    private Vec3 jcraft$preMoveDeltaLocal = null;

    @Unique
    private Vec3 jcraft$postMoveActualWorld = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jcraft$markConstructed(CallbackInfo ci) {
        this.jcraft$constructed = true;
    }

    // ===== C-Moon / Gravity Changer Directional Physics (zero overhead when Direction.DOWN) =====

    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void jcraft$rotateGravityBoundingBox(Vec3 pos, CallbackInfoReturnable<AABB> cir) {
        if (!this.jcraft$constructed || this.dimensions == null) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN) {
            AABB localBox = this.dimensions.makeBoundingBox(0.0, 0.0, 0.0);
            cir.setReturnValue(RotationUtil.boxPlayerToWorld(localBox, dir).move(pos));
        }
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void jcraft$rotateEyePosition(CallbackInfoReturnable<Vec3> cir) {
        if (!this.jcraft$constructed) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN) {
            cir.setReturnValue(this.position().add(RotationUtil.vecPlayerToWorld(0.0, this.getEyeHeight(), 0.0, dir)));
        }
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void jcraft$rotateEyePositionPartial(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!this.jcraft$constructed) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN) {
            cir.setReturnValue(this.getPosition(partialTick).add(RotationUtil.vecPlayerToWorld(0.0, this.getEyeHeight(), 0.0, dir)));
        }
    }

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void jcraft$rotateViewVector(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!this.jcraft$constructed) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN) {
            cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), dir));
        }
    }

    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void jcraft$rotateOnPos(float yOffset, CallbackInfoReturnable<BlockPos> cir) {
        if (!this.jcraft$constructed) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN) {
            Vec3 belowFeet = this.position().add(RotationUtil.vecPlayerToWorld(0.0, -yOffset, 0.0, dir));
            cir.setReturnValue(BlockPos.containing(belowFeet));
        }
    }

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3 jcraft$transformMoveToWorldGravity(Vec3 movement) {
        if (!this.jcraft$constructed) {
            return movement;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir == Direction.DOWN) {
            this.jcraft$preMoveWantedLocal = null;
            this.jcraft$preMoveDeltaLocal = null;
            this.jcraft$postMoveActualWorld = null;
            return movement;
        }
        this.jcraft$preMoveWantedLocal = movement;
        this.jcraft$preMoveDeltaLocal = this.getDeltaMovement();
        this.jcraft$postMoveActualWorld = null;
        return RotationUtil.vecPlayerToWorld(movement, dir);
    }

    @WrapOperation(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V")
    )
    private void jcraft$resolveDirectionalGroundCollision(
            Entity instance,
            boolean onGround,
            boolean horizontalCollision,
            Vec3 actualWorldMove,
            Operation<Void> original) {
        Direction dir = GravityChangerAPI.getGravityDirection(instance);
        if (dir == Direction.DOWN || this.jcraft$preMoveWantedLocal == null) {
            original.call(instance, onGround, horizontalCollision, actualWorldMove);
            return;
        }
        this.jcraft$postMoveActualWorld = actualWorldMove;
        Vec3 localActual = RotationUtil.vecWorldToPlayer(actualWorldMove, dir);
        Vec3 wanted = this.jcraft$preMoveWantedLocal;
        boolean localXHit = !Mth.equal(wanted.x, localActual.x);
        boolean localYHit = !Mth.equal(wanted.y, localActual.y);
        boolean localZHit = !Mth.equal(wanted.z, localActual.z);
        boolean localHoriz = localXHit || localZHit;
        boolean localOnGround = localYHit && wanted.y < 0.0;

        this.horizontalCollision = localHoriz;
        this.verticalCollision = localYHit;
        this.verticalCollisionBelow = localOnGround;
        original.call(instance, localOnGround, localHoriz, actualWorldMove);
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void jcraft$preserveLocalVelocityAxesOnCollision(MoverType type, Vec3 worldMovement, CallbackInfo ci) {
        if (this.jcraft$preMoveWantedLocal == null || this.jcraft$preMoveDeltaLocal == null) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (dir != Direction.DOWN && this.jcraft$postMoveActualWorld != null) {
            Vec3 localActual = RotationUtil.vecWorldToPlayer(this.jcraft$postMoveActualWorld, dir);
            Vec3 wanted = this.jcraft$preMoveWantedLocal;
            Vec3 pre = this.jcraft$preMoveDeltaLocal;
            double vx = Mth.equal(wanted.x, localActual.x) ? pre.x : 0.0;
            double vy = Mth.equal(wanted.y, localActual.y) ? pre.y : 0.0;
            double vz = Mth.equal(wanted.z, localActual.z) ? pre.z : 0.0;
            this.setDeltaMovement(new Vec3(vx, vy, vz));
        }
        this.jcraft$preMoveWantedLocal = null;
        this.jcraft$preMoveDeltaLocal = null;
        this.jcraft$postMoveActualWorld = null;
    }

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

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void jcraft$safeHashCodeBeforeIdAssigned(CallbackInfoReturnable<Integer> cir) {
        if (this.id == 0) {
            cir.setReturnValue(System.identityHashCode(this));
        }
    }

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void jcraft$safeEqualsBeforeIdAssigned(Object other, CallbackInfoReturnable<Boolean> cir) {
        if (this.id == 0 || (other instanceof Entity && ((EntityMixin) other).id == 0)) {
            cir.setReturnValue(this == other);
        }
    }

    @Redirect(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z")
    )
    private boolean jcraft$allowRidingPlayerVehicle(EntityType<?> entityType) {
        return entityType == EntityTypes.PLAYER || entityType.canSerialize();
    }

    @Inject(method = "addPassenger", at = @At("TAIL"))
    private void jcraft$syncAddedPassengerToSelf(Entity passenger, CallbackInfo ci) {
        if (passenger.tickCount > 0 && (Object) this instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundSetPassengersPacket(serverPlayer));
        }
    }

    @Inject(method = "removePassenger", at = @At("TAIL"))
    private void jcraft$syncRemovedPassengerToSelf(Entity passenger, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundSetPassengersPacket(serverPlayer));
        }
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
