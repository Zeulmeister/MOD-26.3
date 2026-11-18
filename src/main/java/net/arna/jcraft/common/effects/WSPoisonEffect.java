package net.arna.jcraft.common.effects;

import lombok.NonNull;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;

public class WSPoisonEffect extends MobEffect {

    public WSPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0xccccdd);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(final @NonNull ServerLevel level, final @NonNull LivingEntity entity, final int amplifier) {
        if (entity instanceof final Mob mob) {
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.2));
            mob.setTarget(null);
            mob.setAggressive(false);
        } else {
            entity.setPose(Pose.SWIMMING);
        }
        return true;
    }
}
