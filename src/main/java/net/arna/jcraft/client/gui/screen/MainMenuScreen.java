package net.arna.jcraft.client.gui.screen;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.menu.MainMenu;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class MainMenuScreen extends AbstractContainerScreen<MainMenu> {

    protected StandEntity<?,?> stand;

    public MainMenuScreen(final MainMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        stand = JUtils.getStand(playerInventory.player);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, JCraft.id("textures/gui/menu_screen.png"), leftPos, topPos, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY) {
        int row = 0;
        drawText(guiGraphics, this.title, row++, 0);
        if (stand == null || StandTypeUtil.isNone(stand.getStandType())) {
            drawText(guiGraphics, JStandTypeRegistry.NONE.get().getData().getInfo().getName(), row++, 0);
        }
        else {
            StandInfo info = stand.getStandData().getInfo();
            drawText(guiGraphics, info.getName(), row++, 0);
            final String desc = String.format("%s%s.info.desc", info.getNameKey(),
                    stand.getModeOrdinal() == 0 ? "" : Integer.toString(stand.getModeOrdinal()));
            drawText(guiGraphics, Component.translatable(desc), row++, 0);
            drawText(guiGraphics, Component.literal("PROS"), row++, 0);
            final int pros = info.getProCount();
            for (int p = 1; p <= pros; p++) {
                final Component pro = Component.literal("● ").append(Component.translatable(
                        String.format("%s.info.pro%d", info.getNameKey(), p)));
                drawText(guiGraphics, pro, row++, 0);
            }
            drawText(guiGraphics, Component.literal("CONS"), row++, 0);
            final int cons = info.getConCount();
            for (int c = 1; c <= cons; c++) {
                final Component con = Component.literal("● ").append(Component.translatable(
                        String.format("%s.info.con%d", info.getNameKey(), c)));
                drawText(guiGraphics, con, row++, 0);
            }
        }
    }

    protected void drawText(final GuiGraphicsExtractor guiGraphics, final Component text, final int row, final int col) {
        drawText(guiGraphics, text, row, col, 4210752);
    }

    protected void drawText(final GuiGraphicsExtractor guiGraphics, final Component text, final int row, final int col, final int color) {
        guiGraphics.text(this.font, text, this.titleLabelX+10*col, this.titleLabelY+10*row, color, false);
    }
}
