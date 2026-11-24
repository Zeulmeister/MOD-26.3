package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.client.rendering.handler.JPostEffectManager;
import net.arna.jcraft.client.util.JClientUtils;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
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
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private List<Identifier> failedPostEffects;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void jcraft$hideFirstPersonHandWhenStealthedOrPiloting(
            CameraRenderState cameraRenderState,
            PlayerRenderState playerRenderState,
            GpuTextureView targetTexture,
            CallbackInfo ci) {
        if (StandRemoteControlHandler.isControlling(this.minecraft)
                || (this.minecraft.player != null && JClientUtils.shouldNotRender(this.minecraft.player))) {
            ci.cancel();
        }
    }

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
