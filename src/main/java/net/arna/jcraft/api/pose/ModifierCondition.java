package net.arna.jcraft.api.pose;

import com.mojang.serialization.Codec;
import net.arna.jcraft.common.util.JCodecUtils;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.model.Model;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.BiPredicate;

public enum ModifierCondition {
    LEFT_ARM_EMPTY((model, user) -> (user.getMainArm() == HumanoidArm.LEFT ? user.getMainHandItem() : user.getOffhandItem()).isEmpty()),
    RIGHT_ARM_EMPTY((model, user) -> (user.getMainArm() == HumanoidArm.RIGHT ? user.getMainHandItem() : user.getOffhandItem()).isEmpty()),
    LEFT_ARM_EMPTY_OR_ITEM((model, user) -> !user.isUsingItem() || user.getUsedItemHand() != (user.getMainArm() == HumanoidArm.LEFT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)),
    RIGHT_ARM_EMPTY_OR_ITEM((model, user) -> !user.isUsingItem() || user.getUsedItemHand() != (user.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)),
    USER_NOT_MOVING((model, user) -> JUtils.deltaPos(user).horizontalDistanceSqr() <= 0),
    USER_NOT_SPRINTING((model, user) -> !user.isSprinting());

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
