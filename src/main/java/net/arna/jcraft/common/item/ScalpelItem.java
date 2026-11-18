package net.arna.jcraft.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.arna.jcraft.common.entity.projectile.ScalpelProjectile;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ScalpelItem extends KnifeItem {
    

    public ScalpelItem(Properties settings) {
        super(settings.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    @Override
    public boolean releaseUsing(@org.jetbrains.annotations.NotNull ItemStack stack, @org.jetbrains.annotations.NotNull Level world, LivingEntity user, int remainingUseTicks) {
        if (user.hasEffect(JStatusRegistry.DAZED.asHolder())) {
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

            ScalpelProjectile scalpel = new ScalpelProjectile(world, user);
            scalpel.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 2.0F * getSpeedMult(stack, remainingUseTicks), 0F);
            world.addFreshEntity(scalpel);
        } else {
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F, 1F); // plays a globalSoundEvent
        }
        return true;
    }

    
}
