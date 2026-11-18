package net.arna.jcraft.client.renderer.entity.ge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.NonNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FrogRenderer;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class GEFrogRenderer extends FrogRenderer {
    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState heldItemState = new ItemStackRenderState();

    public GEFrogRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void extractRenderState(@NonNull Frog frogEntity, @NonNull FrogRenderState state, float partialTick) {
        super.extractRenderState(frogEntity, state, partialTick);
        ItemStack stack = frogEntity.getItemBySlot(EquipmentSlot.MAINHAND);
        this.itemModelResolver.updateForLiving(this.heldItemState, stack, ItemDisplayContext.GROUND, frogEntity);
    }

    @Override
    public void submit(@NonNull FrogRenderState state, @NonNull PoseStack matrixStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        if (!this.heldItemState.isEmpty()) {
            matrixStack.pushPose();
            matrixStack.translate(0.0, 0.4, 0.0);
            matrixStack.rotate(Axis.YN.rotationDegrees(state.bodyRot));
            this.heldItemState.submit(matrixStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrixStack.popPose();
        }
        super.submit(state, matrixStack, collector, cameraState);
    }
}
