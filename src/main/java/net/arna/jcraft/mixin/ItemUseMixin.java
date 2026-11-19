package net.arna.jcraft.mixin;

import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void jcraft$blockUseWhileDazed(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void jcraft$blockReleaseUsingWhileDazed(Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (entity.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            ci.cancel();
        }
    }
}
