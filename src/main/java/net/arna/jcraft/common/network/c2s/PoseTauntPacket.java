package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PoseTauntPacket {
    private static final Map<UUID, Long> LAST_TAUNT_TIMES = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 15_000L;

    private PoseTauntPacket() {}

    public static void send() {
        JNetwork.sendToServer(JPacketRegistry.C2S_POSE_TAUNT, new FriendlyByteBuf(Unpooled.buffer()));
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.isAlive()) {
                return;
            }
            executeTaunt(player);
        });
    }

    public static boolean executeTaunt(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long last = LAST_TAUNT_TIMES.getOrDefault(player.getUUID(), 0L);
        if (now - last < COOLDOWN_MS) {
            return false;
        }
        LAST_TAUNT_TIMES.put(player.getUUID(), now);

        ServerLevel level = player.level();
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, true, true));

        try {
            JComponentPlatformUtils.getCooldowns(player).cooldownCancel();
        } catch (Throwable ignored) {}

        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();
        level.sendParticles(JParticleTypeRegistry.GO.get(), px, py, pz, 14, 0.6, 0.8, 0.6, 0.05);
        level.sendParticles(JParticleTypeRegistry.DO.get(), px, py + 0.3, pz, 8, 0.5, 0.6, 0.5, 0.04);
        level.sendParticles(JParticleTypeRegistry.AURA_ARC.get(), px, py, pz, 10, 0.4, 0.6, 0.4, 0.08);
        level.playSound(null, px, py, pz, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85F, 1.4F);
        level.playSound(null, px, py, pz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);

        FriendlyByteBuf outBuf = new FriendlyByteBuf(Unpooled.buffer());
        outBuf.writeInt(50);
        outBuf.writeFloat(0.60F);
        JNetwork.sendToPlayer(player, JPacketRegistry.S2C_COLOR_SHIFT, outBuf);
        return true;
    }
}
