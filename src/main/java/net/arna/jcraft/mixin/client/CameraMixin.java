package net.arna.jcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.client.zoom.StarPlatinumPrecisionHandler;
import net.arna.jcraft.common.gravity.RotationAnimation;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity entity;

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    @Final
    private static Vector3fc FORWARDS;

    @Shadow
    @Final
    private static Vector3fc UP;

    @Shadow
    @Final
    private static Vector3fc LEFT;

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void jcraft$applyStarPlatinumPrecisionZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float modified = StarPlatinumPrecisionHandler.modifyFov(cir.getReturnValueF(), partialTick);
        if (modified != cir.getReturnValueF()) {
            cir.setReturnValue(modified);
        }
    }

    @Inject(method = "setRotation", at = @At("TAIL"))
    private void jcraft$applyGravityCameraRotation(float yRot, float xRot, CallbackInfo ci) {
        if (this.entity == null) {
            return;
        }
        Direction dir = GravityChangerAPI.getGravityDirection(this.entity);
        Optional<RotationAnimation> animOpt = GravityChangerAPI.getGravityAnimation(this.entity);
        boolean animating = animOpt.map(RotationAnimation::isInAnimation).orElse(false);
        if (dir == Direction.DOWN && !animating) {
            return;
        }

        Quaternionf worldQuat = animOpt
                .map(a -> a.getCurrentGravityRotation(dir, Util.getMillis()))
                .orElseGet(() -> RotationUtil.getWorldRotationQuaternion(dir));
        Quaternionf camQuat = new Quaternionf(worldQuat).conjugate();
        this.rotation.premul(camQuat);
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
    }

    @WrapOperation(
            method = "alignWithEntity(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    )
    private void jcraft$alignCameraEyeWithGravity(Camera instance, double x, double y, double z,
                                                  Operation<Void> original, float partialTick) {
        if (this.entity != null) {
            Direction dir = GravityChangerAPI.getGravityDirection(this.entity);
            Optional<RotationAnimation> animOpt = GravityChangerAPI.getGravityAnimation(this.entity);
            boolean animating = animOpt.map(RotationAnimation::isInAnimation).orElse(false);
            if (dir != Direction.DOWN || animating) {
                float eh = Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight);
                Vec3 base = this.entity.getPosition(partialTick);
                Vector3f eyeVec;
                if (animating) {
                    Quaternionf worldQuat = animOpt.get().getCurrentGravityRotation(dir, Util.getMillis());
                    Quaternionf camQuat = new Quaternionf(worldQuat).conjugate();
                    eyeVec = new Vector3f(0.0f, eh, 0.0f).rotate(camQuat);
                } else {
                    Vec3 rotated = RotationUtil.vecPlayerToWorld(0.0, eh, 0.0, dir);
                    eyeVec = new Vector3f((float) rotated.x, (float) rotated.y, (float) rotated.z);
                }
                original.call(instance, base.x + eyeVec.x, base.y + eyeVec.y, base.z + eyeVec.z);
                return;
            }
        }
        original.call(instance, x, y, z);
    }
}
