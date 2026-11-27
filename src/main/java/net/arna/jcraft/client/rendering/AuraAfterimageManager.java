package net.arna.jcraft.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.JClientConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Renders frozen-pose translucent aura-tinted afterimages and dust trails during dashes,
 * Time Skip warps, and Heavy/Ultimate lunges without obstructing first-person vision.
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
        final @Nullable EntityRenderState frozenState;
        int age;
        final int maxAge;

        GhostEcho(int entityId, Vec3 pos, int rgb, int maxAge, @Nullable EntityRenderState frozenState) {
            this.entityId = entityId;
            this.pos = pos;
            this.rgb = rgb;
            this.maxAge = maxAge;
            this.frozenState = frozenState;
            this.age = 0;
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
        EntityRenderState frozen = captureFrozenState(entityId);
        GHOSTS.add(new GhostEcho(entityId, pos, rgb, maxAge, frozen));
    }

    private static @Nullable EntityRenderState captureFrozenState(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null || !entity.isAlive() || entity.isInvisible()) {
            return null;
        }
        EntityRenderState state = mc.getEntityRenderDispatcher().extractEntity(entity, 1.0f);
        if (state != null) {
            state.shadowPieces.clear();
            state.nameTag = null;
            state.scoreText = null;
            state.displayFireAnimation = false;
            state.outlineColor = 0;
            if (state instanceof LivingEntityRenderState livingState) {
                // Triggers LivingEntityRenderer's entityTranslucentCull render type for proper alpha fading.
                livingState.isInvisible = true;
                livingState.isInvisibleToPlayer = false;
            }
        }
        return state;
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
            if (ghost.frozenState == null) {
                continue;
            }
            final Entity entity = world.getEntity(ghost.entityId);
            if (entity != null) {
                if (entity.position().distanceToSqr(ghost.pos) < 0.09) {
                    continue;
                }
                if (firstPerson && entity == mc.player && ghost.pos.distanceToSqr(camPos) < 1.25) {
                    continue;
                }
            }

            final float life = Mth.clamp(1.0f - (ghost.age + tickDelta) / (float) ghost.maxAge, 0.0f, 1.0f);
            final int alpha = Mth.clamp(Math.round(life * 165.0f), 0, 255);
            if (alpha <= 6) {
                continue;
            }

            final boolean abyssShadow = ghost.rgb == 0x0C0C16;
            final int baseR = abyssShadow ? 26 : ((ghost.rgb >> 16) & 0xFF);
            final int baseG = abyssShadow ? 26 : ((ghost.rgb >> 8) & 0xFF);
            final int baseB = abyssShadow ? 44 : (ghost.rgb & 0xFF);
            final int tintR = abyssShadow ? baseR : Mth.clamp(Math.round(baseR * 0.65f + 255 * 0.35f), 0, 255);
            final int tintG = abyssShadow ? baseG : Mth.clamp(Math.round(baseG * 0.65f + 255 * 0.35f), 0, 255);
            final int tintB = abyssShadow ? baseB : Mth.clamp(Math.round(baseB * 0.65f + 255 * 0.35f), 0, 255);
            final int ghostArgb = (alpha << 24) | (tintR << 16) | (tintG << 8) | tintB;

            final Vec3 rel = ghost.pos.subtract(camPos);
            final SubmitNodeCollector ghostCollector = new GhostSubmitCollector(collector, ghostArgb);
            dispatcher.submit(ghost.frozenState, cameraState, rel.x, rel.y, rel.z, stack, ghostCollector);

            // Subtle ground aura pulse ring in exact skin/Chroma aura color
            final int ringR = abyssShadow ? 14 : baseR;
            final int ringG = abyssShadow ? 14 : baseG;
            final int ringB = abyssShadow ? 24 : baseB;
            final float half = 0.65f + (1.0f - life) * 0.25f;
            final int ringAlpha = Mth.clamp(Math.round(life * 145.0f), 0, 255);

            stack.pushPose();
            stack.translate(rel.x, rel.y + 0.03, rel.z);
            final var renderType = abyssShadow
                    ? RenderTypes.entityTranslucent(RUNE_TEX)
                    : RenderTypes.entityTranslucentEmissive(RUNE_TEX);
            collector.submitCustomGeometry(stack, renderType, (pose, vc) -> {
                Matrix4f mat = pose.pose();
                emitQuadVertex(vc, mat, -half, 0.0f, -half, 0.0f, 0.0f, ringR, ringG, ringB, ringAlpha);
                emitQuadVertex(vc, mat, -half, 0.0f, half, 0.0f, 1.0f, ringR, ringG, ringB, ringAlpha);
                emitQuadVertex(vc, mat, half, 0.0f, half, 1.0f, 1.0f, ringR, ringG, ringB, ringAlpha);
                emitQuadVertex(vc, mat, half, 0.0f, -half, 1.0f, 0.0f, ringR, ringG, ringB, ringAlpha);
            });
            stack.popPose();
        }
    }

    private static int multiplyArgb(int c1, int c2) {
        int a = (((c1 >>> 24) & 0xFF) * ((c2 >>> 24) & 0xFF)) / 255;
        int r = (((c1 >>> 16) & 0xFF) * ((c2 >>> 16) & 0xFF)) / 255;
        int g = (((c1 >>> 8) & 0xFF) * ((c2 >>> 8) & 0xFF)) / 255;
        int b = ((c1 & 0xFF) * (c2 & 0xFF)) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record GhostSubmitCollector(OrderedSubmitNodeCollector delegate, int ghostArgb) implements SubmitNodeCollector {
        @Override
        public OrderedSubmitNodeCollector order(int order) {
            OrderedSubmitNodeCollector sub = delegate instanceof SubmitNodeCollector snc ? snc.order(order) : delegate;
            return new GhostSubmitCollector(sub, ghostArgb);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                    int lightCoords, int overlayCoords, int tintedColor,
                                    @Nullable UvMapping uvMapping, int outlineColor) {
            if (renderType != null && renderType.hasBlending()) {
                delegate.submitModel(model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY,
                        multiplyArgb(tintedColor, ghostArgb), uvMapping, 0);
            }
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
            if (renderType != null && renderType.hasBlending()) {
                delegate.submitCustomGeometry(poseStack, renderType,
                        (pose, vc) -> renderer.render(pose, new TintedVertexConsumer(vc, ghostArgb)));
            }
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

        @Override
        public void submitNameTag(PoseStack poseStack, Vec3 offset, int y, Component text, boolean seeThrough, int backgroundColor, CameraRenderState camera) {}

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode mode, int packedLight, int color, int backgroundColor, int outlineColor) {}

        @Override
        public void submitTextBackground(PoseStack poseStack, float minX, float minY, float maxX, float maxY, int color, Font.DisplayMode mode, int packedLight) {}

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {}

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState state) {}

        @Override
        public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay overlay) {}

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int packedLight) {}

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tints, int packedLight, int packedOverlay, int outlineColor) {}

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int packedLight, boolean crumbling) {}

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float lineWidth, boolean depthTest) {}

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext context, int lightCoords, int overlayCoords, int outlineColor, int[] tints, ItemQuads quads, ItemStackRenderState.FoilType foil) {}

        @Override
        public void submitQuadParticleGroup(QuadParticleRenderState state) {}

        @Override
        public void submitGizmoPrimitives(net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean depth) {}
    }

    private static final class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int tintR, tintG, tintB, tintA, ghostArgb;

        TintedVertexConsumer(VertexConsumer delegate, int ghostArgb) {
            this.delegate = delegate;
            this.ghostArgb = ghostArgb;
            this.tintA = (ghostArgb >>> 24) & 0xFF;
            this.tintR = (ghostArgb >>> 16) & 0xFF;
            this.tintG = (ghostArgb >>> 8) & 0xFF;
            this.tintB = ghostArgb & 0xFF;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            delegate.addVertex(x, y, z, multiplyArgb(color, ghostArgb), u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor((r * tintR) / 255, (g * tintG) / 255, (b * tintB) / 255, (a * tintA) / 255);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            delegate.setColor(multiplyArgb(argb, ghostArgb));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            delegate.setUv3(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            delegate.setLineWidth(width);
            return this;
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