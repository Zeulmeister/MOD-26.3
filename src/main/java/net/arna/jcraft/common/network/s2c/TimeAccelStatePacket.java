package net.arna.jcraft.common.network.s2c;

import dev.architectury.event.events.common.TickEvent;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.NonNull;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Packet sent to clients when MIH's time acceleration ult is used.
 * Sent on start and stop (with authoritative server dayTime for smooth client ease-out resync).
 */
public class TimeAccelStatePacket {
    private static final int SMOOTH_RESYNC_TICKS = 20;
    private static final Int2ObjectMap<TimeAcceleration> accelerations = new Int2ObjectOpenHashMap<>();
    private static final Object lock = new Object();
    private static final WeakHashMap<Level, Long> startDayTime = new WeakHashMap<>();
    private static final WeakHashMap<Level, Long> lastAppliedDayTime = new WeakHashMap<>();
    private static final WeakHashMap<Level, SmoothTimeSync> smoothSyncs = new WeakHashMap<>();

    public static void init() {
        // Decrease all durations.
        TickEvent.SERVER_POST.register(TimeAccelStatePacket::tick);

        // Handle acceleration on server.
        TickEvent.SERVER_LEVEL_POST.register(world -> {
            if (!((net.minecraft.server.level.ServerLevel) world).getGameRules().get(GameRules.ADVANCE_TIME)) {
                return;
            }

            applyAcceleration(world, t -> net.arna.jcraft.common.util.JUtils.setDayTime(world, t));
        });
    }

    public static void tickClient() {
        tick(null);
    }

    public static boolean hasActiveAcceleration() {
        synchronized (lock) {
            return !accelerations.isEmpty();
        }
    }

    public static float getMaxAccelerationIntensity() {
        synchronized (lock) {
            if (accelerations.isEmpty()) {
                return 0.0f;
            }
            long now = System.currentTimeMillis();
            double best = 0.0;
            for (TimeAcceleration ta : accelerations.values()) {
                double durMs = Math.max(50.0, ta.getInitialDuration() * 50.0);
                double x = Mth.clamp((now - ta.getStartTime()) / durMs, 0.0, 1.0);
                double curve = someBsArnaPutTogetherInDesmos(x);
                if (curve > best) {
                    best = curve;
                }
            }
            return (float) Mth.clamp(best, 0.25, 1.0);
        }
    }

    public static void addAcceleration(final int mihEntityId, final int duration, final long startTime) {
        synchronized (lock) {
            accelerations.put(mihEntityId, new TimeAcceleration(duration, mihEntityId, startTime));
        }
    }

    public static void removeAcceleration(final int mihEntityId) {
        synchronized (lock) {
            accelerations.remove(mihEntityId);
        }
    }

    public static void finishAccelerationWithSmoothSync(final int mihEntityId, final Level clientWorld, final long authoritativeServerDayTime) {
        synchronized (lock) {
            accelerations.remove(mihEntityId);
            if (clientWorld != null && accelerations.isEmpty() && authoritativeServerDayTime >= 0L) {
                long from = lastAppliedDayTime.getOrDefault(clientWorld, net.arna.jcraft.common.util.JUtils.getDayTime(clientWorld));
                if (from != authoritativeServerDayTime) {
                    smoothSyncs.put(clientWorld, new SmoothTimeSync(from, authoritativeServerDayTime, SMOOTH_RESYNC_TICKS, SMOOTH_RESYNC_TICKS));
                }
            }
        }
    }

    /**
     * Cubic ease-out interpolation used to glide client dayTime smoothly onto the authoritative server dayTime
     * at the end of Made in Heaven's Time Acceleration, avoiding any abrupt sun/moon snap.
     */
    public static long interpolateSmoothDayTime(final long fromDayTime, final long toDayTime, final float progress) {
        final float p = Mth.clamp(progress, 0.0f, 1.0f);
        final float inv = 1.0f - p;
        final float eased = 1.0f - inv * inv * inv;
        return fromDayTime + Math.round((toDayTime - fromDayTime) * (double) eased);
    }

    public static void sendStart(@NonNull final MadeInHeavenEntity mih, int duration) {
        final long time = System.currentTimeMillis();
        final var server = Objects.requireNonNull(mih.getEntityWorld().getServer());

        // Dedicated server sends to all players; published LAN host sends only to remote LAN guests
        // (the local host shares this static accelerations map in singleplayer/LAN).
        if (server.isDedicatedServer() || server.isPublished()) {
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeEnum(State.START);
            buf.writeVarInt(mih.getId());
            buf.writeVarInt(duration);
            buf.writeLong(time); // for time syncing

            var recipients = ((ServerLevel) mih.level()).players().stream()
                    .filter(p -> server.isDedicatedServer() || !server.isSingleplayerOwner(p.nameAndId()))
                    .toList();
            if (!recipients.isEmpty()) {
                net.arna.jcraft.common.network.JNetwork.sendToPlayers(recipients, JPacketRegistry.S2C_TIME_ACCELERATION_STATE, buf);
            }
        }

        synchronized (lock) {
            accelerations.put(mih.getId(), new TimeAcceleration(duration, mih.getId(), time));
        }
    }

    public static void sendStop(@NonNull final MadeInHeavenEntity mih) {
        final var server = Objects.requireNonNull(mih.level().getServer());
        final long finalDayTime = net.arna.jcraft.common.util.JUtils.getDayTime(mih.level());
        if (server.isDedicatedServer() || server.isPublished()) {
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeEnum(State.STOP);
            buf.writeVarInt(mih.getId());
            buf.writeLong(finalDayTime);

            var recipients = ((ServerLevel) mih.level()).players().stream()
                    .filter(p -> server.isDedicatedServer() || !server.isSingleplayerOwner(p.nameAndId()))
                    .toList();
            if (!recipients.isEmpty()) {
                net.arna.jcraft.common.network.JNetwork.sendToPlayers(recipients, JPacketRegistry.S2C_TIME_ACCELERATION_STATE, buf);
            }
        }

        synchronized (lock) {
            accelerations.remove(mih.getId());
        }
    }

    @SuppressWarnings("unused")
    private static double someBsArnaPutTogetherInDesmos(double x) {
        return Math.sqrt(1 - Math.pow(2 * x * x - 1, 2));
    }

    private static double integralOfSomeBsArnaPutTogetherInDesmos(double x) {
        double x2 = x * x;
        return (x2 - 1) * Math.sqrt(1 - x2) + 1;
    }

    public static void applyAcceleration(Level world, Consumer<Long> dayTimeSetter) {
        synchronized (lock) {
            if (accelerations.isEmpty()) {
                startDayTime.put(world, -1L);
                SmoothTimeSync sync = smoothSyncs.get(world);
                if (sync != null) {
                    float progress = 1.0f - (sync.ticksRemaining / (float) Math.max(1, sync.totalTicks));
                    long smoothed = interpolateSmoothDayTime(sync.fromDayTime, sync.targetDayTime, progress);
                    lastAppliedDayTime.put(world, smoothed);
                    dayTimeSetter.accept(smoothed);
                    sync.ticksRemaining--;
                    if (sync.ticksRemaining < 0) {
                        smoothSyncs.remove(world);
                    }
                }
                return;
            }

            smoothSyncs.remove(world);
            long start = startDayTime.getOrDefault(world, -1L);
            if (start < 0) {
                startDayTime.put(world, start = net.arna.jcraft.common.util.JUtils.getDayTime(world));
            }

            long total = accelerations.values().stream()
                    .filter(ta -> ta.isValid(world))
                    .mapToLong(ta -> (long) Mth.lerp(integralOfSomeBsArnaPutTogetherInDesmos(Math.min(1,
                            (System.currentTimeMillis() - ta.getStartTime()) / (ta.getInitialDuration() * 50d))), 0, 240000))
                    .sum();

            long nextTime = start + total;
            lastAppliedDayTime.put(world, nextTime);
            dayTimeSetter.accept(nextTime);
        }
    }

    private static void tick(Object o) {
        // Avoid thread-safety issues by locking on our lock.
        synchronized (lock) {
            if (accelerations.isEmpty()) return;

            boolean done = accelerations.values().stream()
                    .allMatch(ta -> {
                        if (ta.getDuration() > 0) {
                            ta.decrementDuration();
                        }

                        return ta.getDuration() <= 0;
                    });

            if (done) {
                accelerations.clear();
            }
        }
    }

    public enum State {
        START, STOP
    }

    private static final class SmoothTimeSync {
        final long fromDayTime;
        final long targetDayTime;
        int ticksRemaining;
        final int totalTicks;

        SmoothTimeSync(long fromDayTime, long targetDayTime, int ticksRemaining, int totalTicks) {
            this.fromDayTime = fromDayTime;
            this.targetDayTime = targetDayTime;
            this.ticksRemaining = ticksRemaining;
            this.totalTicks = totalTicks;
        }
    }

    @Data
    public static class TimeAcceleration {
        private int duration;
        private double lastAcceleration;
        private final int initialDuration;
        private final long startTime;
        private final int entityId;

        public TimeAcceleration(int duration, int entityId, long startTime) {
            this.duration = this.initialDuration = duration;
            this.entityId = entityId;
            this.startTime = startTime;
        }

        public boolean isValid(Level world) {
            return world.isClientSide() || world.getEntity(entityId) instanceof MadeInHeavenEntity;
        }

        public void decrementDuration() {
            duration--;
        }
    }
}
