package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.gui.hud.EpitaphOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void jcraft$renderEpitaphOverlay(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean gameLoadFinished, CallbackInfo ci) {
        if (shouldRenderLevel && JClientConfig.getInstance().isEpitaphOverlay()) {
            EpitaphOverlay.render();
        }
    }
}
