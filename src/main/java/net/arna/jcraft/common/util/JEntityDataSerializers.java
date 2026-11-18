package net.arna.jcraft.common.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

import java.util.Optional;
import java.util.UUID;

public final class JEntityDataSerializers {
    public static final EntityDataSerializer<Optional<UUID>> OPTIONAL_UUID =
            EntityDataSerializer.forValueType(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC));

    static {
        EntityDataSerializers.registerSerializer(OPTIONAL_UUID);
    }

    private JEntityDataSerializers() {}

    public static void init() {
        // Triggers static initializer
    }
}
