package net.arna.jcraft.client.renderer.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.events.client.ClientTickEvent;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.common.attack.moves.kingcrimson.PredictionMove;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class TimeErasePredictionEffectRenderer {
    private static int ticksLeft = -1;
    private static final Map<Entity, Vec3> predictions = new WeakHashMap<>();

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (ticksLeft < 0) {
                predictions.clear();
                return;
            }

            if (!Minecraft.getInstance().isPaused()) {
                ticksLeft--;
            }

            synchronized (predictions) {
                updatePredictions();
                if (client.level != null && ticksLeft >= 0) {
                    DustParticleOptions crimsonDust = new DustParticleOptions(0xFF1A33, 1.15f);
                    for (Map.Entry<Entity, Vec3> entry : predictions.entrySet()) {
                        Entity e = entry.getKey();
                        Vec3 pred = entry.getValue();
                        if (e != null && e.isAlive() && pred != null) {
                            client.level.addParticle(crimsonDust, pred.x, pred.y + e.getBbHeight() * 0.5, pred.z, 0, 0.02, 0);
                        }
                    }
                }
            }
        });
    }

    public static void startEffect(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be at least 1.");
        }
        ticksLeft = length;

        final Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        for (final Entity entity : PredictionMove.getEntitiesToCatch(client.level, JCraftClient.getStandEntity(), client.player)) {
            predictions.put(entity, entity.position());
        }
    }

    public static void stopEffect() {
        ticksLeft = -1;
        predictions.clear();
    }

    public static boolean isActive() {
        return ticksLeft >= 0;
    }

    public static Map<Entity, Vec3> getPredictions() {
        return Collections.unmodifiableMap(predictions);
    }

    public static void render(final PoseStack stack, final Vec3 camPos, final ClientLevel world, final float tickDelta,
                              final SubmitNodeCollector collector, final CameraRenderState cameraState) {
        if (ticksLeft < 0) {
            return;
        }
        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        synchronized (predictions) {
            for (final Map.Entry<Entity, Vec3> prediction : predictions.entrySet()) {
                final Entity entity = prediction.getKey();
                final Vec3 predPos = prediction.getValue();
                if (entity == null || !entity.isAlive() || predPos == null) {
                    continue;
                }
                final Vec3 rel = predPos.subtract(camPos);
                EntityRenderState state = dispatcher.extractEntity(entity, tickDelta);
                dispatcher.submit(state, cameraState, rel.x, rel.y - 0.05, rel.z, stack, collector);
            }
        }
    }

    private static void updatePredictions() {
        PredictionMove.updatePredictions(predictions, ticksLeft);
    }
}
