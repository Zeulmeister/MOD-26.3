package net.arna.jcraft.client.renderer.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * Renders stuck DIO/The World throwing knives embedded in living entities.
 */
public class StuckKnivesFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final Identifier KNIFE_TEX = JCraft.id("textures/item/knife.png");
    private static final float[][] OFFSETS = {
            {0.18f, 0.25f, -0.18f, 25f, -15f},
            {-0.20f, 0.38f, -0.16f, -30f, 18f},
            {0.12f, 0.55f, 0.17f, 155f, 12f},
            {-0.15f, 0.15f, 0.16f, -145f, -20f},
            {0.22f, 0.62f, -0.10f, 55f, 25f},
            {-0.22f, 0.48f, 0.12f, -110f, -10f},
            {0.05f, 0.30f, -0.19f, 10f, -28f},
            {-0.08f, 0.68f, -0.15f, -15f, 22f},
            {0.25f, 0.42f, 0.04f, 85f, -18f},
            {-0.24f, 0.22f, -0.05f, -75f, 15f}
    };

    public StuckKnivesFeatureRenderer(final RenderLayerParent<S, M> parent) {
        super(parent);
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final int packedLight,
                       final S state, final float yRot, final float xRot) {
        final LivingEntity entity = ((IJCraftEntityRenderStateHolder) state).jcraft$getLivingEntity();
        if (entity == null || entity.isInvisible()) {
            return;
        }
        final CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(entity);
        if (misc == null) {
            return;
        }
        final int knives = Math.min(misc.getStuckKnifeCount(), OFFSETS.length);
        if (knives <= 0) {
            return;
        }

        for (int i = 0; i < knives; i++) {
            final float[] o = OFFSETS[i];
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.rotateDegrees(Axis.YP, o[3]);
            poseStack.rotateDegrees(Axis.XP, o[4]);
            poseStack.scale(0.35f, 0.35f, 0.35f);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(KNIFE_TEX), (pose, vc) -> {
                final Matrix4f mat = pose.pose();
                // Front & back crossed blade quads
                quad(vc, mat, -0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, packedLight);
                quad(vc, mat, 0.5f, -0.5f, 0.0f, -0.5f, 0.5f, 0.0f, packedLight);
            });
            poseStack.popPose();
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, float x0, float y0, float z,
                             float x1, float y1, float z1, int light) {
        vc.addVertex(mat, x0, y0, z).setColor(255, 255, 255, 255).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        vc.addVertex(mat, x1, y0, z1).setColor(255, 255, 255, 255).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        vc.addVertex(mat, x1, y1, z1).setColor(255, 255, 255, 255).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
        vc.addVertex(mat, x0, y1, z).setColor(255, 255, 255, 255).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 0f, 1f);
    }

    public static void prepareHamonAura() {}
    public static void prepareHamonAura(LivingEntity entity) {}
}
