package net.arna.jcraft.client.rendering;

import com.mojang.authlib.GameProfile;
import net.arna.jcraft.common.entity.PlayerCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CloneSkinTracker {
    @Nullable
    public static PlayerSkin getPlayerSkinFor(final PlayerCloneEntity clone) {
        UUID masterId = clone.getMasterId();
        if (masterId == null) {
            return null;
        }
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            PlayerInfo info = conn.getPlayerInfo(masterId);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(masterId);
    }

    @Nullable
    public static Identifier getSkinLocationFor(final PlayerCloneEntity clone) {
        PlayerSkin skin = getPlayerSkinFor(clone);
        return skin != null ? skin.body().texturePath() : null;
    }
}
