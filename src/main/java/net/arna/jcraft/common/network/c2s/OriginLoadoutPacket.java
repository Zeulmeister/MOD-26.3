package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.stand.OriginProfile;
import net.arna.jcraft.common.stand.OriginSkillCatalog;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles Origin custom skill bind, Chroma RGB, and loadout profile updates from the Origin Arsenal client screen,
 * including anti-exploit combat/cooldown locks, duplicate prevention, Ultimate caps, and build-code imports.
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
    public static final int ACTION_RENAME_PROFILE = 8;
    public static final int ACTION_IMPORT_BUILD = 9;

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

    public static void sendRenameProfile(int slotIdx, String name) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_RENAME_PROFILE);
        buf.writeVarInt(slotIdx);
        buf.writeUtf(OriginProfile.sanitizeName(name, OriginProfile.defaultNameForSlot(slotIdx)), 32);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    public static void sendImportBuild(OriginProfile profile) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ACTION_IMPORT_BUILD);
        profile.writeBuf(buf);
        JNetwork.sendToServer(JPacketRegistry.C2S_ORIGIN_LOADOUT, buf);
    }

    /**
     * Checks whether gameplay-affecting loadout swaps are locked in PvP/Survival (Creative bypasses locks).
     */
    public static @Nullable Component getLoadoutLockReason(final ServerPlayer player) {
        if (player.isCreative()) {
            return null;
        }
        if (JCraft.wasRecentlyAttacked(player.getCombatTracker())) {
            return Component.translatable("jcraft.origin.arsenal.locked_combat");
        }
        if (JUtils.getStand(player) instanceof OriginEntity origin
                && (origin.getMoveStun() > 0 || origin.getTETime() > 0 || origin.getAccelTime() > 0 || origin.getFlightTime() > 0)) {
            return Component.translatable("jcraft.origin.arsenal.locked_busy");
        }
        CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(player);
        for (CooldownType ct : CooldownType.values()) {
            if ((ct.getCategory() == CooldownType.Category.STAND || ct == CooldownType.UTILITY)
                    && cooldowns.getCooldown(ct) > 0) {
                return Component.translatable("jcraft.origin.arsenal.locked_cooldown");
            }
        }
        return null;
    }

    private static EnumMap<MoveClass, String> readCurrentRawSlots(CommonStandComponent standComp) {
        EnumMap<MoveClass, String> map = new EnumMap<>(MoveClass.class);
        for (MoveClass slot : MoveClass.values()) {
            String existing = standComp.getOriginSlot(slot);
            map.put(slot, existing != null && !existing.isEmpty() ? existing : OriginSkillCatalog.getDefaultId(slot));
        }
        return map;
    }

    private static EnumMap<MoveClass, String> toSanitizedIdMap(
            Map<MoveClass, String> rawSlots,
            boolean freeBinds,
            int maxUltimates) {
        EnumMap<MoveClass, OriginSkillCatalog.SkillEntry> sanitized = OriginSkillCatalog.sanitizeLoadout(rawSlots, freeBinds, maxUltimates);
        EnumMap<MoveClass, String> out = new EnumMap<>(MoveClass.class);
        for (Map.Entry<MoveClass, OriginSkillCatalog.SkillEntry> e : sanitized.entrySet()) {
            out.put(e.getKey(), e.getValue().id());
        }
        return out;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int action = buf.readVarInt();
        MoveClass targetSlot = action == ACTION_SET_SLOT
                ? net.arna.jcraft.api.misc.JByteBufUtils.readEnum(buf, MoveClass.class)
                : null;
        String argStr = (action == ACTION_SET_SLOT || action == ACTION_APPLY_PRESET) ? buf.readUtf(128) : null;
        int chromaRgb = action == ACTION_SET_CHROMA ? buf.readInt() : -1;
        int profileIdx = (action == ACTION_SAVE_PROFILE || action == ACTION_LOAD_PROFILE
                || action == ACTION_CLEAR_PROFILE || action == ACTION_RENAME_PROFILE)
                ? buf.readVarInt()
                : -1;
        String profileName = (action == ACTION_SAVE_PROFILE || action == ACTION_RENAME_PROFILE) ? buf.readUtf(32) : null;
        OriginProfile importedBuild = action == ACTION_IMPORT_BUILD ? OriginProfile.readBuf(buf) : null;

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

            boolean affectsCombatLoadout = action == ACTION_SET_SLOT
                    || action == ACTION_APPLY_PRESET
                    || action == ACTION_RESET_DEFAULT
                    || action == ACTION_SYNC_ALL
                    || action == ACTION_LOAD_PROFILE
                    || action == ACTION_IMPORT_BUILD;
            if (affectsCombatLoadout) {
                Component lockReason = getLoadoutLockReason(player);
                if (lockReason != null) {
                    player.sendOverlayMessage(lockReason);
                    standComp.sync(player);
                    return;
                }
            }

            boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
            int maxUltimates = JServerConfig.ORIGIN_MAX_ULTIMATES.getValue();
            boolean changed = false;
            Component notify = null;

            switch (action) {
                case ACTION_SET_SLOT -> {
                    if (targetSlot != null) {
                        OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(targetSlot, argStr, freeBinds);
                        if (resolved != null) {
                            EnumMap<MoveClass, String> currentSlots = readCurrentRawSlots(standComp);
                            MoveClass prevOwner = OriginSkillCatalog.findSlotContaining(currentSlots, targetSlot, resolved.id());
                            if (prevOwner != null) {
                                String displacedId = currentSlots.getOrDefault(targetSlot, OriginSkillCatalog.getDefaultId(targetSlot));
                                OriginSkillCatalog.SkillEntry displacedEntry = OriginSkillCatalog.resolveForSlot(prevOwner, displacedId, freeBinds);
                                String replacementForPrev = (displacedEntry != null && !displacedEntry.id().equals(resolved.id()))
                                        ? displacedEntry.id()
                                        : OriginSkillCatalog.getDefaultId(prevOwner);
                                currentSlots.put(prevOwner, replacementForPrev);
                                currentSlots.put(targetSlot, resolved.id());
                                standComp.setOriginLoadout(toSanitizedIdMap(currentSlots, freeBinds, maxUltimates));
                            } else {
                                if (maxUltimates > 0 && OriginSkillCatalog.isUltimateSkill(resolved)
                                        && OriginSkillCatalog.countUltimatesExcluding(currentSlots, targetSlot, freeBinds) >= maxUltimates) {
                                    player.sendOverlayMessage(Component.translatable("jcraft.origin.arsenal.max_ultimates", maxUltimates));
                                    standComp.sync(player);
                                    return;
                                }
                                standComp.setOriginSlot(targetSlot, resolved.id());
                            }
                            changed = true;
                            notify = Component.translatable(
                                    "jcraft.origin.arsenal.slot_bound",
                                    targetSlot.getFriendlyName(),
                                    resolved.templateMove().getName()
                            );
                        }
                    }
                }
                case ACTION_APPLY_PRESET -> {
                    OriginSkillCatalog.LoadoutPreset preset = OriginSkillCatalog.getPresetById(argStr);
                    if (preset != null) {
                        standComp.setOriginLoadout(toSanitizedIdMap(preset.slots(), freeBinds, maxUltimates));
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
                        standComp.setOriginLoadout(toSanitizedIdMap(capturedMap, freeBinds, maxUltimates));
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
                            standComp.setOriginLoadout(toSanitizedIdMap(prof.slots(), freeBinds, maxUltimates));
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
                case ACTION_RENAME_PROFILE -> {
                    if (profileIdx >= 0 && profileIdx < OriginProfile.MAX_PROFILES) {
                        OriginProfile existing = standComp.getOriginProfile(profileIdx);
                        if (existing != null) {
                            String cleanName = OriginProfile.sanitizeName(profileName, OriginProfile.defaultNameForSlot(profileIdx));
                            standComp.setOriginProfile(profileIdx, new OriginProfile(
                                    cleanName, existing.slots(), existing.skin(), existing.customGlowRgb()));
                            changed = true;
                            notify = Component.translatable("jcraft.origin.profile.renamed", cleanName);
                        }
                    }
                }
                case ACTION_IMPORT_BUILD -> {
                    if (importedBuild != null) {
                        standComp.setOriginLoadout(toSanitizedIdMap(importedBuild.slots(), freeBinds, maxUltimates));
                        int skinCount = JStandTypeRegistry.ORIGIN.get().getData().getInfo().getSkinCount();
                        int safeSkin = Mth.clamp(importedBuild.skin(), 0, Math.max(0, skinCount - 1));
                        standComp.setOriginGlowRgb(importedBuild.customGlowRgb());
                        standComp.setSkin(safeSkin);
                        if (JUtils.getStand(player) instanceof OriginEntity origin) {
                            origin.setSkin(safeSkin);
                        }
                        changed = true;
                        notify = Component.translatable("jcraft.origin.profile.code_imported");
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