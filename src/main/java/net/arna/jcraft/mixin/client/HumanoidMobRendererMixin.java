package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.renderer.features.ArmoredMoveFeatureRenderer;
import net.arna.jcraft.client.renderer.features.HamonParticlesFeatureRenderer;
import net.arna.jcraft.client.renderer.features.StuckKnivesFeatureRenderer;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public abstract class HumanoidMobRendererMixin<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>>
        extends MobRenderer<T, S, M> {

    protected HumanoidMobRendererMixin(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/model/HumanoidModel;FLnet/minecraft/client/renderer/entity/layers/CustomHeadLayer$Transforms;)V",
            at = @At("TAIL")
    )
    private void jcraft$registerMobCombatLayers(
            EntityRendererProvider.Context context,
            M model,
            M babyModel,
            float shadowRadius,
            CustomHeadLayer.Transforms headTransforms,
            CallbackInfo ci) {
        this.addLayer(new ArmoredMoveFeatureRenderer<>(this));
        this.addLayer(new StuckKnivesFeatureRenderer<>(this));
        this.addLayer(new HamonParticlesFeatureRenderer<>(this));
    }

    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void jcraft$storeLivingEntityOnRenderState(
            LivingEntity entity,
            HumanoidRenderState state,
            float partialTick,
            ItemModelResolver itemModelResolver,
            CallbackInfo ci
    ) {
        ((IJCraftEntityRenderStateHolder) state).jcraft$setLivingEntity(entity);
    }
}
