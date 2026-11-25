package net.arna.jcraft.common.item;

import net.minecraft.world.InteractionResult;

import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SheathedAnubisItem extends SpecObtainmentItem {
    public SheathedAnubisItem(Properties settings, RegistrySupplier<SpecType> spec) {
        super(settings, spec);
    }

    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    public int getUseDuration(@NotNull ItemStack stack, net.minecraft.world.entity.LivingEntity _user) {
        return 72000;
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipCtx, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag context) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        Level world = null;
        tooltip.add(Component.translatable("jcraft.anubis.namedesc"));
        tooltip.add(Component.translatable("jcraft.anubis.bloodthirstdesc"));
        tooltip.add(Component.translatable("jcraft.anubis.removaldesc"));
        tooltip.add(Component.translatable("jcraft.sheathedanubis.blockdesc"));
        tooltip.add(Component.translatable("jcraft.sheathedanubis.desc"));
        tooltip.forEach(tooltipAdder); // Doubles the previous Anubis description
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, Player user, @NotNull InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);

        if (user.isShiftKeyDown()) { // Block
            user.startUsingItem(hand);
        } else { // Unsheathe
            if (world.isClientSide()) {
                return InteractionResult.FAIL;
            }
            StandEntity<?, ?> stand = JUtils.getStand(user);
            if (stand != null && stand.blocking) {
                return InteractionResult.FAIL;
            }
            ServerLevel serverWorld = (ServerLevel) world;

            boolean specChanged = tryGetSpec(user);
            if (specChanged) {
                JUtils.serverPlaySound(JSoundRegistry.ANUBIS_UNSHEATHE.get(), serverWorld, user.position());
                JUtils.serverPlaySound(JSoundRegistry.ANUBIS_SPECCHANGE.get(), serverWorld, user.position());
                itemStack = new ItemStack(JItemRegistry.ANUBIS.get());
                user.setItemInHand(hand, itemStack);
            } else if (!isWarned(user)) {
                JUtils.serverPlaySound(JSoundRegistry.ANUBIS_UNSHEATHE.get(), serverWorld, user.position());
                itemStack = new ItemStack(JItemRegistry.ANUBIS.get());
                user.setItemInHand(hand, itemStack);
            } else {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.CONSUME.heldItemTransformedTo(itemStack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.server.level.ServerLevel world, @NotNull Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        boolean selected = slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        if (world.isClientSide()) {
            return;
        }
        if (entity instanceof Player player) // Bloodlust
        {
            AnubisItem.handleAnubisEffects(player.getLastHurtMobTimestamp() - player.tickCount, player);
        }
    }
}
