package net.arna.jcraft.client.renderer.entity.projectiles;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.NonNull;
import mod.azure.azurelib.render.entity.AzEntityRenderState;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer;
import net.arna.jcraft.common.entity.projectile.BlockProjectile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BlockProjectileRenderer extends AbstractEntityRenderer<BlockProjectile> {
    public static final String ID = "block";
    private static final RenderType RENDER_TYPE = RenderTypes.eyes(JCraft.id(TEXTURE_STR_TEMPLATE.formatted(ID)));
    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState blockItemState = new ItemStackRenderState();

    public BlockProjectileRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, () -> new EntityAnimator<>(ID), b -> b.setRenderType(RENDER_TYPE), ID);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    protected boolean shouldShowName(@NotNull BlockProjectile entity, double distSq) {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull BlockProjectile entity, @NonNull AzEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.itemModelResolver.updateForLiving(this.blockItemState, entity.getMainHandItem(), ItemDisplayContext.HEAD, entity);
    }

    @Override
    public void submit(@NonNull AzEntityRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState cameraState) {
        poseStack.pushPose();
        if (!this.blockItemState.isEmpty()) {
            this.blockItemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        super.submit(state, poseStack, collector, cameraState);
        poseStack.popPose();
    }
}
