package net.arna.jcraft.client.util;

import com.mojang.authlib.GameProfile;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import dev.architectury.event.events.client.ClientTickEvent;
import net.arna.jcraft.client.rendering.CloneSkinTracker;
import net.arna.jcraft.common.entity.PlayerCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerCloneClientPlayerEntity extends AbstractClientPlayer {
    private static final Set<PlayerCloneClientPlayerEntity> entities = Collections.newSetFromMap(new WeakHashMap<>());
    private static final GameProfile CLONE_PROFILE = new GameProfile(UUID.nameUUIDFromBytes("jcraft$playerClone".getBytes()), "PlayerClone");
    private final PlayerCloneEntity clone;
    private final AvatarAnimManager animManager = new AvatarAnimManager(this);

    static {
        ClientTickEvent.CLIENT_LEVEL_POST.register(world -> entities.stream()
                .filter(LivingEntity::isAlive)
                .forEach(PlayerCloneClientPlayerEntity::tick));
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.level == null) {
                entities.clear();
            }
        });
    }

    public PlayerCloneClientPlayerEntity(final PlayerCloneEntity clone) {
        super(Objects.requireNonNull(Minecraft.getInstance().level), CLONE_PROFILE);
        this.clone = clone;
        entities.add(this);
    }

    @Override
    public AvatarAnimManager playerAnimLib$getAnimManager() {
        return animManager;
    }

    @Override
    public @Nullable IAnimation playerAnimLib$getAnimation(Identifier id) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(this, id);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Nullable
    @Override
    protected PlayerInfo getPlayerInfo() {
        return null;
    }

    @Override
    public @NotNull PlayerSkin getSkin() {
        PlayerSkin tracked = CloneSkinTracker.getPlayerSkinFor(clone);
        return tracked != null ? tracked : DefaultPlayerSkin.get(clone.getMasterId() != null ? clone.getMasterId() : getUUID());
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
        setMainArm(clone.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, clone.getPartMask());
        setArrowCount(clone.getArrowCount());
        setStingerCount(clone.getStingerCount());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, clone.getItemBySlot(slot));
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        return false;
    }

    public void updateData() {
        xo = clone.xo;
        yo = clone.yo;
        zo = clone.zo;
        setPosRaw(clone.getX(), clone.getY(), clone.getZ());
        yBodyRotO = clone.yBodyRotO;
        yBodyRot = clone.yBodyRot;
        yHeadRotO = clone.yHeadRotO;
        yHeadRot = clone.yHeadRot;
        hurtTime = clone.hurtTime;
        hurtDuration = clone.hurtDuration;
        deathTime = clone.deathTime;
        dead = clone.isDeadOrDying();
    }
}
