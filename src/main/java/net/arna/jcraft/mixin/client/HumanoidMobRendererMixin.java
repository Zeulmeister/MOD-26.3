package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public class HumanoidMobRendererMixin {

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
