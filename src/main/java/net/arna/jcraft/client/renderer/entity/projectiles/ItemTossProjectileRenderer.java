package net.arna.jcraft.client.renderer.entity.projectiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.NonNull;
import net.arna.jcraft.common.entity.projectile.ItemTossProjectile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public class ItemTossProjectileRenderer extends EntityRenderer<ItemTossProjectile, ItemTossProjectileRenderer.TossRenderState> {
    private final ItemModelResolver itemModelResolver;

    public static class TossRenderState extends ThrownItemRenderState {
        public float yRot;
        public float spinAngle;
    }

    public ItemTossProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public TossRenderState createRenderState() {
        return new TossRenderState();
    }

    @Override
    public void extractRenderState(@NonNull ItemTossProjectile entity, @NonNull TossRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getYRot();
        state.spinAngle = Mth.lerp(partialTick, entity.getPrevSpinAngle(), entity.getSpinAngle());
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.FIXED, entity);
    }

    @Override
    public void submit(@NonNull TossRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(state.yRot - 90f));
        poseStack.rotate(Axis.ZP.rotationDegrees(-state.spinAngle));
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
