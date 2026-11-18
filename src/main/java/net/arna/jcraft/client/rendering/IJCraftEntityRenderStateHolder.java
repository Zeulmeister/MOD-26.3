package net.arna.jcraft.client.rendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface IJCraftEntityRenderStateHolder {
    @Nullable
    LivingEntity jcraft$getLivingEntity();

    void jcraft$setLivingEntity(@Nullable LivingEntity entity);
}
