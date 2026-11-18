package net.arna.jcraft.common.effects;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class StandlessEffect extends MobEffect {
    public StandlessEffect() {
        super(MobEffectCategory.NEUTRAL, 0x000000);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplifier) {
        final StandEntity<?, ?> stand = JUtils.getStand(entity);
        if (stand != null) {
            stand.desummon();
        }
        return true;
    }
}
