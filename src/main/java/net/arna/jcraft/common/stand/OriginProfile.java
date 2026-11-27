package net.arna.jcraft.common.stand;

import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.misc.JByteBufUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
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
    public static final int MAX_NAME_LENGTH = 20;
    public static final String BUILD_CODE_PREFIX = "ORIGIN1:";
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
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
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

    /**
     * Encodes this profile (skin, Chroma RGB, and all 9 MoveClass slots) into a compact clipboard build code.
     */
    public static String encodeBuildCode(final OriginProfile profile) {
        StringBuilder slotCsv = new StringBuilder();
        MoveClass[] allSlots = MoveClass.values();
        for (int i = 0; i < allSlots.length; i++) {
            if (i > 0) {
                slotCsv.append(',');
            }
            String id = profile.slots().getOrDefault(allSlots[i], "");
            if (id.startsWith("jcraft:")) {
                id = id.substring("jcraft:".length());
            }
            slotCsv.append(id);
        }
        String glowStr = profile.customGlowRgb() < 0
                ? "-"
                : String.format(Locale.ROOT, "%06X", profile.customGlowRgb());
        String payload = profile.skin() + "|" + glowStr + "|" + slotCsv;
        return BUILD_CODE_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a build code produced by {@link #encodeBuildCode(OriginProfile)}, returning {@code null} if malformed.
     */
    public static @Nullable OriginProfile decodeBuildCode(final @Nullable String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String trimmed = rawCode.trim();
        if (!trimmed.startsWith(BUILD_CODE_PREFIX) || trimmed.length() > 1024) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(trimmed.substring(BUILD_CODE_PREFIX.length()));
            String payload = new String(bytes, StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3) {
                return null;
            }
            int skin = Math.max(0, Math.min(15, Integer.parseInt(parts[0])));
            int glowRgb = "-".equals(parts[1]) ? -1 : (Integer.parseInt(parts[1], 16) & 0xFFFFFF);
            String[] slotParts = parts[2].split(",", -1);
            MoveClass[] allSlots = MoveClass.values();
            if (slotParts.length != allSlots.length) {
                return null;
            }
            EnumMap<MoveClass, String> map = new EnumMap<>(MoveClass.class);
            for (int i = 0; i < allSlots.length; i++) {
                String id = slotParts[i].trim();
                if (id.length() > 128) {
                    return null;
                }
                if (!id.isEmpty()) {
                    if (!id.contains(":")) {
                        id = "jcraft:" + id;
                    }
                    map.put(allSlots[i], id);
                }
            }
            if (map.isEmpty()) {
                return null;
            }
            return new OriginProfile("Imported Build", map, skin, glowRgb);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
