package net.arna.jcraft.mixin;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.IJInputStateManagerHolder;
import net.arna.jcraft.common.util.InputStateManager;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.ServerPlayerEntityMixinLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements IJInputStateManagerHolder {

    @Unique
    private final InputStateManager jcraft$inputStateManager = new InputStateManager();

    @Unique
    private boolean jcraft$hadStandBeforeTeleport = false;

    @Override
    public InputStateManager jcraft$getJInputStateManager() {
        return jcraft$inputStateManager;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jcraft$clearGunAimingWhenUnequipped(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (net.arna.jcraft.common.system.GunAiming.isAiming(self)
                && !net.arna.jcraft.common.system.GunAiming.isAimableGun(self.getMainHandItem())) {
            net.arna.jcraft.common.system.GunAiming.set(self, false);
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void jcraft$copyInputStateUponRestore(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        if (oldPlayer instanceof IJInputStateManagerHolder holder) {
            this.jcraft$inputStateManager.copyFrom(holder.jcraft$getJInputStateManager());
        }
    }

    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD")
    )
    private void jcraft$beforeDimensionTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        StandEntity<?, ?> stand = JUtils.getStand(self);
        this.jcraft$hadStandBeforeTeleport = stand != null;
        if (stand != null) {
            stand.setPlayDesummonSound(false);
        }
    }

    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("RETURN")
    )
    private void jcraft$afterDimensionTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        ServerPlayerEntityMixinLogic.inject_moveToWorld_sendPacket_1(result);
        if (this.jcraft$hadStandBeforeTeleport && transition.newLevel() != null && JUtils.getStand(result) == null) {
            StandEntity<?, ?> stand = JCraft.summon(transition.newLevel(), result);
            if (stand != null) {
                stand.setPlaySummonSound(false);
            }
        }
        this.jcraft$hadStandBeforeTeleport = false;
    }
}
