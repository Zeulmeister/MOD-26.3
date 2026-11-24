package net.arna.jcraft.mixin;

import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private boolean clientIsFloating;

    @Shadow
    private int aboveGroundTickCount;

    @Inject(method = "tickPlayer", at = @At("HEAD"))
    private void jcraft$preventFloatingKickUnderDirectionalGravity(CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null && GravityChangerAPI.getGravityDirection(this.player) != Direction.DOWN) {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }
    }
}
