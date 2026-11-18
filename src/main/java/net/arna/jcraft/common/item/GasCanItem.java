package net.arna.jcraft.common.item;

import net.minecraft.world.InteractionResult;

import lombok.NonNull;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.projectile.GasCanProjectile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class GasCanItem extends Item {
    public static final int COOLDOWN_DURATION = 5 * 20;

    public GasCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, net.minecraft.world.entity.LivingEntity _user) {
        return 7200;
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public @NotNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(usedHand);
        return InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user, int timeCharged) {
        if (user.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(JStatusRegistry.DAZED.get()))) {
            return false;
        }

        // Fix for a Minecraft bug where this method does get called on the client,
        // but not on the server when you switch to a different item the moment you release.
        // The actual selected item stack is already a different one, but this method still gets called.
        if (user.getItemInHand(user.getUsedItemHand()) != stack) return false;

        level.playSound(
                null,
                user.getX(),
                user.getY(),
                user.getZ(),
                JSoundRegistry.GAS_CAN_TOSS.get(),
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (!level.isClientSide()) {
            GasCanProjectile projectile = new GasCanProjectile(user, level);
            projectile.setItem(stack);
            projectile.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, getSpeedMult(stack, user, timeCharged), 1.0F);
            level.addFreshEntity(projectile);
        }

        if (!(user instanceof Player player)) return false;
        player.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(this), COOLDOWN_DURATION);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    private float getSpeedMult(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        float speedMult = (getUseDuration(stack, user) - remainingUseTicks);
        if (speedMult > getChargeTime()) {
            speedMult = getChargeTime();
        }
        speedMult /= getChargeTime();
        return speedMult;
    }

    private float getChargeTime() {
        return 20f;
    }
}
