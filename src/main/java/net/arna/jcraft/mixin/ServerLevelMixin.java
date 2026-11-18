package net.arna.jcraft.mixin;

import net.arna.jcraft.common.util.IJSplatterManagerHolder;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements IJSplatterManagerHolder {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jcraft$timestopTickEntity(Entity entity, CallbackInfo ci) {
        JComponentPlatformUtils.getTimeStopData(entity).ifPresent(ts -> {
            if (ts.tick()) {
                ci.cancel();
            }
        });
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jcraft$tickSplatters(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.jcraft$getSplatterManager().tick();
    }
}
