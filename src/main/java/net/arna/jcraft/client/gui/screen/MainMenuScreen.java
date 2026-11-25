package net.arna.jcraft.client.gui.screen;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.menu.MainMenu;
import net.arna.jcraft.common.util.JUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

@Environment(EnvType.CLIENT)
public class MainMenuScreen extends AbstractContainerScreen<MainMenu> {

    protected StandEntity<?, ?> stand;

    public MainMenuScreen(final MainMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        stand = JUtils.getStand(playerInventory.player);
    }

    @Override
    protected void init() {
        super.init();
        int btnW = 86;
        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.config.open_guide"),
                        b -> Minecraft.getInstance().gui.setScreen(new StandGuideScreen(this)))
                .bounds(this.leftPos + this.imageWidth - btnW - 6, this.topPos + this.imageHeight - 22, btnW, 16)
                .build());
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, JCraft.id("textures/gui/menu_screen.png"), leftPos, topPos, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY) {
        final int maxW = Math.max(80, this.imageWidth - 16);
        final int maxY = this.imageHeight - 28;
        int y = this.titleLabelY;

        y = drawWrappedText(guiGraphics, this.title, y, maxW, maxY, 0xFF3A2A5E);
        if (stand == null || StandTypeUtil.isNone(stand.getStandType())) {
            drawWrappedText(guiGraphics, JStandTypeRegistry.NONE.get().getData().getInfo().getName(), y, maxW, maxY, 4210752);
            return;
        }

        StandInfo info = stand.getStandData().getInfo();
        y = drawWrappedText(guiGraphics, info.getName(), y, maxW, maxY, 0xFF5B21B6);

        final String descKey = String.format("%s%s.info.desc", info.getNameKey(),
                stand.getModeOrdinal() == 0 ? "" : Integer.toString(stand.getModeOrdinal()));
        if (Language.getInstance().has(descKey)) {
            y = drawWrappedText(guiGraphics, Component.translatable(descKey), y, maxW, maxY, 4210752);
        }

        final int pros = info.getProCount();
        if (pros > 0 && y < maxY) {
            y = drawWrappedText(guiGraphics, Component.literal("PROS"), y + 2, maxW, maxY, 0xFF0F766E);
            for (int p = 1; p <= pros && y < maxY; p++) {
                String k = String.format("%s.info.pro%d", info.getNameKey(), p);
                if (Language.getInstance().has(k)) {
                    y = drawWrappedText(guiGraphics, Component.literal("• ").append(Component.translatable(k)), y, maxW, maxY, 4210752);
                }
            }
        }

        final int cons = info.getConCount();
        if (cons > 0 && y < maxY) {
            y = drawWrappedText(guiGraphics, Component.literal("CONS"), y + 2, maxW, maxY, 0xFFB91C1C);
            for (int c = 1; c <= cons && y < maxY; c++) {
                String k = String.format("%s.info.con%d", info.getNameKey(), c);
                if (Language.getInstance().has(k)) {
                    y = drawWrappedText(guiGraphics, Component.literal("• ").append(Component.translatable(k)), y, maxW, maxY, 4210752);
                }
            }
        }
    }

    protected int drawWrappedText(final GuiGraphicsExtractor guiGraphics, final Component text, int y, final int maxW, final int maxY, final int color) {
        final List<FormattedCharSequence> lines = this.font.split(text, maxW);
        for (FormattedCharSequence line : lines) {
            if (y >= maxY) {
                break;
            }
            guiGraphics.text(this.font, line, this.titleLabelX, y, color, false);
            y += 9;
        }
        return y;
    }

    protected void drawText(final GuiGraphicsExtractor guiGraphics, final Component text, final int row, final int col) {
        drawText(guiGraphics, text, row, col, 4210752);
    }

    protected void drawText(final GuiGraphicsExtractor guiGraphics, final Component text, final int row, final int col, final int color) {
        guiGraphics.text(this.font, text, this.titleLabelX + 10 * col, this.titleLabelY + 10 * row, color, false);
    }
}
