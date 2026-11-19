package net.arna.jcraft.common.menu;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JMenuRegistry;
import net.arna.jcraft.common.container.MaybeNoTakeSlot;
import net.arna.jcraft.common.item.DiscCaseItem;
import net.arna.jcraft.common.container.DiscCaseContainer;
import net.arna.jcraft.common.container.DiscCaseSlot;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DiscCaseMenu extends AbstractContainerMenu {

    private final int slots;
    private final ItemStack discCase;
    private final @org.jetbrains.annotations.Nullable InteractionHand discCaseHand;

    public DiscCaseMenu(final int id, final Player player) {
        super(JMenuRegistry.DISC_CASE_MENU_TYPE.get(), id);
        InteractionHand foundHand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof DiscCaseItem)) {
            foundHand = InteractionHand.OFF_HAND;
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (!(stack.getItem() instanceof final DiscCaseItem discCaseItem)) {
            this.slots = 0;
            this.discCase = ItemStack.EMPTY;
            this.discCaseHand = null;
            return;
        }
        this.discCase = stack;
        this.discCaseHand = foundHand;
        this.slots = discCaseItem.getSize();
        final Container container = DiscCaseContainer.of(stack);
        // case inventory
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < slots / 2; k++) {
                this.addSlot(new DiscCaseSlot(container, k + j * (slots / 2), 8 + 9 * (9 - slots / 2) + k * 18, 27 + j * 18));
            }
        }
        // player inventory (guarded with MaybeNoTakeSlot as well)
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new MaybeNoTakeSlot(stack, player.getInventory(), k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
            }
        }
        // player hotbar
        for (int j = 0; j < 9; j++) {
            this.addSlot(new MaybeNoTakeSlot(stack, player.getInventory(), j, 8 + j * 18, 142));
        }
    }

    @NonNull
    @Override
    public ItemStack quickMoveStack(final @NonNull Player player, final int index) {
        if (index < 0 || index >= super.slots.size()) {
            return ItemStack.EMPTY;
        }
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        // The case slots come first, followed by the player's inventory and hotbar.
        final boolean moved = index < slots
                ? moveItemStackTo(stack, slots, super.slots.size(), true)
                : moveIntoCase(stack);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        // Vanilla repeats a shift click for as long as this hands the item back, so moving into the case hands back nothing, letting in one disc per click.
        return index < slots ? original : ItemStack.EMPTY;
    }


    // Puts a single disc into the first free case slot
    private boolean moveIntoCase(final ItemStack stack) {
        for (int i = 0; i < slots; i++) {
            final Slot caseSlot = getSlot(i);
            if (!caseSlot.hasItem() && caseSlot.mayPlace(stack)) {
                caseSlot.setByPlayer(stack.split(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public void clicked(final int slotId, final int button, final @NonNull ContainerInput clickType, final @NonNull Player player) {
        if (clickType == ContainerInput.SWAP) {
            if ((discCaseHand == InteractionHand.OFF_HAND && button == 40)
                    || (button >= 0 && button < 9 && player.getInventory().getItem(button) == discCase)) {
                return;
            }
        }
        if (slotId >= 0 && slotId < super.slots.size() && getSlot(slotId).getItem() == discCase) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(final @NonNull Player player) {
        return discCaseHand != null && !discCase.isEmpty() && player.getItemInHand(discCaseHand) == discCase;
    }

    public int getSlotCount() {
        return slots;
    }

}
