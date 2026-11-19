package net.arna.jcraft.mixin;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.common.item.StandDiscItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {

    @Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
    private void jcraft$grindStandDisc(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
        boolean inputPresent = !input.isEmpty();
        boolean additionalPresent = !additional.isEmpty();
        if (inputPresent == additionalPresent) {
            return; // Neither or both occupied: let vanilla handle (or fail)
        }
        ItemStack stack = inputPresent ? input : additional;
        if (!stack.is(JItemRegistry.STAND_DISC.get()) || stack.getCount() != 1) {
            return;
        }
        if (StandDiscItem.isEmptyDisc(stack)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack emptyDisc = new ItemStack(JItemRegistry.STAND_DISC.get());
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            emptyDisc.set(DataComponents.CUSTOM_NAME, customName);
        }
        StandDiscItem.syncDiscModel(emptyDisc);
        cir.setReturnValue(emptyDisc);
    }
}
