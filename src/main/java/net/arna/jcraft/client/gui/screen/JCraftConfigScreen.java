package net.arna.jcraft.client.gui.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.arna.jcraft.client.JClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in JoJo 26.3 Configuration Screen (accessible from ModMenu and the Y Radial Wheel).
 */
public final class JCraftConfigScreen extends Screen {
    @Nullable
    private final Screen parent;

    public JCraftConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("jcraft.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        JClientConfig cfg = JClientConfig.getInstance();
        int centerX = this.width / 2;
        int startY = 44;
        int btnW = 220;
        int btnH = 20;
        int gap = 24;

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.anime_hud", cfg.isAnimeHudEnabled()), btn -> {
            cfg.setAnimeHudEnabled(!cfg.isAnimeHudEnabled());
            btn.setMessage(toggleLabel("jcraft.config.anime_hud", cfg.isAnimeHudEnabled()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.icon_hud", cfg.isIconHud()), btn -> {
            cfg.setIconHud(!cfg.isIconHud());
            btn.setMessage(toggleLabel("jcraft.config.icon_hud", cfg.isIconHud()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY + gap, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.onomatopoeia", cfg.isOnomatopoeiaEnabled()), btn -> {
            cfg.setOnomatopoeiaEnabled(!cfg.isOnomatopoeiaEnabled());
            btn.setMessage(toggleLabel("jcraft.config.onomatopoeia", cfg.isOnomatopoeiaEnabled()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY + gap * 2, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.input_buffer", cfg.isInputBufferEnabled()), btn -> {
            cfg.setInputBufferEnabled(!cfg.isInputBufferEnabled());
            btn.setMessage(toggleLabel("jcraft.config.input_buffer", cfg.isInputBufferEnabled()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY + gap * 3, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.color_shift", cfg.isColorShiftEnabled()), btn -> {
            cfg.setColorShiftEnabled(!cfg.isColorShiftEnabled());
            btn.setMessage(toggleLabel("jcraft.config.color_shift", cfg.isColorShiftEnabled()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY + gap * 4, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(toggleLabel("jcraft.config.stand_auras", cfg.isStandAuras()), btn -> {
            cfg.setStandAuras(!cfg.isStandAuras());
            btn.setMessage(toggleLabel("jcraft.config.stand_auras", cfg.isStandAuras()));
            saveConfig();
        }).bounds(centerX - btnW / 2, startY + gap * 5, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.config.advanced_cloth"), btn -> {
            try {
                Screen cloth = me.shedaniel.autoconfig.AutoConfigClient.getConfigScreen(JClientConfig.class, this).get();
                Minecraft.getInstance().gui.setScreen(cloth);
            } catch (Throwable ignored) {}
        }).bounds(centerX - btnW / 2, startY + gap * 6, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose())
                .bounds(centerX - 100, this.height - 28, 200, 20).build());
    }

    private static Component toggleLabel(String key, boolean enabled) {
        return Component.translatable(key).append(": ")
                .append(Component.literal(enabled ? "ON" : "OFF")
                        .withColor(enabled ? 0x34D399 : 0xEF4444));
    }

    private static void saveConfig() {
        try {
            AutoConfig.getConfigHolder(JClientConfig.class).save();
        } catch (Throwable ignored) {}
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFDE68A);
    }

    @Override
    public void onClose() {
        saveConfig();
        Minecraft.getInstance().gui.setScreen(this.parent);
    }
}
