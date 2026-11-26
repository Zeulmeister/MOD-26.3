package net.arna.jcraft.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.JClientConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders subtle aura-colored afterimages and dust trails during dashes, Time Skip warps,
 * and Heavy/Ultimate lunges without obstructing first-person vision.
 */
@Environment(EnvType.CLIENT)
public final class AuraAfterimageManager {
    private static final Identifier RUNE_TEX = JCraft.id("textures/effect/stand_runic_circle.png");
    private static final int MAX_GHOSTS = 48;

    private static final class TrailState {
        final int entityId;
        final int rgb;
        int ticksRemaining;
        int sampleCooldown = 0;

        TrailState(int entityId, int rgb, int ticksRemaining) {
            this.entityId = entityId;
            this.rgb = rgb;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class GhostEcho {
        final int entityId;
        final Vec3 pos;
        final int rgb;
        int age;
        final int maxAge;

        GhostEcho(int entityId, Vec3 pos, int rgb, int maxAge) {
            this.entityId = entityId;
            this.pos = pos;
            this.rgb = rgb;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    private static final Map<Integer, TrailState> ACTIVE_TRAILS = new HashMap<>();
    private static final List<GhostEcho> GHOSTS = new ArrayList<>();

    private AuraAfterimageManager() {}

    public static void triggerTrail(final int entityId, final int rgb, final int durationTicks) {
        if (!JClientConfig.getInstance().isAuraAfterimagesEnabled() || durationTicks <= 0) {
            return;
        }
        ACTIVE_TRAILS.put(entityId, new TrailState(entityId, rgb & 0xFFFFFF, Math.min(durationTicks, 30)));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity ent = mc.level.getEntity(entityId);
            if (ent instanceof LivingEntity) {
                addGhost(entityId, ent.position(), rgb & 0xFFFFFF, 10);
            }
        }
    }

    public static void triggerWarp(final int entityId, final int rgb, final Vec3 fromPos, final Vec3 toPos) {
        if (!JClientConfig.getInstance().isAuraAfterimagesEnabled()) {
            return;
        }
        int cleanRgb = rgb & 0xFFFFFF;
        addGhost(entityId, fromPos, cleanRgb, 12);
        addGhost(entityId, fromPos.lerp(toPos, 0.45), cleanRgb, 10);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            int particleRgb = cleanRgb == 0x0C0C16 ? 0x08080E : cleanRgb;
            DustParticleOptions dust = new DustParticleOptions(particleRgb, 1.35f);
            for (int i = 0; i <= 6; i++) {
                Vec3 p = fromPos.lerp(toPos, i / 6.0);
                mc.level.addParticle(dust, p.x, p.y + 0.9, p.z, 0.0, 0.03, 0.0);
            }
        }
    }

    private static void addGhost(int entityId, Vec3 pos, int rgb, int maxAge) {
        if (GHOSTS.size() >= MAX_GHOSTS) {
            GHOSTS.remove(0);
        }
        GHOSTS.add(new GhostEcho(entityId, pos, rgb, maxAge));
    }

    public static void tick(final Minecraft client) {
        if (client == null || client.level == null || !JClientConfig.getInstance().isAuraAfterimagesEnabled()) {
            ACTIVE_TRAILS.clear();
            GHOSTS.clear();
            return;
        }
        if (client.isPaused() && client.isLocalServer()) {
            return;
        }

        Iterator<Map.Entry<Integer, TrailState>> trailIt = ACTIVE_TRAILS.entrySet().iterator();
        while (trailIt.hasNext()) {
            TrailState ts = trailIt.next().getValue();
            Entity ent = client.level.getEntity(ts.entityId);
            if (!(ent instanceof LivingEntity living) || !living.isAlive() || ts.ticksRemaining <= 0) {
                trailIt.remove();
                continue;
            }
            if (ts.sampleCooldown <= 0) {
                ts.sampleCooldown = 2;
                addGhost(ts.entityId, living.position(), ts.rgb, 10);
                int particleRgb = ts.rgb == 0x0C0C16 ? 0x090910 : ts.rgb;
                DustParticleOptions dust = new DustParticleOptions(particleRgb, 1.15f);
                client.level.addParticle(dust, living.getX(), living.getY() + 0.85, living.getZ(), 0.0, 0.02, 0.0);
                if (ts.rgb == 0x0C0C16) {
                    client.level.addParticle(new DustParticleOptions(0xDCE6FF, 0.85f),
                            living.getX(), living.getY() + 1.05, living.getZ(), 0.0, 0.025, 0.0);
                }
            } else {
                ts.sampleCooldown--;
            }
            ts.ticksRemaining--;
        }

        Iterator<GhostEcho> ghostIt = GHOSTS.iterator();
        while (ghostIt.hasNext()) {
            GhostEcho ghost = ghostIt.next();
            ghost.age++;
            if (ghost.age >= ghost.maxAge) {
                ghostIt.remove();
            }
        }
    }

    public static void renderAll(final PoseStack stack, final Vec3 camPos, final ClientLevel world, final float tickDelta,
                                 final SubmitNodeCollector collector, final CameraRenderState cameraState) {
        if (world == null || collector == null || GHOSTS.isEmpty() || !JClientConfig.getInstance().isAuraAfterimagesEnabled()) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        final boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;

        for (final GhostEcho ghost : GHOSTS) {
            final Entity entity = world.getEntity(ghost.entityId);
            if (entity == null || !entity.isAlive() || entity.isInvisible()) {
                continue;
            }
            final double distSq = entity.position().distanceToSqr(ghost.pos);
            if (distSq < 0.09) {
                continue;
            }
            if (firstPerson && entity == mc.player && ghost.pos.distanceToSqr(camPos) < 1.25) {
                continue;
            }

            final float life = Mth.clamp(1.0f - (ghost.age + tickDelta) / (float) ghost.maxAge, 0.0f, 1.0f);
            final int alpha = Mth.clamp(Math.round(life * 175.0f), 0, 255);
            if (alpha <= 6) {
                continue;
            }

            final Vec3 rel = ghost.pos.subtract(camPos);
            final EntityRenderState state = dispatcher.extractEntity(entity, tickDelta);
            state.shadowPieces.clear();
            dispatcher.submit(state, cameraState, rel.x, rel.y, rel.z, stack, collector);

            // Subtle ground aura pulse ring in exact skin/Chroma aura color
            final boolean abyssShadow = ghost.rgb == 0x0C0C16;
            final int r = abyssShadow ? 14 : ((ghost.rgb >> 16) & 0xFF);
            final int g = abyssShadow ? 14 : ((ghost.rgb >> 8) & 0xFF);
            final int b = abyssShadow ? 24 : (ghost.rgb & 0xFF);
            final float half = 0.65f + (1.0f - life) * 0.25f;
            final int ringAlpha = Mth.clamp(Math.round(life * 145.0f), 0, 255);

            stack.pushPose();
            stack.translate(rel.x, rel.y + 0.03, rel.z);
            final var renderType = abyssShadow
                    ? RenderTypes.entityTranslucent(RUNE_TEX)
                    : RenderTypes.entityTranslucentEmissive(RUNE_TEX);
            collector.submitCustomGeometry(stack, renderType, (pose, vc) -> {
                Matrix4f mat = pose.pose();
                emitQuadVertex(vc, mat, -half, 0.0f, -half, 0.0f, 0.0f, r, g, b, ringAlpha);
                emitQuadVertex(vc, mat, -half, 0.0f, half, 0.0f, 1.0f, r, g, b, ringAlpha);
                emitQuadVertex(vc, mat, half, 0.0f, half, 1.0f, 1.0f, r, g, b, ringAlpha);
                emitQuadVertex(vc, mat, half, 0.0f, -half, 1.0f, 0.0f, r, g, b, ringAlpha);
            });
            stack.popPose();
        }
    }

    private static void emitQuadVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                                       float u, float v, int r, int g, int b, int a) {
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728640)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    public static void clear() {
        ACTIVE_TRAILS.clear();
        GHOSTS.clear();
    }

    public static int getActiveGhostCount() {
        return GHOSTS.size();
    }
}
