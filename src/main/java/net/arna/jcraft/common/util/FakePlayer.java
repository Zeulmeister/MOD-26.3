package net.arna.jcraft.common.util;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

public class FakePlayer extends ServerPlayer {
    private static final GameProfile FAKE_PROFILE = new GameProfile(UUID.nameUUIDFromBytes("jcraft".getBytes()), "[JCraft]");

    public FakePlayer(ServerLevel world) {
        super(world.getServer(), world, FAKE_PROFILE, ClientInformation.createDefault());
    }

    @Override
    public void tick() {
    }

    @Override
    public void awardStat(@NotNull Stat<?> stat, int incrementer) {
    }

    @Override
    public void awardStat(@NotNull Stat<?> stat) {
    }

    @Override
    public void sendSystemMessage(@NotNull Component message) {
    }

    @Override
    public void sendOverlayMessage(@NotNull Component message) {
    }

    @Override
    public boolean isInvulnerableTo(@NotNull ServerLevel level, @NotNull DamageSource damageSource) {
        return true;
    }
}
