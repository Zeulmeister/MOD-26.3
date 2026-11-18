package net.arna.jcraft.common.effects;

import net.arna.jcraft.api.AttackData;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.common.entity.damage.JDamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x6F1616);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        int i = 40 >> amplifier;
        return i <= 0 || duration % i == 0;
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplifier) {
        if (entity.invulnerableTime > 10.0F) {
            return true;
        }
        final DamageSource source = JDamageSources.bleeding(level);
        Attacks.damageLogic(
                level,
                entity,
                new AttackData(
                        Vec3.ZERO, 0, 0, false, 1.0f, false, 0,
                        source, null, CommonHitPropertyComponent.HitAnimation.MID, null,
                        false, false, false
                )
        );
        return true;
    }
}
