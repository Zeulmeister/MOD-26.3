package net.arna.jcraft.mixin;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.common.item.StandDiscItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public abstract class GrindstoneSlotMixin extends Slot {

    protected GrindstoneSlotMixin(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void jcraft$allowFilledStandDiscs(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(JItemRegistry.STAND_DISC.get()) && !StandDiscItem.isEmptyDisc(stack)) {
            boolean bothEmpty = this.container.getItem(0).isEmpty() && this.container.getItem(1).isEmpty();
            cir.setReturnValue(bothEmpty);
        }
    }
}
