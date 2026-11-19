package net.arna.jcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.effects.ExhaustionEffect;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoodData.class)
public class FoodDataMixin {
    @Unique
    private CommonVampireComponent jcraft$vampireComponent;
    @Unique
    private boolean jcraft$isVampire;
    @Unique
    private int jcraft$healCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jcraft$updateVampirism(ServerPlayer player, CallbackInfo ci) {
        this.jcraft$vampireComponent = JComponentPlatformUtils.getVampirism(player);
        this.jcraft$isVampire = this.jcraft$vampireComponent != null && this.jcraft$vampireComponent.isVampire();
        if (this.jcraft$isVampire) {
            ci.cancel();
        }
    }

    @Inject(method = "getFoodLevel", at = @At("HEAD"), cancellable = true)
    private void jcraft$getBloodLevel(CallbackInfoReturnable<Integer> cir) {
        if (this.jcraft$isVampire && this.jcraft$vampireComponent != null) {
            cir.setReturnValue((int) Math.floor(this.jcraft$vampireComponent.getBlood()));
        }
    }

    @Inject(method = "getSaturationLevel", at = @At("HEAD"), cancellable = true)
    private void jcraft$getSaturationLevel(CallbackInfoReturnable<Float> cir) {
        if (this.jcraft$isVampire) {
            cir.setReturnValue(0.0F);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void jcraft$increaseHealCounter(ServerPlayer player, float amount, Operation<Void> original) {
        original.call(player, amount);
        this.jcraft$healCounter++;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 10))
    private int jcraft$reduceFastFoodRegen(int constant, ServerPlayer player) {
        return jcraft$reduceFoodRegen(constant, player);
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 80, ordinal = 0))
    private int jcraft$reduceSlowFoodRegen(int constant, ServerPlayer player) {
        return jcraft$reduceFoodRegen(constant, player);
    }

    @Unique
    private int jcraft$reduceFoodRegen(int constant, ServerPlayer player) {
        MobEffectInstance exhaustion = player.getEffect(JStatusRegistry.EXHAUSTION.asHolder());
        if (exhaustion == null) {
            return constant;
        }
        int amp = exhaustion.getAmplifier();
        if (this.jcraft$healCounter % ExhaustionEffect.MAX_LEVEL > amp) {
            return constant;
        }
        return switch (amp) {
            case 0 -> constant * 2;
            case 1 -> constant * 3;
            case 2 -> constant * 4;
            default -> Integer.MAX_VALUE;
        };
    }
}
