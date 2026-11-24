package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class HumanoidRenderStateMixin implements IJCraftEntityRenderStateHolder {

    @Unique
    @Nullable
    private LivingEntity jcraft$livingEntity;

    @Override
    public @Nullable LivingEntity jcraft$getLivingEntity() {
        return this.jcraft$livingEntity;
    }

    @Override
    public void jcraft$setLivingEntity(@Nullable LivingEntity entity) {
        this.jcraft$livingEntity = entity;
    }
}
