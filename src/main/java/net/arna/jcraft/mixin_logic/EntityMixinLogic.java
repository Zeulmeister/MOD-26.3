package net.arna.jcraft.mixin_logic;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class EntityMixinLogic {

    public static void jcraft$updatePassengerPosition(
            Entity vehicle,
            Entity passenger,
            Entity.MoveFunction callback,
            CallbackInfo ci
    ) {
        if (passenger instanceof StandEntity<?, ?> stand) {
            if (stand.isRemote()) {
                ci.cancel();
                return;
            }
            if (stand.isFree()) {
                Vector3f freePos = stand.getFreePos();
                callback.accept(passenger, freePos.x(), freePos.y(), freePos.z());
                ci.cancel();
                return;
            }

            double dist = stand.getDistanceOffset();
            float yRotRad = (vehicle.getYRot() + stand.getRotationOffset()) * Mth.DEG_TO_RAD;
            float xRotRad = stand.shouldOffsetHeight() ? vehicle.getXRot() * Mth.DEG_TO_RAD : 0.0f;
            double horizDist = dist * Mth.cos(xRotRad);
            double heightOffset = -dist * Mth.sin(xRotRad);

            Vec3 offset = RotationUtil.vecPlayerToWorld(
                    Mth.cos(yRotRad) * horizDist,
                    heightOffset + stand.getYDistanceOffset(),
                    Mth.sin(yRotRad) * horizDist,
                    GravityChangerAPI.getGravityDirection(vehicle)
            );
            callback.accept(
                    passenger,
                    vehicle.getX() + offset.x,
                    vehicle.getY() + offset.y,
                    vehicle.getZ() + offset.z
            );
            ci.cancel();
        }
    }

    public static void jcraft$doNotPlayDesummonSoundWhenMovingWorld(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        StandEntity<?, ?> stand = JUtils.getStand(living);
        if (stand == null) {
            return;
        }

        stand.setPlayDesummonSound(false);
    }
}
