package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.JCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class ZaWarudoShaderHandler extends StandShaderHandler {
    public static final ZaWarudoShaderHandler INSTANCE = new ZaWarudoShaderHandler();
    public static final Identifier POST_EFFECT_ID = JCraft.id("za_warudo");

    public float prevRadius = 0f;
    public float radius = 0f;
    public long effectLength = 0;
    public @Nullable LivingEntity shaderSourceEntity = null;

    public void reset() {
        shouldRender = false;
        renderingEffect = false;
        ticks = 0;
        prevRadius = 0f;
        radius = 0f;
        effectLength = 0;
        shaderSourceEntity = null;
    }

    @Override
    public void tick(final Minecraft client) {
        if (!shouldRender) {
            renderingEffect = false;
            return;
        }
        if (!renderingEffect) {
            ticks = 0;
            radius = 0f;
            renderingEffect = true;
        }
        ticks++;
        prevRadius = radius;
        float expansionRate = 4f;
        int inversion = 25;
        if (ticks < inversion) {
            radius += expansionRate;
        } else if (ticks < 2 * inversion) {
            radius -= 2 * expansionRate;
        }
        if (ticks > effectLength) {
            renderingEffect = false;
            shouldRender = false;
        }
    }
}
