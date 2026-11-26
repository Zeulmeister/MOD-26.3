package net.arna.jcraft.common.stand;

import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.misc.JByteBufUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable saved loadout profile for the Origin Stand (4 slots per player).
 * Purely common data model (server + client safe).
 */
public record OriginProfile(
        String name,
        Map<MoveClass, String> slots,
        int skin,
        int customGlowRgb
) {
    public static final int MAX_PROFILES = 4;
    public static final List<String> DEFAULT_NAMES = List.of(
            "Boss Killer",
            "Time Control",
            "Mobility",
            "Sovereign"
    );

    public OriginProfile {
        name = sanitizeName(name, "Origin Build");
        EnumMap<MoveClass, String> copy = new EnumMap<>(MoveClass.class);
        if (slots != null) {
            for (Map.Entry<MoveClass, String> e : slots.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) {
                    copy.put(e.getKey(), e.getValue());
                }
            }
        }
        slots = Collections.unmodifiableMap(copy);
        skin = Math.max(0, skin);
        customGlowRgb = customGlowRgb < 0 ? -1 : (customGlowRgb & 0xFFFFFF);
    }

    public static String sanitizeName(@Nullable String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }

    public static String defaultNameForSlot(int index) {
        if (index >= 0 && index < DEFAULT_NAMES.size()) {
            return DEFAULT_NAMES.get(index);
        }
        return "Profile " + (index + 1);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putInt("Skin", skin);
        tag.putInt("GlowRgb", customGlowRgb);
        CompoundTag slotsTag = new CompoundTag();
        for (Map.Entry<MoveClass, String> e : slots.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) {
                slotsTag.putString(e.getKey().name(), e.getValue());
            }
        }
        tag.put("Slots", slotsTag);
        return tag;
    }

    public static @Nullable OriginProfile fromNbt(@Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        String name = tag.getStringOr("Name", "");
        int skin = tag.getIntOr("Skin", 0);
        int glowRgb = tag.getIntOr("GlowRgb", -1);
        EnumMap<MoveClass, String> slots = new EnumMap<>(MoveClass.class);
        CompoundTag slotsTag = tag.getCompoundOrEmpty("Slots");
        for (MoveClass mc : MoveClass.values()) {
            String skillId = slotsTag.getStringOr(mc.name(), "");
            if (!skillId.isEmpty()) {
                slots.put(mc, skillId);
            }
        }
        if (name.isEmpty() && slots.isEmpty() && glowRgb < 0 && skin == 0) {
            return null;
        }
        return new OriginProfile(name.isEmpty() ? "Origin Build" : name, slots, skin, glowRgb);
    }

    public void writeBuf(FriendlyByteBuf buf) {
        buf.writeUtf(name, 32);
        buf.writeVarInt(skin);
        buf.writeInt(customGlowRgb);
        buf.writeVarInt(slots.size());
        for (Map.Entry<MoveClass, String> e : slots.entrySet()) {
            JByteBufUtils.writeEnum(buf, e.getKey());
            buf.writeUtf(e.getValue(), 128);
        }
    }

    public static OriginProfile readBuf(FriendlyByteBuf buf) {
        String name = buf.readUtf(32);
        int skin = buf.readVarInt();
        int glowRgb = buf.readInt();
        int count = Math.min(buf.readVarInt(), MoveClass.values().length);
        EnumMap<MoveClass, String> slots = new EnumMap<>(MoveClass.class);
        for (int i = 0; i < count; i++) {
            MoveClass mc = JByteBufUtils.readEnum(buf, MoveClass.class);
            String id = buf.readUtf(128);
            if (!id.isEmpty()) {
                slots.put(mc, id);
            }
        }
        return new OriginProfile(name, slots, skin, glowRgb);
    }
}
