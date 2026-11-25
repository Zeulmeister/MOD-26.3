package net.arna.jcraft.common.attack.moves.chariotrequiem;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.attack.moves.shared.CounterMissMove;
import net.arna.jcraft.common.entity.stand.ChariotRequiemEntity;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Chariot Requiem's signature counter ("Stand Rebellion"):
 * punishes anyone who strikes SCR by turning their own Stand power against them,
 * cancelling active moves and inflicting Standless, Darkness, stun, and psychic backlash.
 */
public final class RequiemCounterAttack extends AbstractCounterAttack<RequiemCounterAttack, ChariotRequiemEntity> {
    private final CounterMissMove<ChariotRequiemEntity> counterMiss = new CounterMissMove<>(18);

    public RequiemCounterAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NotNull MoveType<RequiemCounterAttack> getMoveType() {
        return Type.INSTANCE;
    }

    @Override
    public void whiff(final @NonNull ChariotRequiemEntity attacker, final @NonNull LivingEntity user) {
        attacker.setMove(counterMiss, ChariotRequiemEntity.State.COUNTER_MISS);
        JCraft.stun(attacker.getUser(), counterMiss.getDuration(), 0);
    }

    @Override
    public void counter(final @NonNull ChariotRequiemEntity attacker, final Entity countered, final DamageSource counteredDamageSource) {
        super.counter(attacker, countered, counteredDamageSource);

        if (countered == null || !attacker.hasUser() || !(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        final LivingEntity user = attacker.getUserOrThrow();
        if (countered instanceof LivingEntity living) {
            final LivingEntity mainTarget = JUtils.getUserIfStand(living);
            JUtils.cancelMoves(mainTarget);
            JCraft.stun(mainTarget, 25, 0);
            mainTarget.addEffect(new MobEffectInstance(JStatusRegistry.STANDLESS.asHolder(), 60, 0, true, false));
            mainTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, true, false));

            final Vec3 kb = mainTarget.position().subtract(user.position());
            final Vec3 pushVec = (kb.lengthSqr() > 1.0E-4 ? kb.normalize() : attacker.getLookAngle()).scale(0.85).add(0.0, 0.25, 0.0);
            Attacks.damageLogic(serverLevel, mainTarget, pushVec, 20, 1, true, 6.0f,
                    false, 6, attacker.getDamageSource(), user, CommonHitPropertyComponent.HitAnimation.CRUSH, false);

            serverLevel.sendParticles(ParticleTypes.SOUL,
                    mainTarget.getX(), mainTarget.getY() + mainTarget.getBbHeight() * 0.5, mainTarget.getZ(),
                    16, 0.45, 0.55, 0.45, 0.06);
        }

        final Vec3 eyePos = attacker.getEyePosition();
        JCraft.createParticle(serverLevel, eyePos.x, eyePos.y, eyePos.z, JParticleType.FLASH);
        serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                JSoundRegistry.TIME_ERASE.get(), SoundSource.PLAYERS, 0.9f, 0.85f);
    }

    @Override
    protected @NonNull RequiemCounterAttack getThis() {
        return this;
    }

    @Override
    public @NonNull RequiemCounterAttack copy() {
        return copyExtras(new RequiemCounterAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractCounterAttack.Type<RequiemCounterAttack> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<RequiemCounterAttack>, RequiemCounterAttack> buildCodec(
                final RecordCodecBuilder.Instance<RequiemCounterAttack> instance) {
            return baseDefault(instance, RequiemCounterAttack::new);
        }
    }
}
