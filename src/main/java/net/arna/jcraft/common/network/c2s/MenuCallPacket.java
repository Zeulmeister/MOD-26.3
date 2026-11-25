package net.arna.jcraft.common.network.c2s;

import dev.architectury.networking.NetworkManager;
import net.arna.jcraft.common.menu.MainMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class MenuCallPacket {

    public static void handle(final FriendlyByteBuf buf, final NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, player) -> new MainMenu(id, player),
                    Component.translatable("container.jcraft.main_menu")
            ));
        }
    }
}
