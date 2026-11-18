package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class StandRotationPacket {
    private StandRotationPacket() {}

    public static FriendlyByteBuf write(double x, double y, double z, float yRot, float xRot) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yRot);
        buf.writeFloat(xRot);
        return buf;
    }

    public static void send(double x, double y, double z, float yRot, float xRot) {
        JNetwork.sendToServer(JPacketRegistry.C2S_STAND_ROTATION, write(x, y, z, yRot, xRot));
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.isAlive()) {
                return;
            }
            StandEntity<?, ?> stand = JUtils.getStand(player);
            if (stand != null && stand.isRemoteAndControllable()) {
                double maxRange = stand.getMaxRemoteRange();
                if (player.distanceToSqr(x, y, z) <= maxRange * maxRange) {
                    stand.setPos(x, y, z);
                } else {
                    Vec3 userPos = player.position();
                    Vec3 dir = new Vec3(x - userPos.x, y - userPos.y, z - userPos.z).normalize();
                    Vec3 clamped = userPos.add(dir.scale(maxRange));
                    stand.setPos(clamped.x, clamped.y, clamped.z);
                }

                stand.setYRot(yRot);
                stand.setXRot(xRot);
                stand.setYHeadRot(yRot);
                stand.yBodyRot = yRot;
                stand.xRotO = xRot;
                stand.yRotO = yRot;

                player.setYRot(yRot);
                player.setXRot(xRot);
                player.setYHeadRot(yRot);
                player.yBodyRot = yRot;
                player.xRotO = xRot;
                player.yRotO = yRot;
            }
        });
    }
}
