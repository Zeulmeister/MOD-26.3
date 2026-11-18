package net.arna.jcraft.common.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge between Minecraft 26.3 ValueInput/ValueOutput, UUID NBT helpers, and CompoundTag persistence.
 */
public final class JValueIO {
    private JValueIO() {}

    public static CompoundTag read(ValueInput input, String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    public static void write(ValueOutput output, String key, CompoundTag tag) {
        if (tag != null && !tag.isEmpty()) {
            output.store(key, CompoundTag.CODEC, tag);
        }
    }

    public static void putUUID(CompoundTag tag, String key, @Nullable UUID uuid) {
        if (tag == null || uuid == null) return;
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
    }

    @Nullable
    public static UUID getUUID(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key)) return null;
        Optional<int[]> arr = tag.getIntArray(key);
        if (arr.isPresent() && arr.get().length == 4) {
            return UUIDUtil.uuidFromIntArray(arr.get());
        }
        String s = tag.getStringOr(key, "");
        if (!s.isEmpty()) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key)) return false;
        Optional<int[]> arr = tag.getIntArray(key);
        if (arr.isPresent() && arr.get().length == 4) return true;
        return !tag.getStringOr(key, "").isEmpty();
    }
}
