package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.JCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class CrimsonShaderHandler extends StandShaderHandler {
    public static final CrimsonShaderHandler INSTANCE = new CrimsonShaderHandler();
    public static final Identifier POST_EFFECT_ID = JCraft.id("time_erase");

    public int effectLength = 0;
    public float prevRadius = 0f;
    public float radius = 0f;
    public @Nullable LivingEntity shaderSourceEntity = null;

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
        if (ticks > effectLength) {
            renderingEffect = false;
            shouldRender = false;
        }
    }
}
