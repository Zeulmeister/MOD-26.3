package net.arna.jcraft.common.attack.moves.goldexperience.requiem;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.attack.moves.shared.CounterMissMove;
import net.arna.jcraft.common.entity.stand.GEREntity;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class NullificationAttack<A extends StandEntity<? extends A, ?>> extends AbstractCounterAttack<NullificationAttack<A>, A> {
    private static final int COUNTER_STOP_TIME = 20; // Convenience
    private final CounterMissMove<A> counterMiss = new CounterMissMove<>(20);

    public NullificationAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NotNull MoveType<NullificationAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public void whiff(final @NonNull A attacker, final @NonNull LivingEntity user) {
        attacker.setMove(counterMiss, null);
        if (attacker instanceof GEREntity ger) {
            ger.setState(GEREntity.State.COUNTER_MISS);
        }
        JCraft.stun(attacker.getUser(), counterMiss.getDuration(), 0);
    }

    @Override
    public void counter(final @NonNull A attacker, final Entity countered, final DamageSource counteredDamageSource) {
        super.counter(attacker, countered, counteredDamageSource);

        if (countered == null || !attacker.hasUser()) {
            return;
        }

        JComponentPlatformUtils.getTimeStopData(countered).ifPresent(t -> t.setTicks(COUNTER_STOP_TIME));

        if (countered instanceof LivingEntity living) {
            JCraft.stun(living, 10, 0);
            JUtils.cancelMoves(living);
        }

        final Vec3 eP = attacker.getEyePosition();
        JCraft.createParticle((ServerLevel) attacker.level(), eP.x, eP.y, eP.z, JParticleType.FLASH);
    }

    @Override
    protected @NonNull NullificationAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull NullificationAttack<A> copy() {
        return copyExtras(new NullificationAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractCounterAttack.Type<NullificationAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<NullificationAttack<?>>, NullificationAttack<?>> buildCodec(RecordCodecBuilder.Instance<NullificationAttack<?>> instance) {
            return baseDefault(instance, NullificationAttack::new);
        }
    }
}
