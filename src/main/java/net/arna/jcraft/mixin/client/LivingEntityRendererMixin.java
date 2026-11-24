package net.arna.jcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.arna.jcraft.common.gravity.RotationAnimation;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void jcraft$attachLivingEntityReference(T entity, S state, float partialTick, CallbackInfo ci) {
        ((IJCraftEntityRenderStateHolder) state).jcraft$setLivingEntity(entity);
    }

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void jcraft$applyGravityModelRotation(S state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        LivingEntity entity = ((IJCraftEntityRenderStateHolder) state).jcraft$getLivingEntity();
        if (entity == null) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection(entity);
        Optional<RotationAnimation> animOpt = GravityChangerAPI.getGravityAnimation(entity);
        boolean animating = animOpt.map(RotationAnimation::isInAnimation).orElse(false);
        if (dir == Direction.DOWN && !animating) {
            return;
        }

        Quaternionf worldQuat = animOpt
                .map(a -> a.getCurrentGravityRotation(dir, Util.getMillis()))
                .orElseGet(() -> RotationUtil.getWorldRotationQuaternion(dir));
        poseStack.rotate(new Quaternionf(worldQuat).conjugate());
    }
}
