package net.arna.jcraft.common.item;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CinderellaMaskItem extends Item {
    public CinderellaMaskItem() {
        super(JItemRegistry.settings().rarity(Rarity.RARE).equippable(EquipmentSlot.HEAD).enchantable(15));
    }

    @Override
    public void appendHoverText(final @NotNull ItemStack stack, final @NotNull TooltipContext context, final @NotNull TooltipDisplay display, final @NotNull Consumer<Component> tooltipAdder, final @NotNull TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("jcraft.cinderella_mask.enchdesc"));
        tooltipAdder.accept(Component.translatable("jcraft.cinderella_mask.usedesc"));
    }
}
