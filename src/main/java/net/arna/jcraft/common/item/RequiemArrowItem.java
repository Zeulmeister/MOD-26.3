package net.arna.jcraft.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RequiemArrowItem extends Item {
    public RequiemArrowItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipCtx, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag context) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        Level world = null;
        tooltip.add(Component.translatable("jcraft.requiemarrow.desc"));
        tooltip.forEach(tooltipAdder);
    }
}
