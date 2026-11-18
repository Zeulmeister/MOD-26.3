package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class RemoteControlTogglePacket {
    private RemoteControlTogglePacket() {}

    public static FriendlyByteBuf write() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static void send() {
        JNetwork.sendToServer(JPacketRegistry.C2S_REMOTE_CONTROL_TOGGLE, write());
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.isAlive()) {
                return;
            }
            toggleForPlayer(player);
        });
    }

    public static void toggleForPlayer(ServerPlayer player) {
        StandEntity<?, ?> stand = JUtils.getStand(player);
        if (stand != null && stand.isAlive() && stand.remoteControllable()) {
            player.resetFallDistance();
            stand.resetFallDistance();
            stand.togglePilotMode();
            player.resetFallDistance();
        }
    }
}
