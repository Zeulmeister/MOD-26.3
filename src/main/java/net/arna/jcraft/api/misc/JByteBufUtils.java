package net.arna.jcraft.api.misc;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Wire-compatible, bounds-safe enum serialization helpers replacing deprecated
 * {@link FriendlyByteBuf#writeEnum(Enum)} and {@link FriendlyByteBuf#readEnum(Class)}.
 */
public final class JByteBufUtils {
    private JByteBufUtils() {}

    public static FriendlyByteBuf writeEnum(final FriendlyByteBuf buf, final Enum<?> value) {
        return buf.writeVarInt(value.ordinal());
    }

    public static <T extends Enum<T>> T readEnum(final FriendlyByteBuf buf, final Class<T> enumClass) {
        final T[] constants = enumClass.getEnumConstants();
        final int ordinal = buf.readVarInt();
        if (ordinal < 0 || ordinal >= constants.length) {
            return constants[0];
        }
        return constants[ordinal];
    }
}
