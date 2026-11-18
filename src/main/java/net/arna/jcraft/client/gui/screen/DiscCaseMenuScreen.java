package net.arna.jcraft.client.gui.screen;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.menu.DiscCaseMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class DiscCaseMenuScreen extends AbstractContainerScreen<DiscCaseMenu> {
    private static final Identifier DISC_CASE_BACKGROUND = JCraft.id("textures/gui/disc_case_menu_screen.png");
    private final int slots;

    public DiscCaseMenuScreen(final DiscCaseMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        slots = menu.getSlotCount();
    }

    @Override
    public void extractRenderState(final @NonNull GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        extractTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(final @NonNull GuiGraphicsExtractor guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DISC_CASE_BACKGROUND, leftPos, topPos, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DISC_CASE_BACKGROUND, leftPos + 7 + 9 * (9 - slots / 2), topPos + 26, 7f, 83f, 9 * slots, 36, 256, 256);
    }
}
