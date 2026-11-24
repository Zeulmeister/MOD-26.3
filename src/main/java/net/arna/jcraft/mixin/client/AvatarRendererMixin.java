package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.renderer.features.ArmoredMoveFeatureRenderer;
import net.arna.jcraft.client.renderer.features.HamonParticlesFeatureRenderer;
import net.arna.jcraft.client.renderer.features.StuckKnivesFeatureRenderer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {

    protected AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jcraft$registerCombatLayers(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new ArmoredMoveFeatureRenderer<>(this));
        this.addLayer(new StuckKnivesFeatureRenderer<>(this));
        this.addLayer(new HamonParticlesFeatureRenderer<>(this));
    }
}
