package net.arna.jcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAddon {

    @Shadow
    private int id;

    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    @Final
    public RandomSource random;

    @Shadow
    public Optional<BlockPos> mainSupportingBlockPos;

    @Shadow
    private boolean onGroundNoBlocks;

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

    @Shadow
    public abstract float maxUpStep();

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract BlockPos getOnPosLegacy();

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract boolean touchingUnloadedChunk();

    @Shadow
    protected abstract void checkSupportingBlock(boolean onGround, @Nullable Vec3 movement);

    @Shadow
    protected abstract void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos);

    @Shadow
    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 vec, AABB collisionBox, Level level,
                                          List<VoxelShape> potentialHits) {
        throw new AssertionError();
    }

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
    private Vec3 jcraft$transformMoveToWorldGravity(Vec3 movement, @Local(argsOnly = true) MoverType type) {
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
        this.jcraft$preMoveDeltaLocal = this.getDeltaMovement();
        this.jcraft$postMoveActualWorld = null;
        if (type == MoverType.SELF) {
            this.jcraft$preMoveWantedLocal = movement;
            return RotationUtil.vecPlayerToWorld(movement, dir);
        }
        this.jcraft$preMoveWantedLocal = RotationUtil.vecWorldToPlayer(movement, dir);
        return movement;
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
        if (type == MoverType.SELF && dir != Direction.DOWN && this.jcraft$postMoveActualWorld != null) {
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

    @WrapOperation(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V")
    )
    private void jcraft$fixMoveFallDamageForGravity(
            Entity instance,
            double y,
            boolean onGround,
            BlockState state,
            BlockPos pos,
            Operation<Void> original) {
        double fallY = y;
        if (this.jcraft$constructed && this.jcraft$postMoveActualWorld != null) {
            Direction dir = GravityChangerAPI.getGravityDirection(instance);
            if (dir != Direction.DOWN) {
                fallY = RotationUtil.vecWorldToPlayer(this.jcraft$postMoveActualWorld, dir).y;
            }
        }
        original.call(instance, fallY, onGround, state, pos);
    }

    @Inject(method = "doCheckFallDamage(DDDZ)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$fixDoCheckFallDamageForGravity(double dx, double dy, double dz, boolean onGround, CallbackInfo ci) {
        if (!this.jcraft$constructed) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Direction dir = GravityChangerAPI.getGravityDirection(self);
        if (dir == Direction.DOWN) {
            return;
        }
        if (!this.touchingUnloadedChunk()) {
            Vec3 worldDelta = new Vec3(dx, dy, dz);
            this.checkSupportingBlock(onGround, worldDelta);
            BlockPos onPos = this.getOnPosLegacy();
            BlockState blockState = this.level().getBlockState(onPos);
            double localY = RotationUtil.vecWorldToPlayer(worldDelta, dir).y;
            this.checkFallDamage(localY, onGround, blockState, onPos);
        }
        ci.cancel();
    }

    @Inject(method = "checkSupportingBlock(ZLnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$checkSupportingBlockForGravity(boolean onGround, @Nullable Vec3 movement, CallbackInfo ci) {
        if (!this.jcraft$constructed) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Direction dir = GravityChangerAPI.getGravityDirection(self);
        if (dir == Direction.DOWN) {
            return;
        }
        if (onGround) {
            AABB bb = self.getBoundingBox();
            AABB feetBox = switch (dir) {
                case UP -> new AABB(bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY + 1.0E-6, bb.maxZ);
                case NORTH -> new AABB(bb.minX, bb.minY, bb.minZ - 1.0E-6, bb.maxX, bb.maxY, bb.minZ);
                case SOUTH -> new AABB(bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ + 1.0E-6);
                case WEST -> new AABB(bb.minX - 1.0E-6, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.maxZ);
                case EAST -> new AABB(bb.maxX, bb.minY, bb.minZ, bb.maxX + 1.0E-6, bb.maxY, bb.maxZ);
                case DOWN -> new AABB(bb.minX, bb.minY - 1.0E-6, bb.minZ, bb.maxX, bb.minY, bb.maxZ);
            };
            Optional<BlockPos> opt = this.level().findSupportingBlock(self, feetBox);
            if (opt.isPresent() || this.onGroundNoBlocks) {
                this.mainSupportingBlockPos = opt;
            } else if (movement != null) {
                Vec3 localM = RotationUtil.vecWorldToPlayer(movement, dir);
                Vec3 tangBack = RotationUtil.vecPlayerToWorld(-localM.x, 0.0, -localM.z, dir);
                opt = this.level().findSupportingBlock(self, feetBox.move(tangBack));
                this.mainSupportingBlockPos = opt;
            } else {
                this.mainSupportingBlockPos = Optional.empty();
            }
            this.onGroundNoBlocks = opt.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
            if (this.mainSupportingBlockPos.isPresent()) {
                this.mainSupportingBlockPos = Optional.empty();
            }
        }
        ci.cancel();
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void jcraft$collideWithDirectionalStepUp(Vec3 worldMove, CallbackInfoReturnable<Vec3> cir) {
        if (!this.jcraft$constructed) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Direction dir = GravityChangerAPI.getGravityDirection(self);
        if (dir == Direction.DOWN) {
            return;
        }
        AABB bb = self.getBoundingBox();
        Vec3 localUp = RotationUtil.vecPlayerToWorld(0.0, this.maxUpStep(), 0.0, dir);
        List<VoxelShape> entityCols = this.level().getEntityCollisions(self, bb.expandTowards(worldMove).expandTowards(localUp));
        Vec3 collided = worldMove.lengthSqr() == 0.0
                ? worldMove
                : collideBoundingBox(self, worldMove, bb, this.level(), entityCols);

        Vec3 localWanted = RotationUtil.vecWorldToPlayer(worldMove, dir);
        Vec3 localCollided = RotationUtil.vecWorldToPlayer(collided, dir);
        boolean hitX = !Mth.equal(localWanted.x, localCollided.x);
        boolean hitY = !Mth.equal(localWanted.y, localCollided.y);
        boolean hitZ = !Mth.equal(localWanted.z, localCollided.z);
        boolean hitDown = hitY && localWanted.y < 0.0;

        if (this.maxUpStep() > 0.0F && (hitDown || this.onGround()) && (hitX || hitZ)) {
            Vec3 stepUp = collideBoundingBox(self, localUp, bb, this.level(), entityCols);
            Vec3 horizWorld = RotationUtil.vecPlayerToWorld(localWanted.x, 0.0, localWanted.z, dir);
            Vec3 steppedHoriz = collideBoundingBox(self, horizWorld, bb.move(stepUp), this.level(), entityCols);
            Vec3 stepDown = collideBoundingBox(self, stepUp.scale(-1.0), bb.move(stepUp).move(steppedHoriz), this.level(), entityCols);
            Vec3 totalStep = stepUp.add(steppedHoriz).add(stepDown);
            Vec3 localTotal = RotationUtil.vecWorldToPlayer(totalStep, dir);
            double steppedDistSq = localTotal.x * localTotal.x + localTotal.z * localTotal.z;
            double flatDistSq = localCollided.x * localCollided.x + localCollided.z * localCollided.z;
            if (steppedDistSq > flatDistSq) {
                cir.setReturnValue(totalStep);
                return;
            }
        }
        cir.setReturnValue(collided);
    }

    @Inject(method = "spawnSprintParticle()V", at = @At("HEAD"), cancellable = true)
    private void jcraft$spawnSprintParticleWithGravity(CallbackInfo ci) {
        if (!this.jcraft$constructed || this.dimensions == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Direction dir = GravityChangerAPI.getGravityDirection(self);
        if (dir == Direction.DOWN) {
            return;
        }
        BlockPos onPos = this.getOnPosLegacy();
        BlockState blockState = this.level().getBlockState(onPos);
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 localVel = this.getDeltaMovement();
            Vec3 worldOffset = RotationUtil.vecPlayerToWorld(
                    (this.random.nextDouble() - 0.5) * this.dimensions.width(),
                    0.1,
                    (this.random.nextDouble() - 0.5) * this.dimensions.width(),
                    dir
            );
            Vec3 worldParticleVel = RotationUtil.vecPlayerToWorld(
                    -localVel.x * 4.0,
                    1.5,
                    -localVel.z * 4.0,
                    dir
            );
            this.level().addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    self.getX() + worldOffset.x,
                    self.getY() + worldOffset.y,
                    self.getZ() + worldOffset.z,
                    worldParticleVel.x,
                    worldParticleVel.y,
                    worldParticleVel.z
            );
        }
        ci.cancel();
    }

    @WrapOperation(
            method = "isInWall()Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;")
    )
    private AABB jcraft$rotateIsInWallBox(Vec3 center, double xSize, double ySize, double zSize, Operation<AABB> original) {
        if (this.jcraft$constructed) {
            Direction dir = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
            if (dir != Direction.DOWN) {
                Vec3 rotated = RotationUtil.maskPlayerToWorld(xSize, ySize, zSize, dir);
                return original.call(center, rotated.x, rotated.y, rotated.z);
            }
        }
        return original.call(center, xSize, ySize, zSize);
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
