package net.arna.jcraft.common.component.impl.player;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JAdvancementTriggerRegistry;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.spec.SpecTypeUtil;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public abstract class CommonSpecComponentImpl implements CommonSpecComponent {
    protected final LivingEntity user;
    private SpecType type = JSpecTypeRegistry.NONE.get();
    private JSpec<?, ?> spec;

    public CommonSpecComponentImpl(final LivingEntity livingEntity) {
        this.user = livingEntity;
    }

    @Override
    public SpecType getType() {
        return type;
    }

    @Override
    public void setType(final SpecType type) {
        setTypeRaw(type, false);
        sync(user);
    }

    private void setTypeRaw(final SpecType rawType, final boolean loading) {
        final SpecType prevType = this.type;
        final SpecType normalized = rawType == null ? JSpecTypeRegistry.NONE.get() : rawType;
        if (!loading && prevType != normalized && user != null && !user.level().isClientSide()) {
            if (prevType == JSpecTypeRegistry.RANGER.get()) {
                net.arna.jcraft.platform.JComponentPlatformUtils.getGunslinger(user).setFocusActive(false);
            } else if (prevType == JSpecTypeRegistry.ANUBIS.get()) {
                net.arna.jcraft.platform.JComponentPlatformUtils.getMiscData(user).setAttackSpeedMult(1f);
            } else if (prevType == JSpecTypeRegistry.HAMON.get()) {
                var hamon = net.arna.jcraft.platform.JComponentPlatformUtils.getHamon(user);
                hamon.resetHamonCharge();
                hamon.setHamonizeReady(false);
            }
        }
        this.type = normalized;
        spec = SpecTypeUtil.isNone(normalized) ? null : normalized.createSpec(user);
        if (!SpecTypeUtil.isNone(normalized) && user instanceof ServerPlayer player) {
            if (!loading) {
                JUtils.maySendSpecAboutInfo(player);
            }
            JAdvancementTriggerRegistry.OBTAINED_SPEC.trigger(player, normalized);
        }
    }

    @Nullable
    @Override
    public JSpec<?, ?> getSpec() {
        return spec;
    }

    public void sync(final Entity entity) {
    }

    public void readFromNbt(final @NonNull CompoundTag tag) {
        SpecType type = SpecTypeUtil.readFromNBT(tag, "Type");
        setTypeRaw(type, true);
    }

    public void writeToNbt(final @NonNull CompoundTag tag) {
        if (type != null) {
            tag.putString("Type", type.getId().toString());
        }
    }

    public boolean shouldSyncWith(final ServerPlayer player) {
        return player == this.user; // Only our player needs to know.
    }
}
