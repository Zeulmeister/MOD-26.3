package net.arna.jcraft.mixin.client;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityClientTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void jcraft$redirectTurnToControlledStand(double yRot, double xRot, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if ((Object) this != client.player) {
            return;
        }
        if (StandRemoteControlHandler.isControlling(client)) {
            StandEntity<?, ?> stand = StandRemoteControlHandler.getControlledStand();
            if (stand != null && stand.isAlive() && client.player != null) {
                stand.turn(yRot, xRot);
                stand.yHeadRot = stand.getYRot();
                stand.yHeadRotO = stand.yRotO;
                stand.yBodyRot = stand.getYRot();
                stand.yBodyRotO = stand.yRotO;

                client.player.setYRot(stand.getYRot());
                client.player.setXRot(stand.getXRot());
                client.player.yHeadRot = stand.getYRot();
                client.player.yBodyRot = stand.getYRot();
            }
            ci.cancel();
        }
    }
}
