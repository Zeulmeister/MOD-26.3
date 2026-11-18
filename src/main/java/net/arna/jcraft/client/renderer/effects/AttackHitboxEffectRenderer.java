package net.arna.jcraft.client.renderer.effects;

import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AttackHitboxEffectRenderer {
    public record TimedHitbox(AABB box, long expiresAtMs, boolean splatter) {}

    private static final List<TimedHitbox> ACTIVE = new CopyOnWriteArrayList<>();

    public static void init() {}

    public static void addHitboxes(final Collection<AABB> boxes) {
        if (boxes == null) return;
        long exp = System.currentTimeMillis() + 750L;
        for (AABB b : boxes) {
            ACTIVE.add(new TimedHitbox(b, exp, false));
        }
    }

    public static void addHitbox(final AABB box, final long durationMs, final boolean splatter) {
        if (box == null) return;
        ACTIVE.add(new TimedHitbox(box, System.currentTimeMillis() + durationMs, splatter));
    }

    public static List<TimedHitbox> getActiveHitboxes() {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(h -> h.expiresAtMs() < now);
        return ACTIVE;
    }
}
