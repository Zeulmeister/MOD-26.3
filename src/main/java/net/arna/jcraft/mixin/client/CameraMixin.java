package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.zoom.StarPlatinumPrecisionHandler;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void jcraft$applyStarPlatinumPrecisionZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float modified = StarPlatinumPrecisionHandler.modifyFov(cir.getReturnValueF(), partialTick);
        if (modified != cir.getReturnValueF()) {
            cir.setReturnValue(modified);
        }
    }
}
