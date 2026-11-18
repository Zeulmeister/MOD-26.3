package net.arna.jcraft.common.item;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.common.util.JItemData;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

public class BloodBottleItem extends Item {
    private static final int MAX_USE_TIME = 24;
    public static final float MAX_BLOOD = 16f;
    public static final int USAGE_COOLDOWN = 80;

    public BloodBottleItem(Properties settings) {
        super(settings);
    }

    public static void syncBottleModel(ItemStack stack, float blood) {
        int stage = Mth.clamp(Math.round(blood / 3.2f), 0, 5);
        stack.set(DataComponents.ITEM_MODEL, stage == 0 ? JCraft.id("blood_bottle") : JCraft.id("blood_bottle_" + stage));
    }

    @Override
    public @NonNull InteractionResult use(@NonNull final Level world, @NonNull final Player user, @NonNull final InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }

    @Override
    public @NonNull ItemStack finishUsingItem(@NonNull ItemStack stack, @NonNull Level world, @NonNull LivingEntity user) {
        final Player playerEntity = user instanceof Player p ? p : null;
        final CompoundTag nbt = JItemData.getOrCreateTag(stack);
        float blood = nbt.getFloatOr("Blood", 0f);

        if (blood >= 0.5f && playerEntity != null) {
            CommonVampireComponent vampireComponent = JComponentPlatformUtils.getVampirism(playerEntity);
            if (vampireComponent.isVampire()) {
                boolean full = blood >= 1.0f;
                playerEntity.awardStat(Stats.ITEM_USED.get(this));
                float updatedBlood = blood;
                if (!playerEntity.getAbilities().instabuild && vampireComponent.getBlood() < 20) {
                    updatedBlood = Math.max(blood - 1f, 0f);
                    final float finalBlood = updatedBlood;
                    JItemData.edit(stack, t -> t.putFloat("Blood", finalBlood));
                    syncBottleModel(stack, finalBlood);
                }
                playerEntity.getCooldowns().addCooldown(stack, USAGE_COOLDOWN);

                if (!world.isClientSide()) {
                    if (playerEntity instanceof ServerPlayer sp) {
                        CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
                    }
                    vampireComponent.setBlood(vampireComponent.getBlood() + (full ? 2 : 1));
                }
                user.gameEvent(GameEvent.DRINK);
            }
        }
        return stack;
    }

    @Override
    public @NonNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        JItemData.edit(stack, t -> t.putFloat("Blood", 0f));
        return stack;
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack stack) {
        CompoundTag tag = JItemData.getTag(stack);
        if (tag != null && tag.getFloatOr("Blood", 0f) >= 0.5f) {
            return ItemUseAnimation.DRINK;
        }
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext tooltipCtx, @NonNull TooltipDisplay tooltipDisplay, @NonNull Consumer<Component> tooltipAdder, @NonNull TooltipFlag context) {
        CompoundTag nbt = JItemData.getTag(stack);
        if (nbt != null && nbt.contains("Blood")) {
            tooltipAdder.accept(Component.empty()
                    .append(Component.translatable("jcraft.blood_bottle.units"))
                    .append(Component.literal(" "))
                    .append(nbt.getFloatOr("Blood", 0f) + "/" + MAX_BLOOD));
        }
    }

    @Override
    public @NonNull InteractionResult interactLivingEntity(@NonNull ItemStack stack, @NonNull Player user, @NonNull LivingEntity entity, @NonNull InteractionHand hand) {
        if (user.getCooldowns().isOnCooldown(stack) || !JUtils.canAct(user)) {
            return InteractionResult.PASS;
        }

        float bloodMult = JUtils.getBloodMult(entity);
        if (bloodMult <= 0) {
            return InteractionResult.PASS;
        }

        user.getCooldowns().addCooldown(stack, 15);
        if (!user.level().isClientSide() && user.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, user.level().damageSources().playerAttack(user), 2);
            CompoundTag nbtCompound = JItemData.getOrCreateTag(stack);
            float blood = nbtCompound.getFloatOr("Blood", 0f);
            float newBlood = blood + bloodMult;
            if (blood <= MAX_BLOOD * 2 && newBlood > MAX_BLOOD) {
                newBlood = MAX_BLOOD;
            }
            final float finalBlood = newBlood;
            JItemData.edit(stack, t -> t.putFloat("Blood", finalBlood));
            syncBottleModel(stack, finalBlood);
            user.setItemInHand(hand, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity entity) {
        return MAX_USE_TIME;
    }
}
