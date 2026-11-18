package net.arna.jcraft.client.rendering.handler;

import net.arna.jcraft.JCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class MandomRewindShaderHandler extends StandShaderHandler {
    public static final MandomRewindShaderHandler INSTANCE = new MandomRewindShaderHandler();
    public static final Identifier POST_EFFECT_ID = JCraft.id("bites_the_dust");

    public int duration = 0;
    public @Nullable LivingEntity shaderSourceEntity = null;

    @Override
    public void tick(final Minecraft client) {
        if (!shouldRender) {
            renderingEffect = false;
            return;
        }
        if (!renderingEffect) {
            ticks = 0;
            renderingEffect = true;
        }
        ticks++;
        if (ticks > duration) {
            renderingEffect = false;
            shouldRender = false;
        }
    }
}
