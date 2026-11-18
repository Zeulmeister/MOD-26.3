package net.arna.jcraft.client.gui.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.taunt.PlayerPoseTauntHandler;
import net.arna.jcraft.common.network.c2s.RadialMenuActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Interactive 8-Slice JoJo Radial Wheel Screen (default key: Y).
 * Allows quick Stand Skin switching (Default, Skin 1, Skin 2, Skin 3),
 * JoJo Pose Taunt, Pilot Mode toggle, Anime HUD toggle, and opening JCraft Config.
 */
public final class JoJoRadialWheelScreen extends Screen {
    private static final int SLICE_COUNT = 8;
    private static final int RADIUS = 76;
    private int hoveredSlice = -1;

    private static final String[] SLICE_KEYS = {
            "jcraft.radial.skin0",
            "jcraft.radial.skin1",
            "jcraft.radial.skin2",
            "jcraft.radial.skin3",
            "jcraft.radial.pose_taunt",
            "jcraft.radial.pilot_mode",
            "jcraft.radial.toggle_hud",
            "jcraft.radial.open_config"
    };

    private static final String[] SLICE_ICONS = {
            "★0", "★1", "★2", "★3", "ゴ Pose", "✦ Pilot", "◈ HUD", "⚙ Config"
    };

    public JoJoRadialWheelScreen() {
        super(Component.translatable("jcraft.radial.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Subtle translucent anime backdrop without blurring active gameplay
        graphics.fill(0, 0, this.width, this.height, 0x55090514);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        if (dist >= 18.0) {
            double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
            if (angle < 0) angle += Math.PI * 2.0;
            int newSlice = ((int) Math.floor((angle + Math.PI / SLICE_COUNT) / (Math.PI * 2.0 / SLICE_COUNT))) % SLICE_COUNT;
            if (newSlice != hoveredSlice && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.6F);
            }
            hoveredSlice = newSlice;
        } else {
            hoveredSlice = -1;
        }

        // Center medallion
        graphics.fill(cx - 28, cy - 14, cx + 28, cy + 14, 0xDD1A0B2E);
        graphics.outline(cx - 28, cy - 14, 56, 28, 0xFFD4AF37);
        graphics.centeredText(this.font, "★ JOJO ★", cx, cy - 4, 0xFFFDE68A);

        // 8 radial cards arranged around the center
        for (int i = 0; i < SLICE_COUNT; i++) {
            double theta = -Math.PI / 2.0 + i * (Math.PI * 2.0 / SLICE_COUNT);
            int sx = cx + (int) Math.round(Math.cos(theta) * RADIUS);
            int sy = cy + (int) Math.round(Math.sin(theta) * RADIUS);
            boolean selected = (i == hoveredSlice);

            int boxW = 64;
            int boxH = 24;
            int bx = sx - boxW / 2;
            int by = sy - boxH / 2;

            int bg = selected ? 0xEE3B0764 : 0xCC160C28;
            int border = selected ? 0xFFFACC15 : 0x99A855F7;
            graphics.fill(bx, by, bx + boxW, by + boxH, bg);
            graphics.outline(bx, by, boxW, boxH, border);
            graphics.centeredText(this.font, SLICE_ICONS[i], sx, sy - 4, selected ? 0xFFFDE047 : 0xFFE9D5FF);
        }

        if (hoveredSlice >= 0 && hoveredSlice < SLICE_COUNT) {
            graphics.centeredText(this.font, Component.translatable(SLICE_KEYS[hoveredSlice]), cx, cy + RADIUS + 24, 0xFFFDE68A);
        } else {
            graphics.centeredText(this.font, Component.translatable("jcraft.radial.hint"), cx, cy + RADIUS + 24, 0xFFC4B5FD);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && hoveredSlice >= 0) {
            executeSlice(hoveredSlice);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (JCraftClient.RADIAL_MENU_KEY != null && JCraftClient.RADIAL_MENU_KEY.getParent().matches(event)) {
            if (hoveredSlice >= 0) {
                executeSlice(hoveredSlice);
            } else {
                onClose();
            }
            return true;
        }
        return super.keyReleased(event);
    }

    private void executeSlice(int slice) {
        Minecraft mc = Minecraft.getInstance();
        switch (slice) {
            case 0, 1, 2, 3 -> {
                RadialMenuActionPacket.send(slice);
                onClose();
            }
            case 4 -> {
                RadialMenuActionPacket.send(RadialMenuActionPacket.ACTION_POSE_TAUNT);
                PlayerPoseTauntHandler.triggerClientEffects(mc);
                onClose();
            }
            case 5 -> {
                RadialMenuActionPacket.send(RadialMenuActionPacket.ACTION_TOGGLE_PILOT);
                onClose();
            }
            case 6 -> {
                JClientConfig cfg = JClientConfig.getInstance();
                cfg.setAnimeHudEnabled(!cfg.isAnimeHudEnabled());
                try {
                    AutoConfig.getConfigHolder(JClientConfig.class).save();
                } catch (Throwable ignored) {}
                onClose();
            }
            case 7 -> mc.gui.setScreen(new JCraftConfigScreen(null));
            default -> onClose();
        }
    }
}
