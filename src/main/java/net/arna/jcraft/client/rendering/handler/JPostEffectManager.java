package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.util.JClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates native Minecraft 26.3 RenderPearl post_effect shaders for JCraft.
 */
public final class JPostEffectManager {
    private JPostEffectManager() {}

    public static List<Identifier> collectActiveEffects(List<Identifier> vanillaRequested) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return vanillaRequested;
        }

        boolean zaWarudo = ZaWarudoShaderHandler.INSTANCE.renderingEffect
                || JClientUtils.isInTSRange(mc.player);
        boolean timeErase = (CrimsonShaderHandler.INSTANCE.renderingEffect && JClientConfig.getInstance().isTimeEraseShader())
                || EpitaphVignetteShaderHandler.INSTANCE.renderingEffect;
        boolean btd = MandomRewindShaderHandler.INSTANCE.renderingEffect;
        boolean colorShift = (ColorShiftShaderHandler.INSTANCE.renderingEffect && JClientConfig.getInstance().isColorShiftEnabled())
                || RangerFocusShaderHandler.INSTANCE.renderingEffect;

        if (!zaWarudo && !timeErase && !btd && !colorShift) {
            return vanillaRequested;
        }

        List<Identifier> merged = new ArrayList<>(vanillaRequested);
        if (zaWarudo && !merged.contains(ZaWarudoShaderHandler.POST_EFFECT_ID)) {
            merged.add(ZaWarudoShaderHandler.POST_EFFECT_ID);
        }
        if (timeErase && !merged.contains(CrimsonShaderHandler.POST_EFFECT_ID)) {
            merged.add(CrimsonShaderHandler.POST_EFFECT_ID);
        }
        if (btd && !merged.contains(MandomRewindShaderHandler.POST_EFFECT_ID)) {
            merged.add(MandomRewindShaderHandler.POST_EFFECT_ID);
        }
        if (colorShift && !merged.contains(ColorShiftShaderHandler.POST_EFFECT_ID)) {
            merged.add(ColorShiftShaderHandler.POST_EFFECT_ID);
        }
        return merged;
    }
}
