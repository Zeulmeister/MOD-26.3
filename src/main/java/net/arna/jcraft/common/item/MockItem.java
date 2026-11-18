package net.arna.jcraft.common.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.common.util.JItemData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MockItem extends Item {
    private static final ItemStack FALLBACK = new ItemStack(Items.DIRT);

    protected MockItem() {
        super(JItemRegistry.settings());
    }

    public static ItemStack getMockedStack(ItemStack mockItemStack) {
        CompoundTag nbt = JItemData.getTag(mockItemStack);
        if (nbt == null || !nbt.contains("MockItem")) {
            return FALLBACK;
        }

        String mockItemId = nbt.getStringOr("MockItem", "");
        Item mockItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(mockItemId));
        if (mockItem == Items.AIR) {
            return FALLBACK;
        }

        CompoundTag mockData = nbt.contains("MockData") ? nbt.getCompoundOrEmpty("MockData") : null;
        ItemStack mockedStack = new ItemStack(mockItem, mockItemStack.getCount());
        JItemData.setTag(mockedStack, mockData);
        return mockedStack;
    }

    protected static ItemStack createMockStack(ItemStack stack, RegistrySupplier<Item> mockItem) {
        ItemStack mockStack = new ItemStack(mockItem.get(), stack.getCount());
        JItemData.edit(mockStack, nbt -> {
            nbt.putString("MockItem", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            CompoundTag srcTag = JItemData.getTag(stack);
            if (srcTag != null) {
                nbt.put("MockData", srcTag);
            }
        });
        return mockStack;
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return getMockedStack(stack).getHoverName();
    }
}
