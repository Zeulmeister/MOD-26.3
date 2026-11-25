package net.arna.jcraft.common.item;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CinderellaMaskItem extends Item {
    public CinderellaMaskItem() {
        super(JItemRegistry.settings()
                .rarity(Rarity.RARE)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, JCraft.id("cinderella_mask")))
                        .build())
                .enchantable(15));
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(final @NotNull ItemStack stack, final @NotNull TooltipContext context, final @NotNull TooltipDisplay display, final @NotNull Consumer<Component> tooltipAdder, final @NotNull TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("jcraft.cinderella_mask.enchdesc"));
        tooltipAdder.accept(Component.translatable("jcraft.cinderella_mask.usedesc"));
    }
}
