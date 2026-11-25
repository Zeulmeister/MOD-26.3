package net.arna.jcraft.common.item;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.projectile.BulletProjectile;
import net.arna.jcraft.common.util.JItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FVRevolverItem extends Item {

    public FVRevolverItem(Properties settings) {
        super(settings);
    }

    public static int getShots(ItemStack stack) {
        CompoundTag data = JItemData.getTag(stack);
        if (data == null || !data.contains("Shots")) {
            return 6;
        }
        return data.getIntOr("Shots", 0);
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(ItemStack stack, TooltipContext tooltipCtx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag context) {
        tooltipAdder.accept(Component.translatable("jcraft.revolver.shots").append(" Â§e" + getShots(stack)));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (user.hasEffect(JStatusRegistry.DAZED.asHolder()) || user.isSpectator()) {
            return InteractionResult.FAIL;
        }
        int shots = getShots(itemStack);
        if (shots < 1) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            user.getCooldowns().addCooldown(itemStack, 4);
            user.getCooldowns().addCooldown(new ItemStack(JItemRegistry.PEACEMAKER.get()), 4);
            fire(itemStack, world, user);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }

    public static void fire(ItemStack itemStack, Level world, LivingEntity user) {
        int shots = getShots(itemStack);
        if (shots < 1) {
            return;
        }

        JItemData.edit(itemStack, tag -> tag.putInt("Shots", shots - 1));
        world.playSound(null, user.getX(), user.getY(), user.getZ(), JSoundRegistry.REVOLVER_FIRE.get(), SoundSource.PLAYERS, 1f, 1f);

        BulletProjectile bullet = new BulletProjectile(world, user, 9f, 10f, 2, 7f);
        bullet.shootFromRotation(user, user.getXRot(), user.getYRot(), 0f, 10, 0F);
        world.addFreshEntity(bullet);

        if (user instanceof Player player) {
            player.getCooldowns().addCooldown(itemStack, 11);
            player.getCooldowns().addCooldown(new ItemStack(JItemRegistry.PEACEMAKER.get()), 11);
            player.awardStat(Stats.ITEM_USED.get(JItemRegistry.FV_REVOLVER.get()));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        stack.setDamageValue(stack.getDamageValue() + 1);
        if ((stack.getMaxDamage() - stack.getDamageValue()) <= 0) {
            stack.shrink(1);
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack s = new ItemStack(this);
        JItemData.edit(s, nbt -> nbt.putInt("Shots", 6));
        return s;
    }
}
