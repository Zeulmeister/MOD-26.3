package net.arna.jcraft.common.component.impl.living;

import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.misc.JByteBufUtils;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.registry.JAdvancementTriggerRegistry;
import net.arna.jcraft.common.stand.OriginProfile;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public abstract class CommonStandComponentImpl implements CommonStandComponent {
    private final Entity entity;
    private StandEntity<?, ?> stand;
    private StandType type;
    @Getter
    private int skin;
    @Getter
    private boolean tagged;
    private final EnumMap<MoveClass, String> originLoadout = new EnumMap<>(MoveClass.class);
    @Getter
    private int originLoadoutVersion = 0;
    @Getter
    private int originGlowRgb = -1;
    private final OriginProfile[] originProfiles = new OriginProfile[OriginProfile.MAX_PROFILES];

    protected CommonStandComponentImpl(final Entity entity) {
        this.entity = entity;
    }

    @Override
    public void setTypeAndSkin(final @Nullable StandType type, final int skin, final boolean loading) {
        // Exclusive stand check
        if (!entity.level().isClientSide() && entity instanceof Player &&
                !JCraft.getExclusiveStandsData().switchStand(this.type, type)) {
            return;
        }

        if (!StandTypeUtil.isNone(type) && entity instanceof ServerPlayer player) {
            if (!loading) {
                JUtils.maySendStandAboutInfo(player);
            }
            JAdvancementTriggerRegistry.OBTAINED_STAND.trigger(player, type);
        }
        this.type = type;
        this.skin = skin;
        sync(entity);
    }

    @Override
    public void setSkin(final int skin) {
        if (type == null) {
            return;
        }

        this.skin = Mth.clamp(skin, 0, type.getData().getInfo().getSkinCount() - 1);
        sync(entity);
    }

    @Override
    public void setStand(final @Nullable StandEntity<?, ?> stand) {
        // if (this.stand != null) this.stand.setUser(null);
        this.stand = stand;
        sync(entity);
    }

    @Nullable
    @Override
    public StandType getType() {
        if (type == null && stand != null) {
            // this.type = stand.getStandType();
            JCraft.LOGGER.warn("StandType of {} is null despite non-null stand {}", stand.getUser(), stand);
        }
        return this.type;
    }

    @Nullable
    @Override
    public StandEntity<?, ?> getStand() {
        if (stand != null && !stand.isAlive()) {
            setStand(null);
        }
        // Checks if the stand user has a passenger, and updates the stand if the passenger and stand do not match
        if (entity.getFirstPassenger() instanceof StandEntity<?, ?> passenger && stand != passenger) {
            setStand(passenger);
        }
        // Otherwise, returns the stored stand value
        return stand;
    }

    @Override
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
        sync(entity);
    }

    public void sync(Entity entity) {
    }

    @Override
    public Map<MoveClass, String> getOriginLoadout() {
        return Collections.unmodifiableMap(new EnumMap<>(originLoadout));
    }

    @Override
    public @Nullable String getOriginSlot(final MoveClass slot) {
        return slot == null ? null : originLoadout.get(slot);
    }

    @Override
    public void setOriginLoadout(final Map<MoveClass, String> loadout) {
        originLoadout.clear();
        if (loadout != null) {
            for (Map.Entry<MoveClass, String> e : loadout.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) {
                    originLoadout.put(e.getKey(), e.getValue());
                }
            }
        }
        originLoadoutVersion++;
        sync(entity);
    }

    @Override
    public void setOriginSlot(final MoveClass slot, final @Nullable String skillId) {
        if (slot == null) {
            return;
        }
        if (skillId == null || skillId.isEmpty()) {
            originLoadout.remove(slot);
        } else {
            originLoadout.put(slot, skillId);
        }
        originLoadoutVersion++;
        sync(entity);
    }

    @Override
    public void clearOriginLoadout() {
        originLoadout.clear();
        originLoadoutVersion++;
        sync(entity);
    }

    @Override
    public void setOriginGlowRgb(final int rgb) {
        int clean = rgb < 0 ? -1 : (rgb & 0xFFFFFF);
        if (this.originGlowRgb != clean) {
            this.originGlowRgb = clean;
            originLoadoutVersion++;
            sync(entity);
        }
    }

    @Override
    public @Nullable OriginProfile getOriginProfile(final int index) {
        if (index < 0 || index >= OriginProfile.MAX_PROFILES) {
            return null;
        }
        return originProfiles[index];
    }

    @Override
    public void setOriginProfile(final int index, final @Nullable OriginProfile profile) {
        if (index < 0 || index >= OriginProfile.MAX_PROFILES) {
            return;
        }
        originProfiles[index] = profile;
        originLoadoutVersion++;
        sync(entity);
    }

    @Override
    public void saveOriginProfile(final int index, final String name) {
        if (index < 0 || index >= OriginProfile.MAX_PROFILES) {
            return;
        }
        String cleanName = OriginProfile.sanitizeName(name, OriginProfile.defaultNameForSlot(index));
        originProfiles[index] = new OriginProfile(cleanName, originLoadout, skin, originGlowRgb);
        originLoadoutVersion++;
        sync(entity);
    }

    @Override
    public boolean loadOriginProfile(final int index) {
        OriginProfile profile = getOriginProfile(index);
        if (profile == null) {
            return false;
        }
        originLoadout.clear();
        originLoadout.putAll(profile.slots());
        this.originGlowRgb = profile.customGlowRgb();
        if (type != null) {
            this.skin = Mth.clamp(profile.skin(), 0, Math.max(0, type.getData().getInfo().getSkinCount() - 1));
        } else {
            this.skin = Math.max(0, profile.skin());
        }
        originLoadoutVersion++;
        sync(entity);
        return true;
    }

    @Override
    public void clearOriginProfile(final int index) {
        if (index < 0 || index >= OriginProfile.MAX_PROFILES) {
            return;
        }
        if (originProfiles[index] != null) {
            originProfiles[index] = null;
            originLoadoutVersion++;
            sync(entity);
        }
    }

    public void readFromNbt(final @NonNull CompoundTag tag) {
        type = StandTypeUtil.readFromNBT(tag, "Type");
        skin = tag.getIntOr("Skin", 0);
        tagged = tag.getBooleanOr("Tagged", false);
        originLoadout.clear();
        CompoundTag loadoutTag = tag.getCompoundOrEmpty("OriginLoadout");
        for (MoveClass mc : MoveClass.values()) {
            String skillId = loadoutTag.getStringOr(mc.name(), "");
            if (!skillId.isEmpty()) {
                originLoadout.put(mc, skillId);
            }
        }
        originGlowRgb = tag.getIntOr("OriginGlowRgb", -1);
        if (originGlowRgb >= 0) {
            originGlowRgb &= 0xFFFFFF;
        } else {
            originGlowRgb = -1;
        }
        Arrays.fill(originProfiles, null);
        CompoundTag profilesTag = tag.getCompoundOrEmpty("OriginProfiles");
        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            CompoundTag pTag = profilesTag.getCompoundOrEmpty(Integer.toString(i));
            if (!pTag.isEmpty()) {
                originProfiles[i] = OriginProfile.fromNbt(pTag);
            }
        }
        originLoadoutVersion++;
    }

    public void writeToNbt(final @NonNull CompoundTag tag) {
        tag.putString("Type", type == null ? "" : type.getId().toString());
        tag.putInt("Skin", skin);
        tag.putBoolean("Tagged", tagged);
        if (!originLoadout.isEmpty()) {
            CompoundTag loadoutTag = new CompoundTag();
            for (Map.Entry<MoveClass, String> e : originLoadout.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) {
                    loadoutTag.putString(e.getKey().name(), e.getValue());
                }
            }
            tag.put("OriginLoadout", loadoutTag);
        }
        if (originGlowRgb >= 0) {
            tag.putInt("OriginGlowRgb", originGlowRgb);
        }
        CompoundTag profilesTag = new CompoundTag();
        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            if (originProfiles[i] != null) {
                profilesTag.put(Integer.toString(i), originProfiles[i].toNbt());
            }
        }
        if (!profilesTag.isEmpty()) {
            tag.put("OriginProfiles", profilesTag);
        }
    }

    /**
     * Makes a certain entity be considered the component holders stand.
     */
    public void applySyncPacket(final FriendlyByteBuf buf) {
        Entity entity = buf.readBoolean() ? this.entity.level().getEntity(buf.readVarInt()) : null;
        if (entity == null || entity instanceof StandEntity<?, ?>) {
            stand = (StandEntity<?, ?>) entity;
        }
        int count = buf.readVarInt();
        originLoadout.clear();
        for (int i = 0; i < count; i++) {
            MoveClass mc = JByteBufUtils.readEnum(buf, MoveClass.class);
            String skillId = buf.readUtf(128);
            if (!skillId.isEmpty()) {
                originLoadout.put(mc, skillId);
            }
        }
        originGlowRgb = buf.readInt();
        if (originGlowRgb >= 0) {
            originGlowRgb &= 0xFFFFFF;
        } else {
            originGlowRgb = -1;
        }
        Arrays.fill(originProfiles, null);
        int profileCount = Math.min(buf.readVarInt(), OriginProfile.MAX_PROFILES);
        for (int i = 0; i < profileCount; i++) {
            int idx = buf.readVarInt();
            OriginProfile prof = OriginProfile.readBuf(buf);
            if (idx >= 0 && idx < OriginProfile.MAX_PROFILES) {
                originProfiles[idx] = prof;
            }
        }
        originLoadoutVersion++;
    }

    public void writeSyncPacket(final FriendlyByteBuf buf, final ServerPlayer recipient) {
        buf.writeBoolean(stand != null);
        if (stand != null) {
            buf.writeVarInt(stand.getId());
        }
        buf.writeVarInt(originLoadout.size());
        for (Map.Entry<MoveClass, String> e : originLoadout.entrySet()) {
            JByteBufUtils.writeEnum(buf, e.getKey());
            buf.writeUtf(e.getValue(), 128);
        }
        buf.writeInt(originGlowRgb);
        int nonNullCount = 0;
        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            if (originProfiles[i] != null) {
                nonNullCount++;
            }
        }
        buf.writeVarInt(nonNullCount);
        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            if (originProfiles[i] != null) {
                buf.writeVarInt(i);
                originProfiles[i].writeBuf(buf);
            }
        }
    }
}
