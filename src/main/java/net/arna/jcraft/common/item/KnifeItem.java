package net.arna.jcraft.common.item;

import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.projectile.KnifeProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class KnifeItem extends Item {
    public KnifeItem(Properties settings) {
        super(settings.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        return 7200;
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            return InteractionResult.FAIL;
        }
        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    protected float getSpeedMult(ItemStack stack, int remainingUseTicks) {
        float speedMult = (7200 - remainingUseTicks);
        if (speedMult > getChargeTime()) {
            speedMult = getChargeTime();
        }
        speedMult /= getChargeTime();
        return speedMult;
    }

    protected float getChargeTime() {
        return 10F;
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext tooltipCtx, @NotNull TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, @NotNull TooltipFlag context) {
        tooltipAdder.accept(Component.translatable("jcraft.knife.chargetime").append(" Â§e" + getChargeTime() / 20F + "Â§9s"));
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level world, LivingEntity user, int remainingUseTicks) {
        if (user.hasEffect(JStatusRegistry.DAZED.asHolder()) || user.getItemInHand(user.getUsedItemHand()) != stack) {
            return false;
        }

        if (!world.isClientSide()) {
            if (user instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCooldowns().addCooldown(stack, 15);
                serverPlayer.awardStat(Stats.ITEM_USED.get(this));
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }

            KnifeProjectile knife = new KnifeProjectile(world, user);
            knife.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F * getSpeedMult(stack, remainingUseTicks), 0F);
            world.addFreshEntity(knife);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F, 1F);
        }
        return true;
    }

    @Override
    public boolean canDestroyBlock(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, LivingEntity miner) {
        return !(miner instanceof Player p && p.isCreative());
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, BlockState state) {
        return state.is(Blocks.COBWEB);
    }
}
