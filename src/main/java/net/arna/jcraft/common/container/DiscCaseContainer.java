package net.arna.jcraft.common.container;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.common.item.DiscCaseItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class DiscCaseContainer extends SimpleContainer {
    protected final ItemStack discCase;

    protected DiscCaseContainer(final int size, final @NonNull ItemStack discCase) {
        super(size);
        this.discCase = discCase;
    }

    @Override
    public boolean canAddItem(final @NonNull ItemStack stack) {
        return super.canAddItem(stack) && stack.is(JTagRegistry.DISCS);
    }

    @NonNull
    @Override
    public ItemStack addItem(final @NonNull ItemStack stack) {
        final ItemStack result = super.addItem(stack);
        updateDiscCaseStackContent();
        return result;
    }

    @Override
    public void setItem(final int slot, final @NonNull ItemStack stack) {
        super.setItem(slot, stack);
        updateDiscCaseStackContent();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateDiscCaseStackContent();
    }

    protected void updateDiscCaseStackContent() {
        discCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    public static DiscCaseContainer of(final ItemStack discCase) {
        if (!(discCase.getItem() instanceof final DiscCaseItem discCaseItem)) {
            throw new IllegalArgumentException("Not a Disc Case item!");
        }
        final DiscCaseContainer container = new DiscCaseContainer(discCaseItem.getSize(), discCase);
        ItemContainerContents contents = discCase.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        contents.copyInto(container.getItems());
        return container;
    }
}
