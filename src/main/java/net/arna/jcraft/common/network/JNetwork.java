package net.arna.jcraft.common.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean Minecraft 26.3 CustomPacketPayload + StreamCodec bridge for all JCraft packet channels.
 */
public final class JNetwork {
    private static final Map<Identifier, ChannelEntry> CHANNELS = new ConcurrentHashMap<>();
    private static final Set<Identifier> REGISTERED_S2C_TYPES = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> REGISTERED_C2S_RECEIVERS = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> REGISTERED_S2C_RECEIVERS = ConcurrentHashMap.newKeySet();

    private JNetwork() {}

    public record RawPayload(CustomPacketPayload.Type<RawPayload> payloadType, byte[] bytes) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }

    private record ChannelEntry(CustomPacketPayload.Type<RawPayload> type, StreamCodec<RegistryFriendlyByteBuf, RawPayload> codec) {}

    @FunctionalInterface
    public interface LegacyReceiver {
        void receive(FriendlyByteBuf buf, NetworkManager.PacketContext context);
    }

    private static ChannelEntry getOrCreateChannel(Identifier id) {
        return CHANNELS.computeIfAbsent(id, key -> {
            CustomPacketPayload.Type<RawPayload> type = new CustomPacketPayload.Type<>(key);
            StreamCodec<RegistryFriendlyByteBuf, RawPayload> codec = StreamCodec.of(
                    (buf, payload) -> buf.writeByteArray(payload.bytes()),
                    buf -> new RawPayload(type, buf.readByteArray())
            );
            return new ChannelEntry(type, codec);
        });
    }

    public static void registerS2CPayload(Identifier id) {
        ChannelEntry entry = getOrCreateChannel(id);
        if (REGISTERED_S2C_TYPES.add(id)) {
            if (Platform.getEnvironment() == Env.SERVER) {
                NetworkManager.registerS2CPayloadType(entry.type(), entry.codec());
            }
        }
    }

    public static void registerReceiver(NetworkManager.Side side, Identifier id, LegacyReceiver receiver) {
        ChannelEntry entry = getOrCreateChannel(id);
        if (side == NetworkManager.Side.C2S) {
            if (REGISTERED_C2S_RECEIVERS.add(id)) {
                NetworkManager.registerReceiver(NetworkManager.Side.C2S, entry.type(), entry.codec(), (payload, ctx) -> {
                    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()), ctx.registryAccess());
                    receiver.receive(buf, ctx);
                });
            }
        } else {
            REGISTERED_S2C_TYPES.add(id);
            if (REGISTERED_S2C_RECEIVERS.add(id)) {
                NetworkManager.registerReceiver(NetworkManager.Side.S2C, entry.type(), entry.codec(), (payload, ctx) -> {
                    RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()), ctx.registryAccess());
                    receiver.receive(buf, ctx);
                });
            }
        }
    }

    private static byte[] extractBytes(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return bytes;
    }

    public static void sendToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf buf) {
        if (player == null) return;
        ChannelEntry entry = getOrCreateChannel(id);
        NetworkManager.sendToPlayer(player, new RawPayload(entry.type(), extractBytes(buf)));
    }

    public static void sendToPlayers(Iterable<ServerPlayer> players, Identifier id, FriendlyByteBuf buf) {
        ChannelEntry entry = getOrCreateChannel(id);
        RawPayload payload = new RawPayload(entry.type(), extractBytes(buf));
        NetworkManager.sendToPlayers(players, payload);
    }

    public static void sendToServer(Identifier id, FriendlyByteBuf buf) {
        ChannelEntry entry = getOrCreateChannel(id);
        NetworkManager.sendToServer(new RawPayload(entry.type(), extractBytes(buf)));
    }

    public static boolean canPlayerReceive(ServerPlayer player, Identifier id) {
        return player != null && NetworkManager.canPlayerReceive(player, id);
    }
}
