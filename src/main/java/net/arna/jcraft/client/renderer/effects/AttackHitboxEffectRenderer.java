package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Disabled attack hitbox wireframe outline renderer (kept as a no-op API stub).
 */
public class AttackHitboxEffectRenderer {
    public record TimedHitbox(AABB box, long expiresAtMs, boolean splatter) {}

    public static void init() {}

    public static void addHitboxes(final Collection<AABB> boxes) {}

    public static void addHitbox(final AABB box, final long durationMs, final boolean splatter) {}

    public static List<TimedHitbox> getActiveHitboxes() {
        return Collections.emptyList();
    }

    public static void render(final PoseStack matrices, final Vec3 camPos, final SubmitNodeCollector collector) {}
}
