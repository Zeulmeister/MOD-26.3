package net.arna.jcraft.api.pose;

import com.mojang.serialization.Codec;
import net.arna.jcraft.common.util.JCodecUtils;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.model.Model;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.BiPredicate;

public enum ModifierCondition {
    LEFT_ARM_EMPTY((model, user) -> isArmEmptyAndFree(user, HumanoidArm.LEFT)),
    RIGHT_ARM_EMPTY((model, user) -> isArmEmptyAndFree(user, HumanoidArm.RIGHT)),
    LEFT_ARM_EMPTY_OR_ITEM((model, user) -> isArmNotBusy(user, HumanoidArm.LEFT)),
    RIGHT_ARM_EMPTY_OR_ITEM((model, user) -> isArmNotBusy(user, HumanoidArm.RIGHT)),
    USER_NOT_MOVING((model, user) -> JUtils.deltaPos(user).horizontalDistanceSqr() <= 0),
    USER_NOT_SPRINTING((model, user) -> !user.isSprinting());

    private static boolean isArmEmptyAndFree(final LivingEntity user, final HumanoidArm arm) {
        final ItemStack stack = user.getMainArm() == arm ? user.getMainHandItem() : user.getOffhandItem();
        return stack.isEmpty() && isArmNotBusy(user, arm);
    }

    private static boolean isArmNotBusy(final LivingEntity user, final HumanoidArm arm) {
        if (isTwoHandedItemActive(user)) {
            return false;
        }
        if (user.isSwinging()) {
            final LivingEntity.SwingDescription swing = user.getCurrentSwing();
            if (swing == null || swing.hand().asArm(user.getMainArm()) == arm) {
                return false;
            }
        }
        if (user.isUsingItem()) {
            final InteractionHand armHand = user.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (user.getUsedItemHand() == armHand) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTwoHandedItemActive(final LivingEntity user) {
        if (user.isUsingItem()) {
            final ItemUseAnimation useAnim = user.getUseItem().getUseAnimation();
            if (useAnim == ItemUseAnimation.BOW || useAnim == ItemUseAnimation.CROSSBOW) {
                return true;
            }
        }
        final ItemStack main = user.getMainHandItem();
        if (main.is(Items.CROSSBOW) && CrossbowItem.isCharged(main)) {
            return true;
        }
        final ItemStack off = user.getOffhandItem();
        return off.is(Items.CROSSBOW) && CrossbowItem.isCharged(off);
    }

    public static final Codec<ModifierCondition> CODEC = JCodecUtils.createEnumCodec(ModifierCondition.class);

    private final BiPredicate<Model<?>, LivingEntity> condition;

    ModifierCondition(final BiPredicate<Model<?>, LivingEntity> condition) {
        this.condition = condition;
    }

    public static boolean anyFails(final List<ModifierCondition> conditions, final Model<?> model, final LivingEntity user) {
        return conditions.stream().anyMatch(condition -> !condition.test(model, user));
    }

    public boolean test(final Model<?> model, final LivingEntity user) {
        return condition.test(model, user);
    }
}
