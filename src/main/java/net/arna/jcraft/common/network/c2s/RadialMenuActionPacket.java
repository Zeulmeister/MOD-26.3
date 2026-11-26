package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Handles server-side actions triggered from the 8-slice JoJo Interactive Radial Wheel (Skin 0-3, Pose Taunt, Pilot Mode).
 */
public final class RadialMenuActionPacket {
    public static final int ACTION_SKIN_0 = 0;
    public static final int ACTION_SKIN_1 = 1;
    public static final int ACTION_SKIN_2 = 2;
    public static final int ACTION_SKIN_3 = 3;
    public static final int ACTION_POSE_TAUNT = 4;
    public static final int ACTION_TOGGLE_PILOT = 5;
    public static final int ACTION_SKIN_BASE = 100;

    private RadialMenuActionPacket() {}

    public static void send(int actionId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(actionId);
        JNetwork.sendToServer(JPacketRegistry.C2S_RADIAL_ACTION, buf);
    }

    public static void sendSkin(int skinIndex) {
        send(ACTION_SKIN_BASE + Math.max(0, skinIndex));
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        int actionId = buf.readVarInt();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.isAlive()) {
                return;
            }
            if ((actionId >= ACTION_SKIN_0 && actionId <= ACTION_SKIN_3) || actionId >= ACTION_SKIN_BASE) {
                int requestedSkin = actionId >= ACTION_SKIN_BASE ? (actionId - ACTION_SKIN_BASE) : actionId;
                CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
                StandType type = standComp.getType();
                if (StandTypeUtil.isNone(type) || type == null) {
                    return;
                }
                var info = type.getData().getInfo();
                int maxSkins = Math.max(1, info.getSkinCount());
                int targetSkin = Math.max(0, Math.min(requestedSkin, maxSkins - 1));
                standComp.setSkin(targetSkin);
                StandEntity<?, ?> stand = JUtils.getStand(player);
                if (stand != null) {
                    stand.setSkin(targetSkin);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 0.8F, 1.25F);
                Component skinLabel = (targetSkin == 0 || targetSkin - 1 >= info.getSkinNames().size())
                        ? Component.literal("Default")
                        : info.getSkinNames().get(targetSkin - 1);
                player.sendOverlayMessage(Component.translatable("jcraft.radial.skin_applied", skinLabel));
                return;
            }
            switch (actionId) {
                case ACTION_POSE_TAUNT -> PoseTauntPacket.executeTaunt(player);
                case ACTION_TOGGLE_PILOT -> RemoteControlTogglePacket.toggleForPlayer(player);
                default -> {}
            }
        });
    }
}
