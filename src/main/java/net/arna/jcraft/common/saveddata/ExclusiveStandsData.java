package net.arna.jcraft.common.saveddata;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.common.config.JServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ExclusiveStandsData extends SavedData {
    public static final Path DEFAULT_FILE_LOCATION = Path.of("data", "jcraft_exclusiveStands.dat");
    protected static final String KEYWORD = "stands";
    private final Set<Identifier> usedStands = new HashSet<>();

    public ExclusiveStandsData() {}

    public ExclusiveStandsData(final CompoundTag compoundTag) {
        if (compoundTag == null) return;
        ListTag list = compoundTag.getListOrEmpty(KEYWORD);
        for (Tag t : list) {
            t.asString().map(Identifier::tryParse).ifPresent(usedStands::add);
        }
    }

    public boolean isStandUsed(final @NonNull StandType standType) {
        return JServerConfig.EXCLUSIVE_STANDS.getValue() && usedStands.contains(standType.getId());
    }

    public boolean switchStand(final @Nullable StandType prev, final @Nullable StandType curr) {
        if (!JServerConfig.EXCLUSIVE_STANDS.getValue()) return true;
        if (curr != null && isStandUsed(curr)) return false;
        if (prev != null) usedStands.remove(prev.getId());
        if (curr != null) usedStands.add(curr.getId());
        setDirty();
        return true;
    }

    public static File getDefaultFileLocation(final MinecraftServer server) {
        return server.storageSource.levelDirectory.path().resolve(DEFAULT_FILE_LOCATION).toFile();
    }

    public CompoundTag toTag() {
        CompoundTag compoundTag = new CompoundTag();
        ListTag tag = new ListTag();
        for (Identifier id : usedStands) {
            tag.add(StringTag.valueOf(id.toString()));
        }
        compoundTag.put(KEYWORD, tag);
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
            JCraft.LOGGER.warn("Failed saving ExclusiveStandsData", e);
        }
    }

    public static ExclusiveStandsData fromFile(final File file) {
        Path path = file.toPath();
        if (Files.exists(path)) {
            try {
                CompoundTag fileData = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                return new ExclusiveStandsData(fileData.getCompoundOrEmpty("data"));
            } catch (Exception ex) {
                JCraft.LOGGER.warn("Failed reading ExclusiveStandsData", ex);
            }
        }
        return new ExclusiveStandsData();
    }

    public static ExclusiveStandsData fromDefaultFile(final MinecraftServer server) {
        return fromFile(getDefaultFileLocation(server));
    }
}
