package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.input.TrackedKeyBinding;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "setDown", at = @At("HEAD"))
    private void jcraft$queueKeyPressOrRelease(boolean down, CallbackInfo ci) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (down == self.isDown()) {
            return;
        }
        TrackedKeyBinding.onKeyPressSet(self, down);
    }
}
