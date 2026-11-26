package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.stand.OriginProfile;
import net.arna.jcraft.common.stand.OriginSkillCatalog;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles Origin custom skill bind, Chroma RGB, and loadout profile updates from the Origin Arsenal client screen.
 */
public final class OriginLoadoutPacket {
    public static final int ACTION_SET_SLOT = 0;
    public static final int ACTION_APPLY_PRESET = 1;
    public static final int ACTION_RESET_DEFAULT = 2;
    public static final int ACTION_SYNC_ALL = 3;
    public static final int ACTION_SET_CHROMA = 4;
    public static final int ACTION_SAVE_PROFILE = 5;
    public static final int ACTION_LOAD_PROFILE = 6;
    public static final int ACTION_CLEAR_PROFILE = 7;

    private OriginLoadoutPacket() {}

    public static void sendSetSlot(MoveClass slot, String skillId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_SET_SLOT);
        net.arna.jcraft.api.misc.JByteBufUtils.writeEnum(buf, slot);
        buf.writeUtf(skillId == null ? "" : skillId, 128);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendApplyPreset(String presetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_APPLY_PRESET);
        buf.writeUtf(presetId == null ? "" : presetId, 64);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendResetDefault() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_RESET_DEFAULT);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendSyncAll(Map<MoveClass, String> loadout) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_SYNC_ALL);
        buf.writeVarInt(loadout.size());
        for (Map.Entry<MoveClass, String> entry : loadout.entrySet()) {
            net.arna.jcraft.api.misc.JByteBufUtils.writeEnum(buf, entry.getKey());
            buf.writeUtf(entry.getValue() == null ? "" : entry.getValue(), 128);
        }
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendSetChroma(int rgb) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_SET_CHROMA);
        buf.writeInt(rgb < 0 ? -1 : (rgb & 0xFFFFFF));
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendSaveProfile(int slotIdx, String name) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_SAVE_PROFILE);
        buf.writeVarInt(slotIdx);
        buf.writeUtf(OriginProfile.sanitizeName(name, OriginProfile.defaultNameForSlot(slotIdx)), 32);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendLoadProfile(int slotIdx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_LOAD_PROFILE);
        buf.writeVarInt(slotIdx);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendClearProfile(int slotIdx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_CLEAR_PROFILE);
        buf.writeVarInt(slotIdx);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int action = buf.readVarInt();
        MoveClass targetSlot = action == ACTION_SET_SLOT
                ? net.arna.jcraft.api.misc.JByteBufUtils.readEnum(buf, MoveClass.class)
                : null;
        String argStr = (action == ACTION_SET_SLOT || action == ACTION_APPLY_PRESET) ? buf.readUtf(128) : null;
        int chromaRgb = action == ACTION_SET_CHROMA ? buf.readInt() : -1;
        int profileIdx = (action == ACTION_SAVE_PROFILE || action == ACTION_LOAD_PROFILE || action == ACTION_CLEAR_PROFILE)
                ? buf.readVarInt()
                : -1;
        String profileName = action == ACTION_SAVE_PROFILE ? buf.readUtf(32) : null;

        Map<MoveClass, String> fullMap = null;
        if (action == ACTION_SYNC_ALL) {
            int count = Math.min(buf.readVarInt(), MoveClass.values().length);
            fullMap = new EnumMap<>(MoveClass.class);
            for (int i = 0; i < count; i++) {
                MoveClass slot = net.arna.jcraft.api.misc.JByteBufUtils.readEnum(buf, MoveClass.class);
                String skillId = buf.readUtf(128);
                fullMap.put(slot, skillId);
            }
        }

        final Map<MoveClass, String> capturedMap = fullMap;
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.isAlive()) {
                return;
            }
            CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
            if (standComp.getType() != JStandTypeRegistry.ORIGIN.get()) {
                return;
            }

            boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
            boolean changed = false;
            Component notify = null;

            switch (action) {
                case ACTION_SET_SLOT -> {
                    if (targetSlot != null) {
                        OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(targetSlot, argStr, freeBinds);
                        standComp.setOriginSlot(targetSlot, resolved.id());
                        changed = true;
                        notify = Component.translatable(
                                "jcraft.origin.arsenal.slot_bound",
                                targetSlot.getFriendlyName(),
                                resolved.templateMove().getName()
                        );
                    }
                }
                case ACTION_APPLY_PRESET -> {
                    OriginSkillCatalog.LoadoutPreset preset = OriginSkillCatalog.getPresetById(argStr);
                    if (preset != null) {
                        EnumMap<MoveClass, String> sanitized = new EnumMap<>(MoveClass.class);
                        for (MoveClass slot : MoveClass.values()) {
                            String rawId = preset.slots().getOrDefault(slot, OriginSkillCatalog.getDefaultId(slot));
                            OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, rawId, freeBinds);
                            sanitized.put(slot, resolved.id());
                        }
                        standComp.setOriginLoadout(sanitized);
                        changed = true;
                        notify = Component.translatable(
                                "jcraft.origin.arsenal.preset_applied",
                                Component.translatable(preset.titleKey())
                        );
                    }
                }
                case ACTION_RESET_DEFAULT -> {
                    standComp.clearOriginLoadout();
                    changed = true;
                    notify = Component.translatable("jcraft.origin.arsenal.reset_done");
                }
                case ACTION_SYNC_ALL -> {
                    if (capturedMap != null) {
                        EnumMap<MoveClass, String> sanitized = new EnumMap<>(MoveClass.class);
                        for (MoveClass slot : MoveClass.values()) {
                            String rawId = capturedMap.getOrDefault(slot, OriginSkillCatalog.getDefaultId(slot));
                            OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, rawId, freeBinds);
                            sanitized.put(slot, resolved.id());
                        }
                        standComp.setOriginLoadout(sanitized);
                        changed = true;
                    }
                }
                case ACTION_SET_CHROMA -> {
                    int cleanRgb = chromaRgb < 0 ? -1 : (chromaRgb & 0xFFFFFF);
                    standComp.setOriginGlowRgb(cleanRgb);
                    changed = true;
                    if (cleanRgb < 0) {
                        notify = Component.translatable("jcraft.origin.chroma.reset_done");
                    } else {
                        notify = Component.translatable("jcraft.origin.chroma.applied",
                                String.format(Locale.ROOT, "#%06X", cleanRgb));
                    }
                }
                case ACTION_SAVE_PROFILE -> {
                    if (profileIdx >= 0 && profileIdx < OriginProfile.MAX_PROFILES) {
                        String cleanName = OriginProfile.sanitizeName(profileName, OriginProfile.defaultNameForSlot(profileIdx));
                        standComp.saveOriginProfile(profileIdx, cleanName);
                        changed = true;
                        notify = Component.translatable("jcraft.origin.profile.saved", cleanName);
                    }
                }
                case ACTION_LOAD_PROFILE -> {
                    if (profileIdx >= 0 && profileIdx < OriginProfile.MAX_PROFILES) {
                        OriginProfile prof = standComp.getOriginProfile(profileIdx);
                        if (prof != null) {
                            EnumMap<MoveClass, String> sanitized = new EnumMap<>(MoveClass.class);
                            for (MoveClass slot : MoveClass.values()) {
                                String rawId = prof.slots().getOrDefault(slot, OriginSkillCatalog.getDefaultId(slot));
                                OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, rawId, freeBinds);
                                sanitized.put(slot, resolved.id());
                            }
                            standComp.setOriginLoadout(sanitized);
                            standComp.setOriginGlowRgb(prof.customGlowRgb());
                            standComp.setSkin(prof.skin());
                            if (JUtils.getStand(player) instanceof OriginEntity origin) {
                                origin.setSkin(standComp.getSkin());
                            }
                            changed = true;
                            notify = Component.translatable("jcraft.origin.profile.loaded", prof.name());
                        }
                    }
                }
                case ACTION_CLEAR_PROFILE -> {
                    if (profileIdx >= 0 && profileIdx < OriginProfile.MAX_PROFILES) {
                        standComp.clearOriginProfile(profileIdx);
                        changed = true;
                        notify = Component.translatable("jcraft.origin.profile.cleared", profileIdx + 1);
                    }
                }
                default -> {}
            }

            if (changed) {
                if (JUtils.getStand(player) instanceof OriginEntity origin) {
                    origin.rebuildLoadoutFromUser(standComp);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        JSoundRegistry.ORIGIN_BIND.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
                if (notify != null) {
                    player.sendOverlayMessage(notify);
                }
            }
        });
    }
}
