package net.arna.jcraft.common.combat;

import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kinetic Time-Stop Projectile Physics (Minecraft 26.3 Upgrade):
 * Projectiles/knives thrown by a Time Stop user travel ~1.8 blocks outward,
 * smoothly decelerate into mid-air suspension with menacing kinetic sparks,
 * accumulate +25% kinetic momentum, and launch simultaneously when time resumes.
 */
public final class TimeStopKineticPhysicsHandler {
    public static final double INITIAL_TRAVEL_DISTANCE = 1.8;
    public static final int MAX_INITIAL_TRAVEL_TICKS = 4;
    public static final double DECELERATION_FACTOR = 0.55;
    public static final double SUSPENSION_SPEED_SQR_THRESHOLD = 0.0025;
    public static final double KINETIC_MOMENTUM_MULTIPLIER = 1.25;

    private static final Map<Integer, SuspendedProjectileEntry> TRACKED_PROJECTILES = new ConcurrentHashMap<>();

    private TimeStopKineticPhysicsHandler() {}

    public static final class SuspendedProjectileEntry {
        public final int entityId;
        public final UUID ownerUuid;
        public final Vec3 spawnPos;
        public Vec3 originalVelocity;
        public final boolean wasNoGravity;
        public int ticksTracked = 0;
        public boolean suspended = false;
        public Vec3 suspendedPos = Vec3.ZERO;
        public float lockedYRot;
        public float lockedXRot;

        public SuspendedProjectileEntry(Projectile projectile, UUID ownerUuid) {
            this.entityId = projectile.getId();
            this.ownerUuid = ownerUuid;
            this.spawnPos = projectile.position();
            this.originalVelocity = projectile.getDeltaMovement();
            this.wasNoGravity = projectile.isNoGravity();
            this.lockedYRot = projectile.getYRot();
            this.lockedXRot = projectile.getXRot();
        }
    }

    /**
     * Pure physics curve helper for kinetic deceleration during Time Stop.
     */
    public static Vec3 computeDeceleratedVelocity(Vec3 currentVel, double distanceFromSpawn, int ticksTracked) {
        if (distanceFromSpawn < INITIAL_TRAVEL_DISTANCE && ticksTracked < MAX_INITIAL_TRAVEL_TICKS) {
            return currentVel;
        }
        Vec3 slowed = currentVel.scale(DECELERATION_FACTOR);
        if (slowed.lengthSqr() <= SUSPENSION_SPEED_SQR_THRESHOLD) {
            return Vec3.ZERO;
        }
        return slowed;
    }

    /**
     * Pure physics helper for accumulated kinetic momentum upon Time Stop release (+25% speed bonus).
     */
    public static Vec3 computeReleaseVelocity(Vec3 originalVelocity) {
        if (originalVelocity == null || originalVelocity.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return originalVelocity.scale(KINETIC_MOMENTUM_MULTIPLIER);
    }

    /**
     * Returns true if this entity is a projectile owned by the active Time Stop user and is still
     * in its initial 1.8m launch travel phase (so Timestops.tick shouldn't freeze it prematurely).
     */
    public static boolean shouldAllowInitialTravel(Entity entity, @Nullable Entity timestopUser) {
        if (timestopUser == null || !(entity instanceof Projectile projectile)) {
            return false;
        }
        if (projectile.getOwner() != timestopUser) {
            return false;
        }
        if (projectile instanceof AbstractArrow arrow && arrow.isInGround()) {
            return false;
        }
        SuspendedProjectileEntry entry = TRACKED_PROJECTILES.get(projectile.getId());
        if (entry == null) {
            return true;
        }
        return !entry.suspended;
    }

    public static void tickActiveTimestop(ServerLevel world, Entity user, Vec3 centerPos) {
        if (user == null || !user.isAlive()) {
            return;
        }
        UUID userUuid = user.getUUID();
        AABB box = new AABB(centerPos.add(160.0, 160.0, 160.0), centerPos.subtract(160.0, 160.0, 160.0));
        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class, box,
                p -> p.isAlive() && p.getOwner() == user && !(p instanceof AbstractArrow arrow && arrow.isInGround()));

        for (Projectile projectile : projectiles) {
            SuspendedProjectileEntry entry = TRACKED_PROJECTILES.computeIfAbsent(
                    projectile.getId(),
                    id -> new SuspendedProjectileEntry(projectile, userUuid)
            );
            if (entry.originalVelocity.lengthSqr() < 0.01 && projectile.getDeltaMovement().lengthSqr() > 0.01) {
                entry.originalVelocity = projectile.getDeltaMovement();
                entry.lockedYRot = projectile.getYRot();
                entry.lockedXRot = projectile.getXRot();
            }

            entry.ticksTracked++;

            if (!entry.suspended) {
                double dist = projectile.position().distanceTo(entry.spawnPos);
                Vec3 nextVel = computeDeceleratedVelocity(projectile.getDeltaMovement(), dist, entry.ticksTracked);
                if (nextVel.equals(Vec3.ZERO)) {
                    entry.suspended = true;
                    entry.suspendedPos = projectile.position();
                    entry.lockedYRot = projectile.getYRot();
                    entry.lockedXRot = projectile.getXRot();
                    projectile.setNoGravity(true);
                    projectile.setDeltaMovement(Vec3.ZERO);
                    projectile.syncVelocity = true;
                    JComponentPlatformUtils.getTimeStopData(projectile).ifPresent(d -> d.setTicks(3));
                    world.sendParticles(JParticleTypeRegistry.GO.get(),
                            entry.suspendedPos.x, entry.suspendedPos.y + 0.1, entry.suspendedPos.z,
                            2, 0.12, 0.12, 0.12, 0.01);
                } else {
                    JComponentPlatformUtils.getTimeStopData(projectile).ifPresent(d -> d.setTicks(0));
                    projectile.setNoGravity(true);
                    projectile.setDeltaMovement(nextVel);
                    projectile.syncVelocity = true;
                }
            } else {
                projectile.setPos(entry.suspendedPos.x, entry.suspendedPos.y, entry.suspendedPos.z);
                projectile.setYRot(entry.lockedYRot);
                projectile.setXRot(entry.lockedXRot);
                projectile.setDeltaMovement(Vec3.ZERO);
                JComponentPlatformUtils.getTimeStopData(projectile).ifPresent(d -> d.setTicks(3));
                if (entry.ticksTracked % 10 == 0) {
                    world.sendParticles(JParticleTypeRegistry.GO.get(),
                            entry.suspendedPos.x, entry.suspendedPos.y + 0.1, entry.suspendedPos.z,
                            1, 0.08, 0.08, 0.08, 0.005);
                }
            }
        }

        if (user instanceof ServerPlayer sp) {
            sendSuspendedCount(sp, getSuspendedCountForUser(userUuid));
        }
    }

    public static void onTimeStopEnd(@Nullable Entity user, @Nullable ServerLevel world) {
        if (user == null) {
            return;
        }
        UUID userUuid = user.getUUID();
        int released = 0;

        Iterator<Map.Entry<Integer, SuspendedProjectileEntry>> iter = TRACKED_PROJECTILES.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, SuspendedProjectileEntry> mapEntry = iter.next();
            SuspendedProjectileEntry entry = mapEntry.getValue();
            if (!entry.ownerUuid.equals(userUuid)) {
                continue;
            }
            iter.remove();
            if (world == null) {
                continue;
            }
            Entity entity = world.getEntity(entry.entityId);
            if (entity instanceof Projectile projectile && projectile.isAlive()) {
                JComponentPlatformUtils.getTimeStopData(projectile).ifPresent(d -> d.setTicks(0));
                projectile.setNoGravity(entry.wasNoGravity);
                projectile.setYRot(entry.lockedYRot);
                projectile.setXRot(entry.lockedXRot);
                Vec3 launchVel = computeReleaseVelocity(entry.originalVelocity);
                projectile.setDeltaMovement(launchVel);
                projectile.syncVelocity = true;
                projectile.needsSync = true;
                world.sendParticles(JParticleTypeRegistry.AURA_ARC.get(),
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        3, 0.15, 0.15, 0.15, 0.05);
                released++;
            }
        }

        if (released > 0 && world != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0f, 1.35f);
        }
        if (user instanceof ServerPlayer sp) {
            sendSuspendedCount(sp, 0);
        }
    }

    public static int getSuspendedCountForUser(UUID userUuid) {
        int count = 0;
        for (SuspendedProjectileEntry entry : TRACKED_PROJECTILES.values()) {
            if (entry.ownerUuid.equals(userUuid)) {
                count++;
            }
        }
        return count;
    }

    private static void sendSuspendedCount(ServerPlayer player, int count) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(count);
        JNetwork.sendToPlayer(player, JPacketRegistry.S2C_KINETIC_TS_COUNT, buf);
    }
}
