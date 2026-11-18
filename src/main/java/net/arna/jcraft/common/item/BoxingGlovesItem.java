package net.arna.jcraft.common.item;

import net.minecraft.world.InteractionResult;

import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.api.spec.SpecType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BoxingGlovesItem extends SpecObtainmentItem {
    public BoxingGlovesItem(Properties settings, RegistrySupplier<SpecType> spec) {
        super(settings, spec);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipCtx, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag context) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        Level world = null;
        tooltip.add(Component.translatable("jcraft.boxinggloves.desc"));
        tooltip.forEach(tooltipAdder);
    }

    @Override
    public @NotNull InteractionResult use(Level world, Player user, @NotNull InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (!world.isClientSide()) {
            boolean specChanged = tryGetSpec(user);
            if (specChanged) {
                if (!user.isCreative()) {
                    itemStack.shrink(1);
                }
                user.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(this), 20);
            }
        }

        return InteractionResult.CONSUME.heldItemTransformedTo(itemStack);
    }
}
