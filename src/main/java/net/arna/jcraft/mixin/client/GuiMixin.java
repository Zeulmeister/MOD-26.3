package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.gui.hud.EpitaphOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void jcraft$renderEpitaphOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (JClientConfig.getInstance().isEpitaphOverlay()) {
            EpitaphOverlay.render(graphics);
        }
    }
}
