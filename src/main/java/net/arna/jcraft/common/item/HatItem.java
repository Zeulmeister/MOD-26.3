package net.arna.jcraft.common.item;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Consumer;

public class HatItem extends Item {
    public HatItem(ArmorMaterial materialIn, Properties builder) {
        super(builder.humanoidArmor(materialIn, ArmorType.HELMET));
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext context, @NonNull TooltipDisplay display, @NonNull Consumer<Component> tooltipAdder, @NonNull TooltipFlag flag) {
        if (getDefaultInstance().is(JTagRegistry.PROTECTS_FROM_SUN)) {
            tooltipAdder.accept(Component.translatable("jcraft.sunprotection.desc"));
        }
    }
}
