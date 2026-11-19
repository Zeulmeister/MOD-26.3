package net.arna.jcraft.common.saveddata;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persists active Alternate Universe (jcraft:audim) return destinations and timers per world in compressed NBT.
 */
public class PastDimensionsData extends SavedData {
    public static final Path DEFAULT_FILE_LOCATION = Path.of("data", "jcraft_pastDimensions.dat");
    private static final String KEY_DIMENSIONS = "dimensions";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_WORLD = "world";
    private static final String KEY_TIMER = "timer";
    private static final String KEY_HAS_POS = "hasPos";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";

    public record Entry(UUID uuid, ResourceKey<Level> worldKey, @Nullable Vec3 pos, int timer) {}

    private final List<Entry> entries = new ArrayList<>();

    public PastDimensionsData() {}

    public PastDimensionsData(final CompoundTag compoundTag) {
        if (compoundTag == null) return;
        ListTag list = compoundTag.getListOrEmpty(KEY_DIMENSIONS);
        for (Tag tag : list) {
            if (!(tag instanceof CompoundTag ct)) continue;
            UUID uuid = JValueIO.getUUID(ct, KEY_UUID);
            if (uuid == null) continue;
            String worldStr = ct.getStringOr(KEY_WORLD, "");
            Identifier worldId = worldStr.isEmpty() ? null : Identifier.tryParse(worldStr);
            if (worldId == null) continue;
            ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
            int timer = Math.max(1, ct.getIntOr(KEY_TIMER, 1));
            Vec3 pos = ct.getBooleanOr(KEY_HAS_POS, false)
                    ? new Vec3(ct.getDoubleOr(KEY_X, 0.0), ct.getDoubleOr(KEY_Y, 0.0), ct.getDoubleOr(KEY_Z, 0.0))
                    : null;
            entries.add(new Entry(uuid, worldKey, pos, timer));
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static Path getDefaultFileLocation(final MinecraftServer server) {
        return server.storageSource.levelDirectory.path().resolve(DEFAULT_FILE_LOCATION);
    }

    public static CompoundTag toTag(final Collection<Entry> entriesToSave) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry entry : entriesToSave) {
            CompoundTag ct = new CompoundTag();
            JValueIO.putUUID(ct, KEY_UUID, entry.uuid());
            ct.putString(KEY_WORLD, entry.worldKey().identifier().toString());
            ct.putInt(KEY_TIMER, Math.max(1, entry.timer()));
            if (entry.pos() != null) {
                ct.putBoolean(KEY_HAS_POS, true);
                ct.putDouble(KEY_X, entry.pos().x);
                ct.putDouble(KEY_Y, entry.pos().y);
                ct.putDouble(KEY_Z, entry.pos().z);
            }
            list.add(ct);
        }
        root.put(KEY_DIMENSIONS, list);
        return root;
    }

    public static void saveToDefaultFile(final MinecraftServer server, final Collection<Entry> entriesToSave) {
        if (server == null) return;
        Path path = getDefaultFileLocation(server);
        try {
            Files.createDirectories(path.getParent());
            CompoundTag root = new CompoundTag();
            root.put("data", toTag(entriesToSave));
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            NbtIo.writeCompressed(root, tmp);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JCraft.LOGGER.warn("Failed saving PastDimensionsData", e);
        }
    }

    public static PastDimensionsData fromDefaultFile(final MinecraftServer server) {
        if (server == null) return new PastDimensionsData();
        Path path = getDefaultFileLocation(server);
        if (Files.exists(path)) {
            try {
                CompoundTag fileData = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                return new PastDimensionsData(fileData.getCompoundOrEmpty("data"));
            } catch (Exception ex) {
                JCraft.LOGGER.warn("Failed reading PastDimensionsData", ex);
            }
        }
        return new PastDimensionsData();
    }
}
