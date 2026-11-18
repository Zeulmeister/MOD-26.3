package net.arna.jcraft.client.zoom;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Smooth 5x (80% FOV reduction -> 0.20x multiplier) Precision Vision Zoom (default key: Z).
 */
public final class StarPlatinumPrecisionHandler {
    public static final float TARGET_FOV_MULTIPLIER = 0.20F;
    private static boolean precisionActive = false;
    private static float zoomProgress = 0.0F;
    private static float prevZoomProgress = 0.0F;

    private StarPlatinumPrecisionHandler() {}

    public static boolean isPrecisionActive() {
        return precisionActive;
    }

    public static float getZoomProgress(float partialTicks) {
        return Mth.lerp(partialTicks, prevZoomProgress, zoomProgress);
    }

    /**
     * Pure math helper computing FOV multiplier from [0..1] zoom progress (1.0 -> 0.20).
     */
    public static double computeFovMultiplier(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        // Smooth cubic ease-out curve
        float eased = 1.0F - (1.0F - clamped) * (1.0F - clamped) * (1.0F - clamped);
        return Mth.lerp(eased, 1.0D, TARGET_FOV_MULTIPLIER);
    }

    public static float modifyFov(float baseFov, float partialTicks) {
        float progress = getZoomProgress(partialTicks);
        if (progress <= 0.001F) {
            return baseFov;
        }
        return (float) (baseFov * computeFovMultiplier(progress));
    }

    public static void tick(Minecraft client) {
        prevZoomProgress = zoomProgress;
        LocalPlayer player = client.player;
        if (player != null && player.isAlive() && client.gui.screen() == null) {
            StandEntity<?, ?> stand = JUtils.getStand(player);
            boolean hasStand = stand != null && stand.isAlive();
            boolean isKeyDown = JCraftClient.PRECISION_VISION_KEY != null && JCraftClient.PRECISION_VISION_KEY.isDown();
            boolean nextActive = hasStand && isKeyDown;
            if (nextActive && !precisionActive) {
                player.playSound(SoundEvents.SPYGLASS_USE, 0.55F, 1.25F);
            } else if (!nextActive && precisionActive) {
                player.playSound(SoundEvents.SPYGLASS_STOP_USING, 0.45F, 1.15F);
            }
            precisionActive = nextActive;
            if (precisionActive) {
                zoomProgress = Math.min(1.0F, zoomProgress + 0.28F);
            } else {
                zoomProgress = Math.max(0.0F, zoomProgress - 0.28F);
            }
        } else {
            precisionActive = false;
            zoomProgress = 0.0F;
            prevZoomProgress = 0.0F;
        }
    }
}
