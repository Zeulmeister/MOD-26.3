package net.arna.jcraft.platform;

import net.arna.jcraft.common.menu.DiscCaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class JPlatformUtils {
    public static void callDiscCaseMenu(final ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new DiscCaseMenu(syncId, p),
                Component.translatable("jcraft.gui.disc_case")
        ));
    }
}
