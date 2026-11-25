package net.arna.jcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.taunt.PlayerPoseTauntHandler;
import net.arna.jcraft.common.network.c2s.RadialMenuActionPacket;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Interactive 8-Slice JoJo Radial Wheel Screen (default key: Y).
 * Features dynamic Stand skin availability & real skin names, active skin/pilot/HUD badges,
 * smooth pop-in animation, keyboard shortcuts (1-8, G for Guide), and center Guide medallion.
 */
public final class JoJoRadialWheelScreen extends Screen {
    private static final int SLICE_COUNT = 8;
    private static final int RADIUS = 78;
    public static final long OPEN_ANIM_DURATION_MS = 120L;

    private final long openTimeMs = System.currentTimeMillis();
    private int hoveredSlice = -1;
    private boolean hoveringCenter = false;

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

    /**
     * Smooth cubic ease-out pop-in scale in [0.72, 1.0].
     */
    public static float computeOpenScale(final long elapsedMs) {
        if (elapsedMs <= 0L) {
            return 0.72f;
        }
        if (elapsedMs >= OPEN_ANIM_DURATION_MS) {
            return 1.0f;
        }
        float t = elapsedMs / (float) OPEN_ANIM_DURATION_MS;
        float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
        return Mth.lerp(eased, 0.72f, 1.0f);
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

        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        final StandType standType = player != null ? JComponentPlatformUtils.getStandComponent(player).getType() : null;
        final boolean hasStand = standType != null && !StandTypeUtil.isNone(standType);
        final int activeSkin = (player != null && hasStand) ? JComponentPlatformUtils.getStandComponent(player).getSkin() : -1;
        final int skinCount = hasStand ? standType.getData().getInfo().getSkinCount() : 0;
        final StandEntity<?, ?> activeStand = player != null ? JUtils.getStand(player) : null;
        final boolean pilotActive = activeStand != null && activeStand.isRemote();
        final boolean hudOn = JClientConfig.getInstance().isAnimeHudEnabled();

        final int cx = this.width / 2;
        final int cy = this.height / 2;
        final double dx = mouseX - cx;
        final double dy = mouseY - cy;
        final double dist = Math.hypot(dx, dy);

        boolean prevCenter = hoveringCenter;
        hoveringCenter = Math.abs(dx) <= 32 && Math.abs(dy) <= 15;
        if (hoveringCenter && !prevCenter && player != null) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.6F);
        }

        if (!hoveringCenter && dist >= 22.0) {
            double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
            if (angle < 0) angle += Math.PI * 2.0;
            int newSlice = ((int) Math.floor((angle + Math.PI / SLICE_COUNT) / (Math.PI * 2.0 / SLICE_COUNT))) % SLICE_COUNT;
            if (newSlice != hoveredSlice && player != null) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.6F);
            }
            hoveredSlice = newSlice;
        } else {
            hoveredSlice = -1;
        }

        // Equipped Stand header pill cleanly above the wheel
        final Component standHeader = hasStand
                ? Component.literal("★ ").append(standType.getData().getInfo().getName())
                : Component.translatable("jcraft.radial.no_stand");
        final int headerW = Math.max(96, this.font.width(standHeader) + 18);
        final int headerY = cy - RADIUS - 32;
        graphics.fill(cx - headerW / 2, headerY, cx + headerW / 2, headerY + 15, 0xCC160C28);
        graphics.outline(cx - headerW / 2, headerY, headerW, 15, hasStand ? 0xFFD4AF37 : 0x669CA3AF);
        graphics.centeredText(this.font, standHeader, cx, headerY + 4, hasStand ? 0xFFFDE68A : 0xFF9CA3AF);

        final float openScale = computeOpenScale(System.currentTimeMillis() - openTimeMs);

        // Interactive center medallion -> Opens Stand & Gameplay Guide on click (or G)
        int medBg = hoveringCenter ? 0xEE3B0764 : 0xDD1A0B2E;
        int medBorder = hoveringCenter ? 0xFFFDE047 : 0xFFD4AF37;
        graphics.fill(cx - 32, cy - 14, cx + 32, cy + 14, medBg);
        graphics.outline(cx - 32, cy - 14, 64, 28, medBorder);
        graphics.centeredText(this.font, Component.translatable("jcraft.radial.guide_center"), cx, cy - 4,
                hoveringCenter ? 0xFFFDE047 : 0xFFFDE68A);

        // 8 radial cards arranged around the center with smooth open scale-in
        final float scaledRadius = RADIUS * openScale;
        for (int i = 0; i < SLICE_COUNT; i++) {
            double theta = -Math.PI / 2.0 + i * (Math.PI * 2.0 / SLICE_COUNT);
            int sx = cx + (int) Math.round(Math.cos(theta) * scaledRadius);
            int sy = cy + (int) Math.round(Math.sin(theta) * scaledRadius);
            boolean selected = (i == hoveredSlice);

            boolean isSkinSlice = i <= 3;
            boolean skinAvailable = !isSkinSlice || (hasStand && i < skinCount);
            boolean isEquippedSkin = isSkinSlice && hasStand && i == activeSkin;

            int boxW = 68;
            int boxH = 24;
            int bx = sx - boxW / 2;
            int by = sy - boxH / 2;

            int bg;
            int border;
            int textCol;
            if (!skinAvailable) {
                bg = 0x77100C1A;
                border = selected ? 0x886B7280 : 0x444B5563;
                textCol = 0xFF6B7280;
            } else if (selected) {
                bg = 0xEE3B0764;
                border = 0xFFFACC15;
                textCol = 0xFFFDE047;
            } else if (isEquippedSkin) {
                bg = 0xDD122620;
                border = 0xFF34D399;
                textCol = 0xFFA7F3D0;
            } else if (i == 5 && pilotActive) {
                bg = 0xDD0E2233;
                border = 0xFF38BDF8;
                textCol = 0xFF7DD3FC;
            } else {
                bg = 0xCC160C28;
                border = 0x99A855F7;
                textCol = 0xFFE9D5FF;
            }

            graphics.fill(bx, by, bx + boxW, by + boxH, bg);
            graphics.outline(bx, by, boxW, boxH, border);

            String label = SLICE_ICONS[i];
            if (isEquippedSkin) {
                label = "✓ " + label;
            } else if (i == 6) {
                label = hudOn ? "◈ HUD ●" : "◈ HUD ○";
            } else if (i == 5 && pilotActive) {
                label = "✦ Pilot ●";
            }
            graphics.centeredText(this.font, label, sx, sy - 4, textCol);

            // Subtle numeric hotkey hint in top-left corner of each card (1-8)
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(bx + 3, by + 2);
            pose.scale(0.60f, 0.60f);
            graphics.text(this.font, Integer.toString(i + 1), 0, 0, skinAvailable ? 0x99C4B5FD : 0x556B7280, false);
            pose.popMatrix();
        }

        // Contextual footer description below the wheel
        final Component footerMsg;
        final int footerCol;
        if (hoveringCenter) {
            footerMsg = Component.translatable("jcraft.radial.open_guide");
            footerCol = 0xFFFDE047;
        } else if (hoveredSlice >= 0 && hoveredSlice < SLICE_COUNT) {
            if (hoveredSlice <= 3) {
                if (!hasStand || hoveredSlice >= skinCount) {
                    footerMsg = Component.translatable("jcraft.radial.skin_unavailable");
                    footerCol = 0xFF9CA3AF;
                } else {
                    footerMsg = resolveSkinHoverLabel(standType.getData().getInfo(), hoveredSlice, hoveredSlice == activeSkin);
                    footerCol = hoveredSlice == activeSkin ? 0xFF34D399 : 0xFFFDE68A;
                }
            } else {
                footerMsg = Component.translatable(SLICE_KEYS[hoveredSlice]);
                footerCol = 0xFFFDE68A;
            }
        } else {
            footerMsg = Component.translatable("jcraft.radial.hint");
            footerCol = 0xFFC4B5FD;
        }
        graphics.centeredText(this.font, footerMsg, cx, cy + RADIUS + 24, footerCol);
    }

    private static Component resolveSkinHoverLabel(final StandInfo info, final int skinIndex, final boolean equipped) {
        final Component baseLabel;
        if (skinIndex == 0) {
            baseLabel = Component.translatable("jcraft.radial.skin0");
        } else {
            List<Component> names = info.getSkinNames();
            if (skinIndex - 1 < names.size()) {
                baseLabel = Component.translatable("jcraft.radial.skin_named", skinIndex, names.get(skinIndex - 1));
            } else {
                baseLabel = Component.translatable(SLICE_KEYS[skinIndex]);
            }
        }
        if (equipped) {
            return baseLabel.copy().append(" ").append(Component.translatable("jcraft.radial.active_tag"));
        }
        return baseLabel;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (hoveringCenter) {
                Minecraft.getInstance().gui.setScreen(new StandGuideScreen(null));
                return true;
            }
            if (hoveredSlice >= 0) {
                executeSlice(hoveredSlice);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_8) {
            executeSlice(key - InputConstants.KEY_1);
            return true;
        }
        if (key == InputConstants.KEY_G) {
            Minecraft.getInstance().gui.setScreen(new StandGuideScreen(null));
            return true;
        }
        return super.keyPressed(event);
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
                LocalPlayer player = mc.player;
                if (player != null) {
                    CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
                    StandType st = comp.getType();
                    if (st == null || StandTypeUtil.isNone(st) || slice >= st.getData().getInfo().getSkinCount()) {
                        return; // Gracefully ignore unavailable skin slot
                    }
                }
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
