package net.arna.jcraft.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Clean Minecraft 26.3 ItemStack DataComponents.CUSTOM_DATA helper.
 */
public final class JItemData {
    private JItemData() {}

    public static boolean hasTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (stack == null) return new CompoundTag();
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (stack == null) return;
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void edit(ItemStack stack, Consumer<CompoundTag> mutator) {
        if (stack == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, mutator);
    }
}
