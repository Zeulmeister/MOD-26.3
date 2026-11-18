package net.arna.jcraft.client.gui.hud;

import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.combat.CombatInputBuffer;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.client.taunt.PlayerPoseTauntHandler;
import net.arna.jcraft.client.util.JClientUtils;
import net.arna.jcraft.client.zoom.StarPlatinumPrecisionHandler;
import net.arna.jcraft.common.util.DimensionData;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Modern JoJo Anime HUD Renderer for Minecraft 26.3:
 * Renders Stand Gauge, Time Stop countdown + suspended kinetic projectile counter,
 * Pilot Mode range/efficiency & Body-Under-Attack alert, Pose Taunt progress,
 * Precision Vision reticle framing, and Vampire/Hamon spec gauges.
 */
public final class JoJoAnimeHudRenderer {
    private static int clientSuspendedProjectiles = 0;

    private JoJoAnimeHudRenderer() {}

    public static void setClientSuspendedProjectiles(int count) {
        clientSuspendedProjectiles = Math.max(0, count);
    }

    public static int getClientSuspendedProjectiles() {
        return clientSuspendedProjectiles;
    }

    public static void render(GuiGraphicsExtractor ctx, float partialTick) {
        if (!JClientConfig.getInstance().isAnimeHudEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        Font font = client.font;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
        StandType standType = standComp.getType();
        StandEntity<?, ?> stand = JUtils.getStand(player);

        // 1. Sleek Bottom-Left JoJo Stand Card & Guard Gauge
        if (standType != null && !StandTypeUtil.isNone(standType)) {
            int cardX = 10;
            int cardY = sh - 44;
            int cardW = 132;
            int cardH = 32;

            ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xB4120B24);
            ctx.outline(cardX, cardY, cardW, cardH, stand != null ? 0xFFD4AF37 : 0x887C3AED);
            // Gold anime corner accent
            ctx.fill(cardX, cardY, cardX + 3, cardY + cardH, stand != null ? 0xFFF59E0B : 0xFF7C3AED);

            String standName = standType.getData().getInfo().getName().getString();
            int skin = stand != null ? stand.getSkin() : standComp.getSkin();
            String title = skin > 0 ? (standName + " [S" + skin + "]") : standName;
            ctx.text(font, title, cardX + 7, cardY + 4, 0xFFFDE68A, true);

            if (stand != null) {
                float ratio = Mth.clamp(stand.getStandGauge() / Math.max(1.0F, stand.getMaxStandGauge()), 0.0F, 1.0F);
                int barX = cardX + 7;
                int barY = cardY + 17;
                int barW = cardW - 14;
                int barH = 8;
                ctx.fill(barX, barY, barX + barW, barY + barH, 0xCC1E1B2E);
                int barColor = ratio > 0.5F ? 0xFF34D399 : (ratio > 0.25F ? 0xFFFBBF24 : 0xFFEF4444);
                ctx.fill(barX, barY, barX + (int) (barW * ratio), barY + barH, barColor);
                ctx.outline(barX, barY, barW, barH, 0x99FDE68A);

                String gaugeLabel = Math.round(stand.getStandGauge()) + "/" + Math.round(stand.getMaxStandGauge());
                ctx.text(font, gaugeLabel, barX + barW - font.width(gaugeLabel) - 2, barY, 0xFFFFFFFF, true);
            } else {
                String hint = "[" + JCraftClient.generateName(JCraftClient.STAND_SUMMON_KEY.getParent(), true) + "] Stand Off";
                ctx.text(font, hint, cardX + 7, cardY + 17, 0xFFB4A5D9, true);
            }

            // Buffered Move indicator pill
            if (CombatInputBuffer.hasBufferedMove() && CombatInputBuffer.peekBufferedMove() != null) {
                String bufTxt = "⚡ " + CombatInputBuffer.peekBufferedMove().name();
                ctx.text(font, bufTxt, cardX + 7, cardY - 11, 0xFF38BDF8, true);
            }
        }

        // 2. Spec Gauge (Vampire Blood / Hamon Charge) right above Stand Card
        CommonVampireComponent vamp = JComponentPlatformUtils.getVampirism(player);
        CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(player);
        if (vamp.isVampire()) {
            float bloodRatio = Mth.clamp(vamp.getBlood() / 20.0F, 0.0F, 1.0F);
            drawMiniSpecBar(ctx, font, 10, sh - 58, 132, bloodRatio, 0xFFDC2626, "BLOOD");
        } else if (hamon.getHamonCharge() > 0.01F) {
            float hamonRatio = Mth.clamp(hamon.getHamonCharge() / 100.0F, 0.0F, 1.0F);
            drawMiniSpecBar(ctx, font, 10, sh - 58, 132, hamonRatio, 0xFFFACC15, "HAMON");
        }

        // 3. Active Time Stop Top-Center Banner + Kinetic Projectile Counter
        int tsTicks = getRemainingTimeStopTicks(player);
        if (tsTicks > 0) {
            int boxW = 168;
            int boxX = (sw - boxW) / 2;
            int boxY = 10;
            ctx.fill(boxX, boxY, boxX + boxW, boxY + 24, 0xCC1A0B2E);
            ctx.outline(boxX, boxY, boxW, boxY + 14, 0xFFC084FC);
            String secStr = String.format(java.util.Locale.ROOT, "⏱ ZA WARUDO: %.1fs", tsTicks / 20.0F);
            ctx.centeredText(font, secStr, sw / 2, boxY + 4, 0xFFE9D5FF);
            if (clientSuspendedProjectiles > 0) {
                Component kinStr = Component.translatable("jcraft.hud.kinetic_count", clientSuspendedProjectiles);
                ctx.centeredText(font, kinStr, sw / 2, boxY + 14, 0xFFFDE047);
            }
        }

        // 4. Pilot Mode Telemetry & Body-Under-Attack Warning Banner
        if (stand != null && stand.isRemoteAndControllable()) {
            double dist = stand.distanceTo(player);
            double maxRange = stand.getMaxRemoteRange();
            int effPct = Math.round(stand.getRemoteEfficiency() * 100.0F);
            int pBoxW = 176;
            int pBoxX = (sw - pBoxW) / 2;
            int pBoxY = tsTicks > 0 ? 38 : 10;
            ctx.fill(pBoxX, pBoxY, pBoxX + pBoxW, pBoxY + 22, 0xCC0F172A);
            ctx.outline(pBoxX, pBoxY, pBoxW, 22, 0xFF38BDF8);
            Component pilotTxt = Component.translatable(
                    "jcraft.hud.pilot_active",
                    String.format(java.util.Locale.ROOT, "%.1f", dist),
                    String.format(java.util.Locale.ROOT, "%.0f", maxRange),
                    effPct
            );
            ctx.centeredText(font, pilotTxt, sw / 2, pBoxY + 7, effPct >= 75 ? 0xFF7DD3FC : 0xFFFBBF24);

            int danger = StandRemoteControlHandler.getBodyDangerTicks();
            if (danger > 0 && (danger / 4) % 2 == 0) {
                ctx.centeredText(font, Component.translatable("jcraft.pilot.body_danger"), sw / 2, pBoxY + 26, 0xFFEF4444);
            }
        }

        // 5. JoJo Pose Taunt Charge Bar above Crosshair
        if (PlayerPoseTauntHandler.isPosing()) {
            float ratio = PlayerPoseTauntHandler.getProgressRatio();
            int barW = 72;
            int barX = (sw - barW) / 2;
            int barY = sh / 2 + 16;
            ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + 5, 0xCC1E1030);
            ctx.fill(barX, barY, barX + (int) (barW * ratio), barY + 4, 0xFFF59E0B);
            ctx.centeredText(font, "★ JOJO POSE ★", sw / 2, barY + 7, 0xFFFDE68A);
        }

        // 6. Precision Vision Zoom Tactical Brackets
        float zoom = StarPlatinumPrecisionHandler.getZoomProgress(partialTick);
        if (zoom > 0.05F) {
            int alpha = Mth.clamp((int) (zoom * 200.0F), 0, 200);
            int col = ARGB.color(alpha, 0xA855F7);
            int cx = sw / 2;
            int cy = sh / 2;
            int r = (int) Mth.lerp(zoom, 48.0F, 26.0F);
            ctx.fill(cx - r, cy - r, cx - r + 10, cy - r + 2, col);
            ctx.fill(cx - r, cy - r, cx - r + 2, cy - r + 10, col);
            ctx.fill(cx + r - 10, cy - r, cx + r, cy - r + 2, col);
            ctx.fill(cx + r - 2, cy - r, cx + r, cy - r + 10, col);
            ctx.fill(cx - r, cy + r - 2, cx - r + 10, cy + r, col);
            ctx.fill(cx - r, cy + r - 10, cx - r + 2, cy + r, col);
            ctx.fill(cx + r - 10, cy + r - 2, cx + r, cy + r, col);
            ctx.fill(cx + r - 2, cy + r - 10, cx + r, cy + r, col);
            ctx.centeredText(font, "5.0× PRECISION", cx, cy + r + 5, ARGB.color(alpha, 0xE9D5FF));
        }
    }

    private static void drawMiniSpecBar(GuiGraphicsExtractor ctx, Font font, int x, int y, int width, float ratio, int color, String label) {
        ctx.fill(x, y, x + width, y + 11, 0xAA120B24);
        ctx.fill(x + 2, y + 2, x + 2 + (int) ((width - 4) * ratio), y + 9, color);
        ctx.outline(x, y, width, 11, 0x88FDE68A);
        ctx.text(font, label, x + 5, y + 2, 0xFFFFFFFF, true);
    }

    private static int getRemainingTimeStopTicks(LocalPlayer player) {
        for (DimensionData ts : JClientUtils.activeTimestops) {
            if (ts != null && ts.getTimer() > 0 && ts.getUser() == player) {
                return ts.getTimer();
            }
        }
        for (DimensionData ts : JClientUtils.activeTimestops) {
            if (ts != null && ts.getTimer() > 0 && JClientUtils.isInTSRange(player)) {
                return ts.getTimer();
            }
        }
        return 0;
    }
}
