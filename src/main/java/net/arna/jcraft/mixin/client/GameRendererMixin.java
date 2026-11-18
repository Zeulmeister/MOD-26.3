package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.rendering.handler.JPostEffectManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private List<Identifier> failedPostEffects;

    @Inject(method = "extract", at = @At("TAIL"))
    private void jcraft$appendActivePostEffects(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (!this.gameRenderState.shouldRenderLevel) {
            return;
        }
        List<Identifier> merged = JPostEffectManager.collectActiveEffects(this.gameRenderState.requestedPostEffects);
        if (merged != this.gameRenderState.requestedPostEffects) {
            this.gameRenderState.requestedPostEffects.clear();
            for (Identifier id : merged) {
                if (!this.failedPostEffects.contains(id)) {
                    this.gameRenderState.requestedPostEffects.add(id);
                }
            }
        }
    }
}
