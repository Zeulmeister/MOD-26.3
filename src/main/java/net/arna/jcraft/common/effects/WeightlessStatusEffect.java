package net.arna.jcraft.common.effects;

import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static net.arna.jcraft.common.gravity.api.GravityChangerAPI.getGravityDirection;

public class WeightlessStatusEffect extends MobEffect {

    public WeightlessStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x000011);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return amplifier == 1;
    }

    @Override
    public void onEffectStarted(final LivingEntity entity, final int amplifier) {
        super.onEffectStarted(entity, amplifier);
        if (!entity.level().isClientSide()) {
            CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(entity);
            misc.setPrevNoGrav(entity.isNoGravity());
            misc.setHoverTime(0);
        }
    }

    @Override
    public boolean applyEffectTick(final ServerLevel world, final LivingEntity entity, final int amplifier) {
        Vec3 pos = entity.position();
        Vec3 downPos = pos.add(RotationUtil.vecPlayerToWorld(0.0, -5.0, 0.0, getGravityDirection(entity)));
        world.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 1, 0.4, 0.4, 0.4, 0.0);

        CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(entity);
        if (!entity.isAlive()) {
            cleanupWeightless(entity);
            return false;
        }

        HitResult hitResult = world.clip(new ClipContext(pos, downPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            misc.setHoverTime(0);
        } else {
            int newHoverTime = misc.getHoverTime() + 1;
            misc.setHoverTime(newHoverTime);
            if (newHoverTime > 10) {
                cleanupWeightless(entity);
                return false;
            }
        }
        return true;
    }

    public static void cleanupWeightless(final LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        GravityChangerAPI.clearGravity(entity);
        if (!JComponentPlatformUtils.getMiscData(entity).getPrevNoGrav()) {
            entity.setNoGravity(false);
        }
    }
}
