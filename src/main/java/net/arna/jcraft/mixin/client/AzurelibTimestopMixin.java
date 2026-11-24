package net.arna.jcraft.mixin.client;

import mod.azure.azurelib.animation.AzAnimator;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AzAnimator.class, remap = false)
public class AzurelibTimestopMixin<K, T> {

    @Inject(method = "animate(Ljava/lang/Object;FZ)V", at = @At("HEAD"), cancellable = true)
    private void jcraft$freezeAzurelibAnimationInTimestop(T animatable, float partialTick, boolean isReRender, CallbackInfo ci) {
        if (animatable instanceof Entity entity && JUtils.isAffectedByTimeStop(entity)) {
            ci.cancel();
        }
    }
}
