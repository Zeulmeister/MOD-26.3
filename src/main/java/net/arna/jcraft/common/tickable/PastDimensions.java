package net.arna.jcraft.common.tickable;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.saveddata.PastDimensionsData;
import net.arna.jcraft.common.util.DimensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PastDimensions {
    protected static final List<DimensionData> dimensions = new ArrayList<>();
    protected static final Map<UUID, PastDimensionsData.Entry> pendingEntries = new LinkedHashMap<>();
    private static boolean dirty = false;

    public static void enqueue(DimensionData dimensionData) {
        if (dimensionData.getUser() != null) {
            pendingEntries.remove(dimensionData.getUser().getUUID());
        }
        dimensions.add(dimensionData);
        dirty = true;
    }

    public static void remove(DimensionData dimensionData) {
        if (dimensions.remove(dimensionData)) {
            dirty = true;
        }
    }

    public static void load(MinecraftServer server) {
        dimensions.clear();
        pendingEntries.clear();
        dirty = false;
        PastDimensionsData data = PastDimensionsData.fromDefaultFile(server);
        for (PastDimensionsData.Entry entry : data.getEntries()) {
            pendingEntries.put(entry.uuid(), entry);
        }
        resolvePending(server);
    }

    public static void save(MinecraftServer server) {
        if (server == null) return;
        if (!dirty && dimensions.isEmpty() && pendingEntries.isEmpty()) {
            return;
        }
        Map<UUID, PastDimensionsData.Entry> snapshot = new LinkedHashMap<>(pendingEntries);
        for (DimensionData dd : dimensions) {
            Entity user = dd.getUser();
            if (user == null || !user.isAlive() || user.isRemoved()) continue;
            snapshot.put(user.getUUID(), new PastDimensionsData.Entry(
                    user.getUUID(),
                    dd.getWorldKey(),
                    dd.getPos(),
                    Math.max(1, dd.getTimer())
            ));
        }
        PastDimensionsData.saveToDefaultFile(server, snapshot.values());
        dirty = false;
    }

    public static void clearAll() {
        dimensions.clear();
        pendingEntries.clear();
        dirty = false;
    }

    private static void resolvePending(MinecraftServer server) {
        if (pendingEntries.isEmpty() || server == null || JCraft.auWorld == null) {
            return;
        }
        Iterator<Map.Entry<UUID, PastDimensionsData.Entry>> it = pendingEntries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PastDimensionsData.Entry> pair = it.next();
            UUID uuid = pair.getKey();
            PastDimensionsData.Entry entry = pair.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                if (player.level() == JCraft.auWorld && player.isAlive() && !hasActiveEntry(player)) {
                    dimensions.add(new DimensionData(player, entry.pos(), entry.worldKey(), Math.max(1, entry.timer())));
                }
                it.remove();
                dirty = true;
                continue;
            }

            Entity entity = JCraft.auWorld.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                if (living.isAlive() && !living.isRemoved() && !hasActiveEntry(living)) {
                    dimensions.add(new DimensionData(living, entry.pos(), entry.worldKey(), Math.max(1, entry.timer())));
                }
                it.remove();
                dirty = true;
            }
        }
    }

    private static boolean hasActiveEntry(LivingEntity entity) {
        for (DimensionData dd : dimensions) {
            if (dd.getUser() == entity) {
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        if (JCraft.auWorld == null) {
            return;
        }
        resolvePending(server);

        for (ServerPlayer serverPlayer : JCraft.auWorld.players()) {
            boolean contained = false;
            for (DimensionData dimensionData : dimensions) {
                if (dimensionData.getUser() == serverPlayer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) safeReturn(serverPlayer);
        }

        List<DimensionData> newDimensions = new ArrayList<>();

        for (DimensionData dimensionData : dimensions) {
            Entity user = dimensionData.getUser();
            if (user == null || !user.isAlive() || user.isRemoved()) {
                dirty = true;
                continue;
            }

            ServerLevel original = server.getLevel(dimensionData.getWorldKey());
            if (original == null || user.level() == original) {
                dirty = true;
                continue;
            }

            dimensionData.decreaseTimer();
            dirty = true;
            if (dimensionData.getTimer() > 1) {
                newDimensions.add(dimensionData);
                continue;
            }

            Vec3 dimPos = user.position().add(0d, (JCraft.auWorld.getHeight() - original.getHeight()) / 2, 0d); //dimValues.pos;
            if (user instanceof ServerPlayer player) {
                player.teleportTo(original, dimPos.x, dimPos.y, dimPos.z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            } else {
                JCraft.teleportToWorld(user, original, dimPos.x, dimPos.y, dimPos.z);
            }
        }

        if (JCraft.preloadLockTicks <= 0 && newDimensions.isEmpty()) // Nobody left in AU
        {
            JCraft.clearPreloadedChunks();
        }

        dimensions.clear();
        dimensions.addAll(newDimensions);
    }

    /**
     * Returns a player to their respawn point.
     * Used when PastDimensions does not have a return DimensionData specified for a player in the jcraft:audim
     */
    public static void safeReturn(ServerPlayer serverPlayer) {
        //todo: implement NBT saving for PastDimensions!
        JCraft.LOGGER.warn("PastDimensions.safeReturn called on " + serverPlayer);
        var respawnCfg = serverPlayer.getRespawnConfig();
        BlockPos spawnPos = respawnCfg != null ? respawnCfg.respawnData().pos() : serverPlayer.blockPosition();
        var respawnDim = respawnCfg != null ? respawnCfg.respawnData().dimension() : net.minecraft.world.level.Level.OVERWORLD;
        PastDimensions.enqueue(new DimensionData(serverPlayer, new Vec3(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()), respawnDim));
    }

    public static boolean tryExit(LivingEntity user, Set<? extends Entity> targets) {
        boolean isStored = false;

        for (DimensionData dimV : dimensions) {
            // Bring others out of the AU
            if (targets.contains(dimV.getUser())) {
                dimV.setTimer(1);
                continue;
            }

            if (dimV.getUser() != user) {
                continue;
            }
            isStored = true;
            dimV.setTimer(1);
            dirty = true;
        }

        return isStored;
    }
}
