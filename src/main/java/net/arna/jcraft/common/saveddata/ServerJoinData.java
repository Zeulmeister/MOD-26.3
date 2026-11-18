package net.arna.jcraft.common.saveddata;
import net.arna.jcraft.common.util.JValueIO;

import net.arna.jcraft.JCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerJoinData extends SavedData {
    public static final Path DEFAULT_FILE_LOCATION = Path.of("data", "jcraft_serverJoined.dat");
    protected static final String KEYWORD_JOINED = "joined";
    protected static final String KEYWORD_UUID = "uuid";
    protected static final String KEYWORD_TIME = "time";

    protected final Map<UUID, Instant> joinedPlayers = new HashMap<>();

    public ServerJoinData() {}

    public ServerJoinData(final CompoundTag compoundTag) {
        if (compoundTag == null) return;
        ListTag list = compoundTag.getListOrEmpty(KEYWORD_JOINED);
        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                String u = ct.getStringOr(KEYWORD_UUID, "");
                if (!u.isEmpty()) {
                    try {
                        joinedPlayers.put(UUID.fromString(u), Instant.ofEpochSecond(ct.getLongOr(KEYWORD_TIME, 0L)));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public boolean addPlayer(final ServerPlayer player) {
        if (player == null) return false;
        UUID uuid = player.getUUID();
        if (joinedPlayers.containsKey(uuid)) return false;
        joinedPlayers.put(uuid, Instant.now());
        setDirty();
        return true;
    }

    public static File getDefaultFileLocation(final MinecraftServer server) {
        return server.storageSource.levelDirectory.path().resolve(DEFAULT_FILE_LOCATION).toFile();
    }

    public CompoundTag toTag() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Instant> entry : joinedPlayers.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putString(KEYWORD_UUID, entry.getKey().toString());
            ct.putLong(KEYWORD_TIME, entry.getValue().getEpochSecond());
            list.add(ct);
        }
        compoundTag.put(KEYWORD_JOINED, list);
        return compoundTag;
    }

    public void saveToDefaultFile(final MinecraftServer server) {
        if (!isDirty()) return;
        Path path = getDefaultFileLocation(server).toPath();
        try {
            Files.createDirectories(path.getParent());
            CompoundTag root = new CompoundTag();
            root.put("data", toTag());
            NbtIo.writeCompressed(root, path);
            setDirty(false);
        } catch (IOException e) {
            JCraft.LOGGER.warn("Failed saving ServerJoinData", e);
        }
    }

    public static ServerJoinData fromFile(final File file) {
        Path path = file.toPath();
        if (Files.exists(path)) {
            try {
                CompoundTag fileData = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                return new ServerJoinData(fileData.getCompoundOrEmpty("data"));
            } catch (Exception ex) {
                JCraft.LOGGER.warn("Failed reading ServerJoinData", ex);
            }
        }
        return new ServerJoinData();
    }

    public static ServerJoinData fromDefaultFile(final MinecraftServer server) {
        return fromFile(getDefaultFileLocation(server));
    }
}
