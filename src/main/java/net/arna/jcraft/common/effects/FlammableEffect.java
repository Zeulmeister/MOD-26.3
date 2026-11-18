package net.arna.jcraft.common.effects;

import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;

public class FlammableEffect extends MobEffect {
    public FlammableEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplifier) {
        if (!entity.isOnFire()) return true;
        entity.hurtServer(level, entity.damageSources().onFire(), 1.0f);
        BlockPos pos = entity.blockPosition();
        if (level.getBlockState(pos).isAir()) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
        return true;
    }

    public static boolean isFlammable(final LivingEntity entity) {
        return entity.hasEffect(JStatusRegistry.FLAMMABLE.asHolder());
    }
}
