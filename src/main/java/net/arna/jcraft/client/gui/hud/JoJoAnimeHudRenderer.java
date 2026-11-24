package net.arna.jcraft.client.gui.hud;

import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
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
 * Minimalist JoJo HUD Renderer for Minecraft 26.3:
 * Keeps the screen completely uncluttered at idle (the Stand status & ability bar is rendered
 * cleanly by {@link JCraftAbilityHud} only when a Stand is summoned), while displaying sleek
 * contextual overlays for active Time Stop, Pilot Mode telemetry, Pose Taunt charge,
 * Precision Vision reticle brackets, and slim Blood/Hamon gauges.
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
        if (player == null || player.isSpectator()) {
            return;
        }

        Font font = client.font;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        StandEntity<?, ?> stand = JUtils.getStand(player);
        JClientConfig cfg = JClientConfig.getInstance();

        // 1. Minimal Stand Guard micro-strip ONLY if Icon HUD is disabled while Stand is active
        //    (when Icon HUD is enabled, JCraftAbilityHud integrates this cleanly above the ability icons)
        if (stand != null && (!cfg.isIconHud() || cfg.getUiDuration() == JClientConfig.UIDuration.NEVER)) {
            int barW = 86;
            int barX = (cfg.getUiPosition() == JClientConfig.UIPos.LEFT ? 8 : sw - barW - 8) + cfg.getHorizontalHudOffset();
            int barY = sh - 18 + cfg.getVerticalHudOffset();
            drawMinimalStandHeader(ctx, font, stand, barX, barY, barW);
        }

        // 2. Subtle Buffered Move indicator only during the short 6-tick input buffer window
        if (stand != null && CombatInputBuffer.hasBufferedMove() && CombatInputBuffer.peekBufferedMove() != null) {
            String bufTxt = "⚡ " + CombatInputBuffer.peekBufferedMove().name();
            int bx = (cfg.getUiPosition() == JClientConfig.UIPos.LEFT ? 8 : sw - font.width(bufTxt) - 10) + cfg.getHorizontalHudOffset();
            int by = sh - (cfg.isIconHud() ? 72 : 28) + cfg.getVerticalHudOffset();
            ctx.text(font, bufTxt, bx, by, 0xCC7DD3FC, true);
        }

        // 3. Slim Vampire Blood / Hamon Charge micro-bar (only when relevant, unobtrusive)
        CommonVampireComponent vamp = JComponentPlatformUtils.getVampirism(player);
        CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(player);
        int specBarY = (stand != null && cfg.isIconHud() && cfg.getUiDuration() != JClientConfig.UIDuration.NEVER
                && cfg.getUiPosition() != JClientConfig.UIPos.MIDDLE)
                ? sh - 68
                : sh - 15;
        int specBarW = 89;
        int specBarX = (cfg.getUiPosition() == JClientConfig.UIPos.LEFT ? 8 : sw - specBarW - 8) + cfg.getHorizontalHudOffset();
        specBarY += cfg.getVerticalHudOffset();
        if (vamp.isVampire()) {
            float bloodRatio = Mth.clamp(vamp.getBlood() / 20.0F, 0.0F, 1.0F);
            drawMicroSpecBar(ctx, font, specBarX, specBarY, specBarW, bloodRatio, 0xCCDC2626, "BLOOD");
        } else if (hamon.getHamonCharge() > 0.01F || hamon.isHamonizeReady()) {
            float hamonRatio = Mth.clamp(hamon.getHamonCharge() / 100.0F, 0.0F, 1.0F);
            int hamonColor = hamon.isHamonizeReady() ? 0xFFFDE047 : 0xCCFACC15;
            String hamonLabel = hamon.isHamonizeReady() ? "HAMON ★" : "HAMON";
            drawMicroSpecBar(ctx, font, specBarX, specBarY, specBarW, hamonRatio, hamonColor, hamonLabel);
        }

        // 4. Active Time Stop Slim Top-Center Pill + Kinetic Projectile Counter
        int tsTicks = getRemainingTimeStopTicks(player);
        if (tsTicks > 0) {
            int boxW = 148;
            int boxH = clientSuspendedProjectiles > 0 ? 24 : 14;
            int boxX = (sw - boxW) / 2;
            int boxY = 8;
            ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x99120820);
            ctx.outline(boxX, boxY, boxW, boxH, 0x99C084FC);
            String secStr = String.format(java.util.Locale.ROOT, "⏱ %.1fs", tsTicks / 20.0F);
            ctx.centeredText(font, "ZA WARUDO  " + secStr, sw / 2, boxY + 3, 0xFFE9D5FF);
            if (clientSuspendedProjectiles > 0) {
                Component kinStr = Component.translatable("jcraft.hud.kinetic_count", clientSuspendedProjectiles);
                ctx.centeredText(font, kinStr, sw / 2, boxY + 13, 0xFFFDE047);
            }
        }

        // 5. Pilot Mode Telemetry & Body-Under-Attack Warning Pill
        if (stand != null && stand.isRemoteAndControllable()) {
            double dist = stand.distanceTo(player);
            double maxRange = stand.getMaxRemoteRange();
            int effPct = Math.round(stand.getRemoteEfficiency() * 100.0F);
            int pBoxW = 166;
            int pBoxX = (sw - pBoxW) / 2;
            int pBoxY = tsTicks > 0 ? 35 : 8;
            ctx.fill(pBoxX, pBoxY, pBoxX + pBoxW, pBoxY + 18, 0x990B1220);
            ctx.outline(pBoxX, pBoxY, pBoxW, 18, 0x9938BDF8);
            Component pilotTxt = Component.translatable(
                    "jcraft.hud.pilot_active",
                    String.format(java.util.Locale.ROOT, "%.1f", dist),
                    String.format(java.util.Locale.ROOT, "%.0f", maxRange),
                    effPct
            );
            ctx.centeredText(font, pilotTxt, sw / 2, pBoxY + 5, effPct >= 75 ? 0xFF7DD3FC : 0xFFFBBF24);

            int danger = StandRemoteControlHandler.getBodyDangerTicks();
            if (danger > 0 && (danger / 4) % 2 == 0) {
                ctx.centeredText(font, Component.translatable("jcraft.pilot.body_danger"), sw / 2, pBoxY + 22, 0xFFEF4444);
            }
        }

        // 6. JoJo Pose Taunt Charge Bar below Crosshair
        if (PlayerPoseTauntHandler.isPosing()) {
            float ratio = PlayerPoseTauntHandler.getProgressRatio();
            int barW = 64;
            int barX = (sw - barW) / 2;
            int barY = sh / 2 + 16;
            ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + 4, 0xAA160C24);
            ctx.fill(barX, barY, barX + (int) (barW * ratio), barY + 3, 0xEEF59E0B);
            ctx.centeredText(font, "★ POSE ★", sw / 2, barY + 6, 0xFFFDE68A);
        }

        // 7. Precision Vision Zoom Tactical Brackets
        float zoom = StarPlatinumPrecisionHandler.getZoomProgress(partialTick);
        if (zoom > 0.05F) {
            int alpha = Mth.clamp((int) (zoom * 190.0F), 0, 190);
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

    /**
     * Draws a whisper-quiet Stand name + 3px Guard bar directly above the ability dock.
     */
    static void drawMinimalStandHeader(GuiGraphicsExtractor ctx, Font font, StandEntity<?, ?> stand, int x, int y, int width) {
        String standName = stand.getStandType().getData().getInfo().getName().getString();
        float textScale = 0.72f;
        int maxTextPx = (int) (width / textScale);
        String fitted = font.width(standName) > maxTextPx
                ? font.plainSubstrByWidth(standName, Math.max(12, maxTextPx - 6)) + "…"
                : standName;

        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(textScale, textScale);
        ctx.text(font, fitted, 0, 0, 0xDCEAE4FF, true);
        pose.popMatrix();

        float ratio;
        int barColor;
        if (stand instanceof net.arna.jcraft.common.entity.stand.TheSunEntity theSun) {
            float sunSpan = Math.max(0.1F, net.arna.jcraft.common.entity.stand.TheSunEntity.MAX_SCALE - net.arna.jcraft.common.entity.stand.TheSunEntity.MIN_SCALE);
            ratio = Mth.clamp((theSun.getRawScale() - net.arna.jcraft.common.entity.stand.TheSunEntity.MIN_SCALE) / sunSpan, 0.05F, 1.0F);
            barColor = theSun.isPassive() ? 0xCC92400E : 0xCCF59E0B;
        } else {
            ratio = Mth.clamp(stand.getStandGauge() / Math.max(1.0F, stand.getMaxStandGauge()), 0.0F, 1.0F);
            barColor = ratio > 0.5F ? 0xCC34D399 : (ratio > 0.25F ? 0xCCFBBF24 : 0xCCEF4444);
        }
        int barY = y + 8;
        ctx.fill(x, barY, x + width, barY + 3, 0x660E0B16);
        int fillW = Math.round(width * ratio);
        if (fillW > 0) {
            ctx.fill(x, barY, x + fillW, barY + 3, barColor);
        }
    }

    private static void drawMicroSpecBar(GuiGraphicsExtractor ctx, Font font, int x, int y, int width, float ratio, int color, String label) {
        ctx.fill(x, y, x + width, y + 7, 0x660E0B16);
        int fillW = Math.round((width - 2) * ratio);
        if (fillW > 0) {
            ctx.fill(x + 1, y + 1, x + 1 + fillW, y + 6, color);
        }
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x + 3, y + 1);
        pose.scale(0.65f, 0.65f);
        ctx.text(font, label, 0, 0, 0xDDFFFFFF, true);
        pose.popMatrix();
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
