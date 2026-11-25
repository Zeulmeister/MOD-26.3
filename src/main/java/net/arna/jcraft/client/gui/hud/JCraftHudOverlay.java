package net.arna.jcraft.client.gui.hud;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.moves.BlockMarkerMove;
import net.arna.jcraft.api.component.living.CommonGunslingerComponent;
import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.common.entity.stand.AbstractPurpleHazeEntity;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.common.entity.stand.MetallicaEntity;
import net.arna.jcraft.common.entity.stand.PurpleHazeDistortionEntity;
import net.arna.jcraft.common.entity.stand.PurpleHazeEntity;
import net.arna.jcraft.common.spec.AnubisSpec;
import net.arna.jcraft.common.spec.RangerSpec;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Renders sleek, contextual Stand & Spec micro-gauges (Metallica Iron, Aerosmith Overheat,
 * Purple Haze Obedience, PHD Virus mode pill, Made in Heaven Speedometer, BlockMarker timers,
 * Anubis Bloodlust, and Ranger Focus) docked cleanly alongside the main HUD.
 */
@Environment(EnvType.CLIENT)
public final class JCraftHudOverlay {
    private static final Identifier PH_INDICATOR = JCraft.id("textures/gui/purple_haze/indicator.png");
    private static final int GAUGE_WIDTH = 86;
    private static final int BAR_HEIGHT = 7;
    private static final int BAR_STEP = 10;

    private JCraftHudOverlay() {}

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register((ctx, tickDelta) -> render(ctx));
    }

    public static void render(final GuiGraphicsExtractor ctx) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        final JClientConfig cfg = JClientConfig.getInstance();
        if (!cfg.isAnimeHudEnabled()) {
            return;
        }
        final int sw = client.getWindow().getGuiScaledWidth();
        final int sh = client.getWindow().getGuiScaledHeight();
        final Font font = client.font;

        final StandEntity<?, ?> stand = JUtils.getStand(player);
        final JSpec<?, ?> spec = JUtils.getSpec(player);
        final CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(player);
        final CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(player);

        final int gaugeX = switch (cfg.getUiPosition()) {
            case LEFT -> 8;
            case RIGHT -> sw - GAUGE_WIDTH - 8;
            case MIDDLE -> (sw - GAUGE_WIDTH) / 2;
        } + cfg.getHorizontalHudOffset();

        final boolean dockVisible = stand != null && cfg.isIconHud() && cfg.getUiDuration() != JClientConfig.UIDuration.NEVER;
        final boolean hasBottomSpecBar = hamon.getHamonCharge() > 0.01F || hamon.isHamonizeReady();
        final float hudScale = JCraftAbilityHud.getClampedHudScale();
        final int scaledDockLift = Math.round(72.0f * hudScale);

        int yCursor;
        if (cfg.getUiPosition() == JClientConfig.UIPos.MIDDLE) {
            yCursor = (dockVisible ? sh - 66 - Math.round(42.0f * hudScale) : sh - 58) + cfg.getVerticalHudOffset();
        } else {
            yCursor = (dockVisible ? sh - 8 - scaledDockLift : sh - 26) + cfg.getVerticalHudOffset();
        }
        if (hasBottomSpecBar) {
            yCursor -= 11;
        }

        if (stand instanceof MetallicaEntity metallica) {
            float ratio = Mth.clamp(metallica.getIron() / MetallicaEntity.IRON_MAX, 0.0F, 1.0F);
            int color = ratio < 0.25F ? 0xCCEF4444 : 0xCC93C5FD;
            String label = Component.translatable("jcraft.hud.gauge.iron").getString() + " " + Math.round(metallica.getIron());
            drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, color, label);
            yCursor -= BAR_STEP;
        } else if (stand instanceof AerosmithEntity aerosmith && aerosmith.getOverheat() > 0.05F) {
            float ratio = Mth.clamp(aerosmith.getOverheat() / AerosmithEntity.OVERHEAT_MAX, 0.0F, 1.0F);
            int color = ratio > 0.75F ? 0xCCEF4444 : 0xCCF97316;
            String label = Component.translatable("jcraft.hud.gauge.overheat").getString()
                    + " " + Math.round(ratio * 100.0F) + "%";
            drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, color, label);
            yCursor -= BAR_STEP;
        } else if (stand instanceof PurpleHazeEntity purpleHaze) {
            float ratio = Mth.clamp(purpleHaze.getObedience() / (float) PurpleHazeEntity.MAX_OBEDIENCE, 0.0F, 1.0F);
            String label = Component.translatable("jcraft.hud.gauge.obedience").getString()
                    + " " + Math.round(ratio * 100.0F) + "%";
            drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, 0xCCA855F7, label);
            yCursor -= BAR_STEP;
        } else if (stand instanceof PurpleHazeDistortionEntity phd) {
            yCursor = drawPhdModePill(ctx, font, gaugeX, yCursor - 10, phd.getSyncedPoisonType()) - 4;
        } else if (stand instanceof MadeInHeavenEntity mih && mih.getAccelTime() > 0) {
            float ratio = Mth.clamp(mih.getSpeedometer() / (float) MadeInHeavenEntity.MAXIMUM_SPEEDOMETER, 0.0F, 1.0F);
            String label = Component.translatable("jcraft.hud.gauge.speedometer").getString()
                    + " " + mih.getSpeedometer() + "/" + MadeInHeavenEntity.MAXIMUM_SPEEDOMETER;
            drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, 0xCCFACC15, label);
            yCursor -= BAR_STEP;
        }

        if (stand != null) {
            for (final BlockMarkerMove.RecordingTimes rt : misc.viewActiveBlockMarkerMoves()) {
                if (rt.getRemaining() > 0 && rt.getMax() > 0) {
                    float ratio = Mth.clamp(rt.getRemaining() / (float) rt.getMax(), 0.0F, 1.0F);
                    String sec = String.format(Locale.ROOT, "%.1fs", rt.getRemaining() / 20.0F);
                    String label = Component.translatable("jcraft.hud.gauge.marker").getString() + " " + sec;
                    drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, 0xCCF472B6, label);
                    yCursor -= BAR_STEP;
                }
            }
        }

        if (spec instanceof AnubisSpec) {
            float extra = misc.getAttackSpeedMult() - 1.0F;
            if (extra > 0.01F) {
                float ratio = Mth.clamp(extra / (AnubisSpec.BLOODLUST_MAX - 1.0F), 0.0F, 1.0F);
                int stacks = Mth.clamp(Math.round(extra / AnubisSpec.BLOODLUST_HIT_INCREMENT), 1, 5);
                String label = Component.translatable("jcraft.hud.gauge.bloodlust").getString() + " x" + stacks;
                drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, 0xCCDC2626, label);
                yCursor -= BAR_STEP;
            }
        } else if (spec instanceof RangerSpec) {
            final CommonGunslingerComponent gunslinger = JComponentPlatformUtils.getGunslinger(player);
            final float focus = gunslinger.getFocus();
            if (gunslinger.isFocusActive() || focus < RangerSpec.MAX_FOCUS - 0.1F) {
                float ratio = Mth.clamp(focus / RangerSpec.MAX_FOCUS, 0.0F, 1.0F);
                int color = gunslinger.isFocusActive() ? 0xCC22D3EE : 0xCC34D399;
                String label = Component.translatable("jcraft.hud.gauge.focus").getString() + " " + Math.round(ratio * 100.0F) + "%";
                drawGaugeBar(ctx, font, gaugeX, yCursor, ratio, color, label);
            }
        }
    }

    private static int drawPhdModePill(final GuiGraphicsExtractor ctx, final Font font, final int x, final int y, final int poisonIdx) {
        final int clamped = Mth.clamp(poisonIdx, 0, 2);
        final AbstractPurpleHazeEntity.PoisonType type = AbstractPurpleHazeEntity.PoisonType.values()[clamped];
        final String key = switch (type) {
            case HARMING -> "jcraft.hud.phd.mode.harming";
            case NULLIFYING -> "jcraft.hud.phd.mode.nullifying";
            case DEBILITATING -> "jcraft.hud.phd.mode.debilitating";
        };
        final int accent = switch (type) {
            case HARMING -> 0xFFC084FC;
            case NULLIFYING -> 0xFF38BDF8;
            case DEBILITATING -> 0xFFF472B6;
        };

        ctx.fill(x, y, x + GAUGE_WIDTH, y + 18, 0x770E0B16);
        ctx.outline(x, y, GAUGE_WIDTH, 18, accent & 0x66FFFFFF | 0x44000000);
        ctx.blit(RenderPipelines.GUI_TEXTURED, PH_INDICATOR, x + 2, y, 0.0F, clamped * 18.0F, 18, 18, 18, 54);

        final var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x + 23, y + 6);
        pose.scale(0.68F, 0.68F);
        ctx.text(font, Component.translatable(key), 0, 0, accent, true);
        pose.popMatrix();
        return y;
    }

    private static void drawGaugeBar(final GuiGraphicsExtractor ctx, final Font font, final int x, final int y,
                                     final float ratio, final int color, final String label) {
        ctx.fill(x, y, x + GAUGE_WIDTH, y + BAR_HEIGHT, 0x770E0B16);
        int fillW = Math.round((GAUGE_WIDTH - 2) * ratio);
        if (fillW > 0) {
            ctx.fill(x + 1, y + 1, x + 1 + fillW, y + BAR_HEIGHT - 1, color);
        }
        ctx.outline(x, y, GAUGE_WIDTH, BAR_HEIGHT, 0x33FFFFFF);
        final var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x + 3, y + 1);
        pose.scale(0.62F, 0.62F);
        ctx.text(font, label, 0, 0, 0xEEF9FAFB, true);
        pose.popMatrix();
    }
}
