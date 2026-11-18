package net.arna.jcraft.common.tickable;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.stand.GEREntity;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.common.util.DimensionData;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * For clientside timestops, see {@link net.arna.jcraft.client.util.JClientUtils#activeTimestops}
 */
public class Timestops {
    protected static final List<DimensionData> TIMESTOPS = new ArrayList<>();

    public static void enqueue(final DimensionData dimensionData) {
        TIMESTOPS.add(dimensionData);
    }

    public static void remove(final DimensionData dimensionData) {
        TIMESTOPS.remove(dimensionData);
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
        final List<Entity> expiredUsers = new ArrayList<>();

        for (final DimensionData timestop : TIMESTOPS) {
            final Entity user = timestop.getUser();
            final int timer = timestop.getTimer();
            if (user != null && user.isAlive() && timer > 0) {
                timestop.decreaseTimer();
                if (timestop.getTimer() <= 0) {
                    expiredUsers.add(user);
                    continue;
                }
                final ServerLevel world = server.getLevel(timestop.getWorldKey());
                if (world == null) {
                    JCraft.LOGGER.warn("World that timestop belongs to no longer exists! Key: " + timestop.getWorldKey() + " Timestopper: " + user);
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
                        JComponentPlatformUtils.getTimeStopData(entity).ifPresent(d -> d.setTicks(3));
                    }
                }

                newActiveTimestops.add(timestop);
            }
        }

        TIMESTOPS.clear();
        TIMESTOPS.addAll(newActiveTimestops);

        for (final Entity expiredUser : expiredUsers) {
            // Re-enqueue momentarily so JCraft.stopTimestop finds the DimensionData cleanly, or stop directly
            if (expiredUser.level() instanceof ServerLevel serverWorld) {
                net.arna.jcraft.common.network.s2c.TimeStopStatePacket.send(serverWorld.players(),
                        net.arna.jcraft.common.network.s2c.TimeStopStatePacket.createStopPacket(expiredUser.getId()));
                net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.onTimeStopEnd(expiredUser, serverWorld);
                Vec3 pos = expiredUser.position();
                for (net.minecraft.server.level.ServerPlayer sp : serverWorld.getEntitiesOfClass(
                        net.minecraft.server.level.ServerPlayer.class,
                        new AABB(pos.add(160.0, 160.0, 160.0), pos.subtract(160.0, 160.0, 160.0)),
                        net.minecraft.world.entity.EntitySelector.LIVING_ENTITY_STILL_ALIVE)) {
                    net.arna.jcraft.common.network.s2c.ShaderDeactivationPacket.send(sp, net.arna.jcraft.common.network.s2c.ShaderActivationPacket.Type.ZA_WARUDO);
                }
            }
        }
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
