package net.arna.jcraft.client.rendering;

import net.arna.jcraft.client.JClientConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Manages Heavy/Ultimate impact frames (subtle edge flash + particle burst) and damped
 * micro camera-shake for visceral combat weight, configurable in JClientConfig.
 */
@Environment(EnvType.CLIENT)
public final class CombatImpactEffects {
    private static int shakeTicksLeft = 0;
    private static int shakeMaxTicks = 1;
    private static float shakeIntensity = 0.0f;
    private static float shakePhase = 0.0f;

    private static int flashTicksLeft = 0;
    private static int flashMaxTicks = 1;
    private static float flashIntensity = 0.0f;
    private static int flashRgb = 0xFFFFFF;
    private static boolean flashUltimate = false;

    private CombatImpactEffects() {}

    public static void trigger(final Vec3 worldPos, final float intensity, final int rgb, final boolean isUltimate) {
        if (!JClientConfig.getInstance().isImpactEffectsEnabled()) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        float atten = 1.0f;
        if (mc.player != null && worldPos != null) {
            double dist = mc.player.position().distanceTo(worldPos);
            if (dist > 24.0) {
                return;
            }
            atten = (float) Mth.clamp(1.0 - dist / 24.0, 0.12, 1.0);
        }

        if (mc.level != null && worldPos != null) {
            int cleanRgb = rgb & 0xFFFFFF;
            DustParticleOptions ringDust = new DustParticleOptions(cleanRgb, isUltimate ? 1.55f : 1.3f);
            int count = isUltimate ? 14 : 9;
            for (int i = 0; i < count; i++) {
                double ang = i * (Math.PI * 2.0 / count);
                double spd = (isUltimate ? 0.24 : 0.16);
                mc.level.addParticle(ringDust, worldPos.x, worldPos.y, worldPos.z,
                        Math.cos(ang) * spd, 0.04, Math.sin(ang) * spd);
                if (i % 2 == 0) {
                    mc.level.addParticle(ParticleTypes.CRIT, worldPos.x, worldPos.y, worldPos.z,
                            Math.cos(ang) * (spd * 1.4), 0.08, Math.sin(ang) * (spd * 1.4));
                }
            }
        }

        float scaled = Mth.clamp(intensity, 0.15f, 1.0f) * atten;
        int newShakeTicks = isUltimate ? 10 : 6;
        if (scaled >= shakeIntensity * 0.7f || shakeTicksLeft <= 0) {
            shakeTicksLeft = newShakeTicks;
            shakeMaxTicks = newShakeTicks;
            shakeIntensity = Math.max(shakeIntensity, scaled);
            shakePhase += 1.37f;
        }

        int newFlashTicks = isUltimate ? 5 : 3;
        flashTicksLeft = Math.max(flashTicksLeft, newFlashTicks);
        flashMaxTicks = Math.max(flashMaxTicks, newFlashTicks);
        flashIntensity = Math.max(flashIntensity, scaled);
        flashRgb = rgb & 0xFFFFFF;
        flashUltimate = flashUltimate || isUltimate;
    }

    public static void tick(final Minecraft client) {
        if (!JClientConfig.getInstance().isImpactEffectsEnabled()) {
            clear();
            return;
        }
        if (client != null && client.isPaused() && client.isLocalServer()) {
            return;
        }
        if (shakeTicksLeft > 0) {
            shakeTicksLeft--;
            if (shakeTicksLeft <= 0) {
                shakeIntensity = 0.0f;
            }
        }
        if (flashTicksLeft > 0) {
            flashTicksLeft--;
            if (flashTicksLeft <= 0) {
                flashIntensity = 0.0f;
                flashUltimate = false;
            }
        }
    }

    /**
     * Computes damped camera shake offsets in degrees: [pitchDeg, yawDeg, rollDeg].
     */
    public static float[] computeShakeAnglesDeg(final float partialTick) {
        return computeShakeAnglesDeg(
                JClientConfig.getInstance().isImpactEffectsEnabled(),
                JClientConfig.getInstance().getClampedScreenShakeStrength(),
                shakeTicksLeft, shakeMaxTicks, shakeIntensity, shakePhase, partialTick
        );
    }

    public static float[] computeShakeAnglesDeg(
            final boolean enabled, final float strengthMult,
            final int ticksLeft, final int maxTicks, final float intensity, final float phase, final float partialTick) {
        if (!enabled || strengthMult <= 0.001f || ticksLeft <= 0 || maxTicks <= 0 || intensity <= 0.001f) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        float t = Mth.clamp((ticksLeft - partialTick * 0.5f) / (float) maxTicks, 0.0f, 1.0f);
        float envelope = t * t * intensity * Mth.clamp(strengthMult, 0.0f, 2.0f);
        float arg = (maxTicks - ticksLeft + partialTick) * 1.95f + phase;
        float pitch = Mth.clamp(Mth.sin(arg * 1.35f) * 1.25f * envelope, -1.6f, 1.6f);
        float yaw = Mth.clamp(Mth.cos(arg * 1.15f) * 1.10f * envelope, -1.6f, 1.6f);
        float roll = Mth.clamp(Mth.sin(arg * 1.65f + 0.8f) * 1.05f * envelope, -1.4f, 1.4f);
        return new float[]{pitch, yaw, roll};
    }

    public static void renderHudOverlay(final GuiGraphicsExtractor ctx, final float partialTick) {
        if (flashTicksLeft <= 0 || !JClientConfig.getInstance().isImpactEffectsEnabled()) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final int sw = mc.getWindow().getGuiScaledWidth();
        final int sh = mc.getWindow().getGuiScaledHeight();

        float life = Mth.clamp((flashTicksLeft - partialTick * 0.5f) / (float) Math.max(1, flashMaxTicks), 0.0f, 1.0f);
        float maxAlpha = flashUltimate ? 0.24f : 0.14f;
        int alpha = Mth.clamp(Math.round(life * life * flashIntensity * maxAlpha * 255.0f), 0, 90);
        if (alpha <= 2) {
            return;
        }

        int frameColor = ARGB.color(alpha, flashRgb == 0x0C0C16 ? 0xDCE6FF : flashRgb);
        int border = flashUltimate ? 10 : 6;
        ctx.fill(0, 0, sw, border, frameColor);
        ctx.fill(0, sh - border, sw, sh, frameColor);
        ctx.fill(0, border, border, sh - border, frameColor);
        ctx.fill(sw - border, border, sw, sh - border, frameColor);
        ctx.outline(2, 2, sw - 4, sh - 4, ARGB.color(Math.min(255, alpha * 2), 0xFFFFFF));
    }

    public static boolean isActive() {
        return shakeTicksLeft > 0 || flashTicksLeft > 0;
    }

    public static void clear() {
        shakeTicksLeft = 0;
        shakeMaxTicks = 1;
        shakeIntensity = 0.0f;
        flashTicksLeft = 0;
        flashMaxTicks = 1;
        flashIntensity = 0.0f;
        flashUltimate = false;
    }
}
