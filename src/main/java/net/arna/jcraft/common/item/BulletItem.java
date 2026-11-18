package net.arna.jcraft.common.item;
import net.arna.jcraft.common.util.JItemData;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BulletItem extends Item {
    public BulletItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipCtx, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag context) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        Level world = null;
        CompoundTag itemData = JItemData.getTag(stack);

        if (itemData != null && itemData.contains("Caliber")) {
            tooltip.add(Component.translatable("jcraft.bullet.caliber").append(" §e" + itemData.getFloatOr("Caliber", 0f) + "§9mm"));
        }
        tooltip.forEach(tooltipAdder);
    }

    public static ItemStack ofCaliber(float caliber) {
        ItemStack s = new ItemStack(JItemRegistry.BULLET.get());
        JItemData.edit(s, nbt -> nbt.putFloat("Caliber", caliber));
        return s;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack s = new ItemStack(this);
        JItemData.edit(s, nbt -> nbt.putFloat("Caliber", 9));
        return s;
    }
}
