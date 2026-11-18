package net.arna.jcraft.mixin;

import net.arna.jcraft.common.events.JEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public class PersistentEntitySectionManagerMixin<T extends EntityAccess> {

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void jcraft$firePostAddEntity(T entityAccess, boolean worldGenSpawned, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entityAccess instanceof Entity entity) {
            JEntityEvents.POST_ADD.invoker().add(entity, worldGenSpawned);
        }
    }
}
