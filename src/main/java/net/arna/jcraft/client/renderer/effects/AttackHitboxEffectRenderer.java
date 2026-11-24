package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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
            if (b != null) {
                ACTIVE.add(new TimedHitbox(b, exp, false));
            }
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

    public static void render(final PoseStack matrices, final Vec3 camPos, final SubmitNodeCollector collector) {
        if (collector == null || ACTIVE.isEmpty()) {
            return;
        }
        final List<TimedHitbox> active = getActiveHitboxes();
        if (active.isEmpty()) {
            return;
        }

        boolean anyAttackBox = false;
        for (TimedHitbox h : active) {
            if (!h.splatter()) {
                anyAttackBox = true;
                break;
            }
        }
        if (!anyAttackBox) {
            return;
        }

        collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, vc) -> {
            final Matrix4f mat = pose.pose();
            for (final TimedHitbox th : active) {
                if (th.splatter()) {
                    continue;
                }
                final AABB b = th.box();
                float x0 = (float) (b.minX - camPos.x), y0 = (float) (b.minY - camPos.y), z0 = (float) (b.minZ - camPos.z);
                float x1 = (float) (b.maxX - camPos.x), y1 = (float) (b.maxY - camPos.y), z1 = (float) (b.maxZ - camPos.z);
                int r = 255, g = 80, bl = 80, a = 220;
                // Bottom rectangle
                line(vc, mat, x0, y0, z0, x1, y0, z0, r, g, bl, a);
                line(vc, mat, x1, y0, z0, x1, y0, z1, r, g, bl, a);
                line(vc, mat, x1, y0, z1, x0, y0, z1, r, g, bl, a);
                line(vc, mat, x0, y0, z1, x0, y0, z0, r, g, bl, a);
                // Top rectangle
                line(vc, mat, x0, y1, z0, x1, y1, z0, r, g, bl, a);
                line(vc, mat, x1, y1, z0, x1, y1, z1, r, g, bl, a);
                line(vc, mat, x1, y1, z1, x0, y1, z1, r, g, bl, a);
                line(vc, mat, x0, y1, z1, x0, y1, z0, r, g, bl, a);
                // Vertical edges
                line(vc, mat, x0, y0, z0, x0, y1, z0, r, g, bl, a);
                line(vc, mat, x1, y0, z0, x1, y1, z0, r, g, bl, a);
                line(vc, mat, x1, y0, z1, x1, y1, z1, r, g, bl, a);
                line(vc, mat, x0, y0, z1, x0, y1, z1, r, g, bl, a);
            }
        });
    }

    private static void line(VertexConsumer vc, Matrix4f mat, float x0, float y0, float z0,
                             float x1, float y1, float z1, int r, int g, int b, int a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-5f) len = 1.0f;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(mat, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(2.0f);
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(2.0f);
    }
}
