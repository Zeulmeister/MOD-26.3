package net.arna.jcraft.common.item;

import net.minecraft.world.InteractionResult;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AnubisItem extends Item {
    

    public AnubisItem(Properties settings) {
        super(settings.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    

    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
        return !miner.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipCtx, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipAdder, TooltipFlag context) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        Level world = null;
        tooltip.add(Component.translatable("jcraft.anubis.namedesc"));
        tooltip.add(Component.translatable("jcraft.anubis.bloodthirstdesc"));
        tooltip.add(Component.translatable("jcraft.anubis.removaldesc"));
        tooltip.add(Component.translatable("jcraft.anubis.desc"));
        tooltip.forEach(tooltipAdder);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack item = user.getItemInHand(hand);
        StandEntity<?, ?> stand = JUtils.getStand(user);

        if (user.isShiftKeyDown() || world.isClientSide() || (stand != null && stand.blocking)) {
            return InteractionResult.FAIL;
        }

        JUtils.serverPlaySound(JSoundRegistry.ANUBIS_SHEATHE.get(), (ServerLevel) world, user.position());
        user.setItemInHand(hand, new ItemStack(JItemRegistry.ANUBIS_SHEATHED.get()));

        return InteractionResult.SUCCESS.heldItemTransformedTo(item);
    }

    public static void handleAnubisEffects(int timeSinceAttack, Player player) {
        // If was in battle within last 5 minutes, apply haste 1
        if (timeSinceAttack > -6000) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 20, 0, true, false));
        } else {
            // If wasn't in battle for 10 minutes, apply mining fatigue 1
            if (timeSinceAttack < -12000) {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20, 0, true, false));
                // If wasn't in battle for 15 minutes, apply weakness 1
                if (timeSinceAttack < -18000) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 0, true, false));
                    // If wasn't in battle for 20 minutes, apply nausea 1
                    if (timeSinceAttack < -24000) {
                        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20, 0, true, false));
                        // If wasn't in battle for 25 minutes, apply slowness 1
                        if (timeSinceAttack < -30000) {
                            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, true, false));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel world, Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (world.isClientSide()) {
            return;
        }
        if (entity instanceof Player player) // Bloodlust
        {
            handleAnubisEffects(player.getLastHurtMobTimestamp() - player.tickCount, player);
        }
    }
}
