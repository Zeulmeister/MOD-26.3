package net.arna.jcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @WrapOperation(
            method = "aiStep",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z", opcode = 181),
            require = 0
    )
    private void jcraft$disableSpaceDoubleTapWhenCreaming(Abilities abilities, boolean value, Operation<Void> original) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!CreamEntity.isCreaming(self)) {
            original.call(abilities, value);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void jcraft$isolatePlayerMovementDuringPilotMode(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (StandRemoteControlHandler.isControlling(mc)) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            self.xxa = 0.0f;
            self.zza = 0.0f;
            self.setJumping(false);
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void jcraft$dampPlayerMomentumDuringPilotMode(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (StandRemoteControlHandler.isControlling(mc)) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            self.xxa = 0.0f;
            self.zza = 0.0f;
            self.setJumping(false);
            var vel = self.getDeltaMovement();
            self.setDeltaMovement(0.0, vel.y, 0.0);
        }
    }

    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$moveTowardsClosestSpaceWithGravity(double x, double z, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Direction dir = GravityChangerAPI.getGravityDirection(self);
        if (dir == Direction.DOWN) {
            return;
        }
        double lx = x - self.getX();
        double lz = z - self.getZ();
        Vec3 sampleWorld = self.position().add(RotationUtil.vecPlayerToWorld(lx, 0.0, lz, dir));
        BlockPos bp = BlockPos.containing(sampleWorld);
        if (!jcraft$suffocatesAtWithGravity(self, bp, dir)) {
            ci.cancel();
            return;
        }
        Vec3 localFrac = RotationUtil.vecWorldToPlayer(
                sampleWorld.subtract(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5),
                dir
        );
        Direction[] horiz = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        Direction bestLocal = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction localDir : horiz) {
            BlockPos neighbor = bp.relative(RotationUtil.dirPlayerToWorld(localDir, dir));
            if (!jcraft$suffocatesAtWithGravity(self, neighbor, dir)) {
                double dist = switch (localDir) {
                    case WEST -> 0.5 + localFrac.x;
                    case EAST -> 0.5 - localFrac.x;
                    case NORTH -> 0.5 + localFrac.z;
                    case SOUTH -> 0.5 - localFrac.z;
                    default -> Double.MAX_VALUE;
                };
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLocal = localDir;
                }
            }
        }
        if (bestLocal != null) {
            Vec3 vel = self.getDeltaMovement();
            switch (bestLocal) {
                case WEST -> self.setDeltaMovement(-0.1, vel.y, vel.z);
                case EAST -> self.setDeltaMovement(0.1, vel.y, vel.z);
                case NORTH -> self.setDeltaMovement(vel.x, vel.y, -0.1);
                case SOUTH -> self.setDeltaMovement(vel.x, vel.y, 0.1);
                default -> {}
            }
        }
        ci.cancel();
    }

    @Unique
    private static boolean jcraft$suffocatesAtWithGravity(LocalPlayer self, BlockPos bp, Direction dir) {
        AABB bb = self.getBoundingBox();
        AABB column = switch (dir.getAxis()) {
            case Y -> new AABB(bp.getX(), bb.minY, bp.getZ(), bp.getX() + 1.0, bb.maxY, bp.getZ() + 1.0);
            case Z -> new AABB(bp.getX(), bp.getY(), bb.minZ, bp.getX() + 1.0, bp.getY() + 1.0, bb.maxZ);
            case X -> new AABB(bb.minX, bp.getY(), bp.getZ(), bb.maxX, bp.getY() + 1.0, bp.getZ() + 1.0);
        };
        return self.level().collidesWithSuffocatingBlock(self, column.deflate(1.0E-7));
    }
}
