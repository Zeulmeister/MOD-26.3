package net.arna.jcraft.client.renderer.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

/**
 * Renders a golden hyper-armor sheen on entities executing armored Stand/Spec moves or absorbing armored hits.
 */
public class ArmoredMoveFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final Identifier OVERLAY_TEX = JCraft.id("textures/gui/full_gauge.png");

    public ArmoredMoveFeatureRenderer(final RenderLayerParent<S, M> parent) {
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
        boolean armoredHit = misc != null && misc.getArmoredHitTicks() > 0;
        boolean armoredMove = false;
        final StandEntity<?, ?> stand = JUtils.getStand(entity);
        if (stand != null && stand.getCurrentMove() != null && stand.getCurrentMove().getArmor() > 0) {
            armoredMove = true;
        } else {
            final JSpec<?, ?> spec = JUtils.getSpec(entity);
            if (spec != null && spec.getCurrentMove() != null && spec.getCurrentMove().getArmor() > 0) {
                armoredMove = true;
            }
        }
        if (!armoredHit && !armoredMove) {
            return;
        }

        final int tint = armoredHit ? ARGB.color(140, 255, 240, 120) : ARGB.color(75, 250, 204, 21);
        poseStack.pushPose();
        poseStack.scale(1.035f, 1.035f, 1.035f);
        collector.submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.entityTranslucentEmissive(OVERLAY_TEX),
                0x00F000F0,
                OverlayTexture.NO_OVERLAY,
                tint,
                null,
                state.outlineColor
        );
        poseStack.popPose();
    }

    public static void prepareHamonAura() {}
    public static void prepareHamonAura(LivingEntity entity) {}
}
