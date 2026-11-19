package net.arna.jcraft.mixin.client;

import net.arna.jcraft.api.pose.ModelType;
import net.arna.jcraft.api.pose.modifier.IPoseModifier;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.arna.jcraft.client.rendering.StandUserPoseLoader;
import net.arna.jcraft.client.util.JClientUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow
    @Final
    public ModelPart body;

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Shadow
    @Final
    public ModelPart rightLeg;

    @Shadow
    @Final
    public ModelPart leftLeg;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void jcraft$applyStandAndTimestopLevitationPoses(T state, CallbackInfo ci) {
        LivingEntity entity = ((IJCraftEntityRenderStateHolder) state).jcraft$getLivingEntity();
        if (entity == null) {
            return;
        }

        if (entity.hasPose(Pose.STANDING)
                && !state.isCrouching
                && !state.isPassenger
                && !state.isFallFlying
                && state.swimAmount <= 0.0f
                && state.swingAnimation <= 0.0f) {
            IPoseModifier pose = StandUserPoseLoader.getPose(ModelType.HUMANOID, entity);
            if (pose != IPoseModifier.EMPTY) {
                pose.apply((HumanoidModel<?>) (Object) this, entity, state.ageInTicks);
            }
        }

        // Constant JoJo hover levitation pose during Time Stop flight
        if (entity instanceof Player player
                && (player.getAbilities().flying || !player.onGround())
                && JClientUtils.isUserOfActiveTimestop(player)) {
            this.body.xRot = (float) Math.toRadians(4.0);
            if (state.swingAnimation <= 0.0f) {
                this.body.yRot = 0.0f;
            }
            this.body.zRot = 0.0f;

            this.rightLeg.xRot = (float) Math.toRadians(10.0);
            this.rightLeg.yRot = (float) Math.toRadians(4.0);
            this.rightLeg.zRot = (float) Math.toRadians(5.0);

            this.leftLeg.xRot = (float) Math.toRadians(20.0);
            this.leftLeg.yRot = (float) Math.toRadians(-4.0);
            this.leftLeg.zRot = (float) Math.toRadians(-5.0);

            boolean twoHanded = state.rightArmPose.isTwoHanded() || state.leftArmPose.isTwoHanded();
            if (state.swingAnimation <= 0.0f && !state.isUsingItem && !twoHanded) {
                if (state.rightArmPose == HumanoidModel.ArmPose.EMPTY) {
                    this.rightArm.xRot = (float) Math.toRadians(-6.0);
                    this.rightArm.zRot = (float) Math.toRadians(10.0);
                }
                if (state.leftArmPose == HumanoidModel.ArmPose.EMPTY) {
                    this.leftArm.xRot = (float) Math.toRadians(-6.0);
                    this.leftArm.zRot = (float) Math.toRadians(-10.0);
                }
            }
        }
    }
}
