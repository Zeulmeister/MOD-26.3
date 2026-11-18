package net.arna.jcraft.common.network.s2c;

import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.util.JExplosionModifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class JExplosionPacket {
    public static void send(ServerPlayer player, double x, double y, double z, float power, Explosion explosion, @Nullable JExplosionModifier modifier) {
        net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(power);
        Vec3 kb = explosion instanceof ServerExplosion se ? se.getHitPlayers().get(player) : null;
        buf.writeBoolean(kb != null);
        if (kb != null) {
            buf.writeDouble(kb.x);
            buf.writeDouble(kb.y);
            buf.writeDouble(kb.z);
        }
        if (modifier != null) {
            modifier.write(buf, player.getRandom());
        } else {
            JExplosionModifier.builder().build().write(buf, player.getRandom());
        }
        JNetwork.sendToPlayer(player, JPacketRegistry.S2C_J_EXPLOSION, buf);
    }
}
