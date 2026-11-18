package net.arna.jcraft.common.compat;

import lombok.NonNull;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Safe chunk protection hook for Minecraft 26.3.
 */
public class FtbChunksCompat {
    private static final Itf INSTANCE = (player, level, pos) -> true;

    public static Itf get() {
        return INSTANCE;
    }

    public interface Itf {
        boolean mayEdit(ServerPlayer player, ServerLevel level, @NonNull BlockPos pos);
    }
}
