package net.arna.jcraft.common.tickable;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.stand.GEREntity;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.common.util.DimensionData;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * For clientside timestops, see {@link net.arna.jcraft.client.util.JClientUtils#activeTimestops}
 */
public class Timestops {
    protected static final List<DimensionData> TIMESTOPS = new ArrayList<>();
    private static @Nullable BiPredicate<ResourceKey<Level>, BlockPos> clientTsRangeChecker = null;

    public static void setClientTsRangeChecker(@Nullable final BiPredicate<ResourceKey<Level>, BlockPos> checker) {
        clientTsRangeChecker = checker;
    }

    public static void enqueue(final DimensionData dimensionData) {
        TIMESTOPS.removeIf(existing -> existing == null || existing.getUser() == dimensionData.getUser());
        TIMESTOPS.add(dimensionData);
    }

    public static void remove(final DimensionData dimensionData) {
        TIMESTOPS.remove(dimensionData);
    }

    public static void clearAll() {
        TIMESTOPS.clear();
    }

    /**
     * Common-side. Thus cannot access serverside data.
     */
    public static final Predicate<Entity> TIMESTOP_PREDICATE = entity -> {
        if (entity instanceof Player player) {
            if (entity.isSpectator()) {
                return false;
            }
            if (player.isCreative()) {
                return false;
            }
        }
        if (entity instanceof final LivingEntity living) {
            StandEntity<?, ?> stand = null;
            if (living instanceof final StandEntity<?,?> livingStand) {
                stand = livingStand;
            } else {
                CommonStandComponent standComponent = JComponentPlatformUtils.getStandComponent(living);
                if (standComponent.getStand() != null) {
                    stand = standComponent.getStand();
                }
            }

            if (stand != null) {
                if (stand instanceof final KingCrimsonEntity kingCrimson) {
                    if (kingCrimson.getTETime() > 0) {
                        return false;
                    }
                }
                if (stand instanceof final GEREntity requiem) {
                    if (requiem.getState() == GEREntity.State.COUNTER) {
                        return false;
                    }
                }
            }
        }
        return true;
    };

    public static void tick(final MinecraftServer server) {
        final List<DimensionData> newActiveTimestops = new ArrayList<>();
        final List<DimensionData> expiredTimestops = new ArrayList<>();

        for (final DimensionData timestop : TIMESTOPS) {
            if (timestop == null) {
                continue;
            }
            final Entity user = timestop.getUser();
            final int timer = timestop.getTimer();
            final boolean userValid = user != null
                    && user.isAlive()
                    && !user.isRemoved()
                    && user.level().dimension().equals(timestop.getWorldKey());

            if (userValid && timer > 0) {
                timestop.decreaseTimer();
                if (timestop.getTimer() <= 0) {
                    expiredTimestops.add(timestop);
                    continue;
                }
                final ServerLevel world = server.getLevel(timestop.getWorldKey());
                if (world == null) {
                    JCraft.LOGGER.warn("World that timestop belongs to no longer exists! Key: " + timestop.getWorldKey() + " Timestopper: " + user);
                    expiredTimestops.add(timestop);
                    continue;
                }

                final Vec3 pos = user.position();
                timestop.setPos(pos);

                net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.tickActiveTimestop(world, user, pos);

                final List<? extends Entity> toStop = world.getEntitiesOfClass(Entity.class,
                        new AABB(pos.add(160.0, 160.0, 160.0), pos.subtract(160.0, 160.0, 160.0)), TIMESTOP_PREDICATE);

                for (final Entity entity : toStop) {
                    if (
                            !entity.isPassenger()
                            && entity != user
                            && !(user instanceof LivingEntity livingUser && entity == net.arna.jcraft.common.util.JUtils.getStand(livingUser))
                            && entity != user.getVehicle()
                            && !user.isPassengerOfSameVehicle(entity)
                            && !net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.shouldAllowInitialTravel(entity, user)
                    ) {
                        // Refresh only when close to expiring to avoid syncing hundreds of entities every single tick.
                        JComponentPlatformUtils.getTimeStopData(entity).ifPresent(d -> {
                            if (d.getTicks() <= 1) {
                                d.setTicks(4);
                            }
                        });
                    }
                }

                newActiveTimestops.add(timestop);
            } else {
                expiredTimestops.add(timestop);
            }
        }

        TIMESTOPS.clear();
        TIMESTOPS.addAll(newActiveTimestops);

        for (final DimensionData expired : expiredTimestops) {
            final Entity expiredUser = expired.getUser();
            final ServerLevel serverWorld = server.getLevel(expired.getWorldKey());
            if (expiredUser instanceof LivingEntity livingUser) {
                final StandEntity<?, ?> stand = net.arna.jcraft.common.util.JUtils.getStand(livingUser);
                if (stand != null && stand.getTsTime() > 0) {
                    stand.setTsTime(0);
                }
            }
            if (expiredUser instanceof Player expiredPlayer) {
                net.arna.jcraft.api.misc.ConditionalFlightHandler.revokeStaleFlightIfNotAllowed(expiredPlayer);
            }
            if (serverWorld != null) {
                if (expiredUser != null) {
                    net.arna.jcraft.common.network.s2c.TimeStopStatePacket.send(serverWorld.players(),
                            net.arna.jcraft.common.network.s2c.TimeStopStatePacket.createStopPacket(expiredUser.getId()));
                    net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.onTimeStopEnd(expiredUser, serverWorld);
                }
                final Vec3 centerPos = expired.getPos() != null
                        ? expired.getPos()
                        : (expiredUser != null ? expiredUser.position() : Vec3.ZERO);
                for (final ServerPlayer sp : serverWorld.players()) {
                    if (!isInTSRange(serverWorld.dimension(), sp.position())) {
                        net.arna.jcraft.common.network.s2c.ShaderDeactivationPacket.send(sp, net.arna.jcraft.common.network.s2c.ShaderActivationPacket.Type.ZA_WARUDO);
                        if (sp != expiredUser) {
                            clearPlayerItemCooldowns(sp);
                        }
                    }
                }
                releaseFrozenEntities(serverWorld, centerPos, expiredUser);
            } else if (expiredUser != null) {
                net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.onTimeStopEnd(expiredUser, null);
            }
        }
    }

    public static void releaseFrozenEntities(final ServerLevel world, final @Nullable Vec3 centerPos, final @Nullable Entity excludeUser) {
        if (world == null || centerPos == null) {
            return;
        }
        final List<? extends Entity> frozen = world.getEntitiesOfClass(Entity.class,
                new AABB(centerPos.add(168.0, 168.0, 168.0), centerPos.subtract(168.0, 168.0, 168.0)));
        for (final Entity entity : frozen) {
            if (entity == excludeUser) {
                continue;
            }
            if (!isInTSRange(world.dimension(), entity.position())) {
                JComponentPlatformUtils.getTimeStopData(entity).ifPresent(d -> {
                    if (d.getTicks() > 0) {
                        d.releaseNow();
                    }
                });
            }
        }
    }

    public static void clearPlayerItemCooldowns(final ServerPlayer serverPlayer) {
        for (int i = 0; i < 36; i++) {
            final ItemStack st = serverPlayer.getInventory().getItem(i);
            if (!st.isEmpty()) {
                serverPlayer.getCooldowns().removeCooldown(serverPlayer.getCooldowns().getCooldownGroup(st));
            }
        }
        if (!serverPlayer.getOffhandItem().isEmpty()) {
            serverPlayer.getCooldowns().removeCooldown(serverPlayer.getCooldowns().getCooldownGroup(serverPlayer.getOffhandItem()));
        }
    }

    public static boolean isInTSRangeForLevel(final Level level, final BlockPos pos) {
        if (level.isClientSide()) {
            return clientTsRangeChecker != null && clientTsRangeChecker.test(level.dimension(), pos);
        }
        return isInTSRange(level.dimension(), pos);
    }

    public static boolean isInTSRange(final ResourceKey<Level> dimension, final Vec3 pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getTimer() > 0 && dimension.equals(timeStop.getWorldKey())) {
                Vec3 center = timeStop.getUser() != null ? timeStop.getUser().position() : timeStop.getPos();
                if (center != null && center.distanceToSqr(pos.x(), pos.y(), pos.z()) <= 65536.0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInTSRange(final ResourceKey<Level> dimension, final BlockPos pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getTimer() > 0 && dimension.equals(timeStop.getWorldKey())) {
                Vec3 center = timeStop.getUser() != null ? timeStop.getUser().position() : timeStop.getPos();
                if (center != null && center.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536.0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInTSRange(Vec3 pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getTimer() > 0) {
                Vec3 center = timeStop.getUser() != null ? timeStop.getUser().position() : timeStop.getPos();
                if (center != null && center.distanceToSqr(pos.x(), pos.y(), pos.z()) <= 65536.0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isInTSRange(BlockPos pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getTimer() > 0) {
                Vec3 center = timeStop.getUser() != null ? timeStop.getUser().position() : timeStop.getPos();
                if (center != null && center.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536.0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getTicksIfInTSRange(final ResourceKey<Level> dimension, final BlockPos pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getTimer() > 0 && dimension.equals(timeStop.getWorldKey())) {
                Vec3 center = timeStop.getUser() != null ? timeStop.getUser().position() : timeStop.getPos();
                if (center != null && center.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536.0) {
                    return timeStop.getTimer();
                }
            }
        }
        return 0;
    }

    public static int getTicksIfInTSRange(BlockPos pos) {
        for (DimensionData timeStop : TIMESTOPS) {
            if (timeStop != null && timeStop.getUser() != null && timeStop.getUser().distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 65536.0) {
                return timeStop.getTimer();
            }
        }

        return 0;
    }

    public static boolean isUserOfActiveTimestop(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        for (DimensionData data : TIMESTOPS) {
            if (data != null && data.getUser() == entity && data.getTimer() > 0) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable DimensionData getTimestop(Entity entity) {
        for (DimensionData data : TIMESTOPS) {
            if (data.getUser() == entity) {
                return data;
            }
        }
        return null;
    }
}
