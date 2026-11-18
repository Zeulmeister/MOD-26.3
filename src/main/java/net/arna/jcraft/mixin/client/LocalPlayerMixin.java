package net.arna.jcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @WrapOperation(
            method = "aiStep",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z", opcode = 181),
            require = 0
    )
    private void jcraft$disableSpaceDoubleTapWhenCreaming(Abilities abilities, boolean value, Operation<Void> original) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!CreamEntity.isCreaming(self)) {
            original.call(abilities, value);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void jcraft$isolatePlayerMovementDuringPilotMode(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (StandRemoteControlHandler.isControlling(mc)) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            self.xxa = 0.0f;
            self.zza = 0.0f;
            self.setJumping(false);
        }
    }
}
