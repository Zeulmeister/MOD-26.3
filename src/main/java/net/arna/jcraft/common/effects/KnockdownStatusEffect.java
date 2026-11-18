package net.arna.jcraft.common.effects;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class KnockdownStatusEffect extends MobEffect {

    public KnockdownStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x444444);
        this.addAttributeModifier(Attributes.ARMOR, JCraft.id("knockdown_armor"), 30.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.ARMOR_TOUGHNESS, JCraft.id("knockdown_toughness"), 20.0, AttributeModifier.Operation.ADD_VALUE)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, JCraft.id("knockdown_movement"), 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return true;
    }

    @Override
    public void onEffectStarted(final LivingEntity entity, final int amplifier) {
        super.onEffectStarted(entity, amplifier);
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        entity.setPose(entity instanceof Player ? Pose.SWIMMING : Pose.SLEEPING);
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplifier) {
        if (entity.getPose() != Pose.SWIMMING && entity.getPose() != Pose.SLEEPING) {
            entity.setPose(entity instanceof Player ? Pose.SWIMMING : Pose.SLEEPING);
        }
        var inst = entity.getEffect(JStatusRegistry.KNOCKDOWN.asHolder());
        if (inst != null && inst.getDuration() <= 5) {
            entity.removeEffect(JStatusRegistry.DAZED.asHolder());
            if (inst.getDuration() <= 1) {
                entity.setPose(Pose.STANDING);
            }
        }
        return true;
    }
}
