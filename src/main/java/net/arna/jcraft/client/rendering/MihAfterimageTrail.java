package net.arna.jcraft.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arna.jcraft.api.stand.ITimeAccelCapable;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-entity ring buffer of recent world positions, used to draw the Made In Heaven afterimage along the path the user
 * actually travelled (so the copies trail out of the player instead of snapping to fixed offsets). Sampled once per
 * game tick; rendered natively through MC 26.3 SubmitNodeCollector.
 */
public class MihAfterimageTrail {
    private static final Map<LivingEntity, MihAfterimageTrail> TRAILS = new WeakHashMap<>();

    private final Vec3[] positions; // index 0 = most recent
    private int size = 0;
    private int lastTick = Integer.MIN_VALUE;

    private MihAfterimageTrail(final int capacity) {
        this.positions = new Vec3[capacity];
    }

    public static MihAfterimageTrail get(final LivingEntity entity, final int capacity) {
        return TRAILS.computeIfAbsent(entity, e -> new MihAfterimageTrail(capacity));
    }

    public boolean sample(final int tick, final Vec3 position) {
        if (tick == lastTick) {
            return false;
        }
        if (lastTick != Integer.MIN_VALUE && tick - lastTick != 1) {
            size = 0;
        }
        lastTick = tick;
        for (int i = positions.length - 1; i > 0; i--) {
            positions[i] = positions[i - 1];
        }
        positions[0] = position;
        if (size < positions.length) {
            size++;
        }
        return true;
    }

    public Vec3 at(final int index) {
        return index >= 0 && index < size ? positions[index] : null;
    }

    public static void renderAll(final PoseStack stack, final Vec3 camPos, final ClientLevel world, final float tickDelta,
                                 final SubmitNodeCollector collector, final CameraRenderState cameraState) {
        if (world == null || collector == null || JClientConfig.getInstance().isDisableMihAfterimages()) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        for (final Player player : world.players()) {
            if (player == null || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (!(JUtils.getStand(player) instanceof ITimeAccelCapable ta)
                    || ta.getAccelTime() <= 0 || ta.getSpeedometer() < 2) {
                continue;
            }
            final MihAfterimageTrail trail = get(player, 10);
            trail.sample(player.tickCount, player.position());

            final EntityRenderState state = dispatcher.extractEntity(player, tickDelta);
            for (int idx : new int[]{2, 5, 8}) {
                final Vec3 ghostPos = trail.at(idx);
                if (ghostPos == null || ghostPos.distanceToSqr(player.position()) < 0.04) {
                    continue;
                }
                final Vec3 rel = ghostPos.subtract(camPos);
                dispatcher.submit(state, cameraState, rel.x, rel.y, rel.z, stack, collector);
            }
        }
    }
}
