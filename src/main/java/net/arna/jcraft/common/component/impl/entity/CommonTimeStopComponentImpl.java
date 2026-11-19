package net.arna.jcraft.common.component.impl.entity;

import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.component.entity.CommonGrabComponent;
import net.arna.jcraft.api.component.entity.CommonTimeStopComponent;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static net.arna.jcraft.common.util.JUtils.addVelocity;
import static net.arna.jcraft.common.util.JUtils.stopTick;

@Getter
public abstract class CommonTimeStopComponentImpl implements CommonTimeStopComponent {
    public static final double MAX_ACCUMULATED_LAUNCH_SPEED = 3.5;

    private final Entity entity;
    private int ticks;
    // Launch buildup implementation, because players are special snowflakes and don't build it in timestop like enemies do
    private Vec3 totalVelocity = Vec3.ZERO;

    protected CommonTimeStopComponentImpl(final Entity entity) {
        this.entity = entity;
    }

    @Override
    public void setTicks(final int ticks) {
        if (this.ticks <= 0 && ticks > 0) // If just beginning a new Timestop
        {
            totalVelocity = Vec3.ZERO;
        }
        this.ticks = Math.max(0, ticks);
        if (!entity.level().isClientSide()) {
            sync(entity);
        }
    }

    @Override
    public void setClientTicks(final int ticks) {
        if (this.ticks <= 0 && ticks > 0) {
            totalVelocity = Vec3.ZERO;
        }
        this.ticks = Math.max(0, ticks);
    }

    @Override
    public void releaseNow() {
        final boolean wasFrozen = this.ticks > 0;
        this.ticks = 0;
        if (wasFrozen && !entity.level().isClientSide()) {
            applyAccumulatedVelocity();
            sync(entity);
        } else {
            totalVelocity = Vec3.ZERO;
        }
    }

    public void sync(final Entity entity) {
    }

    @Override
    public void addTotalVelocity(final Vec3 vel) {
        if (vel == null || !Double.isFinite(vel.x) || !Double.isFinite(vel.y) || !Double.isFinite(vel.z)) {
            return;
        }
        totalVelocity = clampAccumulatedVelocity(totalVelocity.add(vel));
    }

    public static Vec3 clampAccumulatedVelocity(final Vec3 velocity) {
        if (velocity == null) {
            return Vec3.ZERO;
        }
        final double lenSqr = velocity.lengthSqr();
        final double maxSqr = MAX_ACCUMULATED_LAUNCH_SPEED * MAX_ACCUMULATED_LAUNCH_SPEED;
        if (lenSqr > maxSqr) {
            return velocity.normalize().scale(MAX_ACCUMULATED_LAUNCH_SPEED);
        }
        return velocity;
    }

    private void applyAccumulatedVelocity() {
        final Vec3 launch = clampAccumulatedVelocity(totalVelocity);
        totalVelocity = Vec3.ZERO;
        if (launch.lengthSqr() > 0.01 && entity.isAlive() && !entity.isRemoved()) {
            // Lift off ground to stop friction from cutting the launch short
            Vec3i localUp = GravityChangerAPI.getGravityDirection(entity).getOpposite().getUnitVec3i();
            double upX = entity.getX() + localUp.getX() * 0.1;
            double upY = entity.getY() + localUp.getY() * 0.1;
            double upZ = entity.getZ() + localUp.getZ() * 0.1;
            entity.teleportTo(upX, upY, upZ);
            entity.setOnGround(false);

            addVelocity(entity, launch.x, launch.y, launch.z);
        }
    }

    @Override
    public boolean tick() {
        if (ticks <= 0) {
            return false;
        }

        // Allow an unfrozen attacker (e.g. SPTW using SP3 Grab during Time Stop) to keep holding/positioning the victim.
        if (!entity.level().isClientSide() && entity instanceof net.minecraft.world.entity.LivingEntity living) {
            final CommonGrabComponent grab = JComponentPlatformUtils.getGrab(living);
            if (grab instanceof CommonGrabComponentImpl grabImpl
                    && grabImpl.getAttacker() != null
                    && grabImpl.getAttacker().isAlive()
                    && !JUtils.isAffectedByTimeStop(grabImpl.getAttacker())) {
                grabImpl.tick();
            }
        }

        stopTick(entity);
        for (final Entity passenger : entity.getPassengers()) {
            stopTick(passenger);
        }
        ticks--;

        if (ticks == 0) {
            if (!entity.level().isClientSide()) {
                applyAccumulatedVelocity();
            } else {
                totalVelocity = Vec3.ZERO;
            }
        }

        return true;
    }

    public void readFromNbt(final @NonNull CompoundTag tag) {
        ticks = tag.getIntOr("Ticks", 0);
    }

    public void writeToNbt(final @NonNull CompoundTag tag) {
        tag.putInt("Ticks", ticks);
    }
}
