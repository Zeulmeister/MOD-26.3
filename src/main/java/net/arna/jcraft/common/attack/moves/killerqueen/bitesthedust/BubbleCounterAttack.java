package net.arna.jcraft.common.attack.moves.killerqueen.bitesthedust;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.attack.moves.shared.CounterMissMove;
import net.arna.jcraft.common.entity.stand.KQBTDEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class BubbleCounterAttack<A extends StandEntity<? extends A, ?>> extends AbstractCounterAttack<BubbleCounterAttack<A>, A> {
    private final CounterMissMove<A> missAttack = new CounterMissMove<>(15);

    public BubbleCounterAttack(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NotNull MoveType<BubbleCounterAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public void whiff(final @NonNull A attacker, final @NonNull LivingEntity user) {
        attacker.setMove(missAttack, null);
        if (attacker instanceof KQBTDEntity btd) {
            btd.setState(KQBTDEntity.State.COUNTER_MISS);
        }
        JCraft.stun(user, missAttack.getDuration(), 0);
    }

    @Override
    public void counter(final @NonNull A attacker, final Entity countered, final DamageSource counteredDamageSource) {
        super.counter(attacker, countered, counteredDamageSource);
        if (countered == null || !attacker.hasUser() || counteredDamageSource.is(DamageTypes.MAGIC)) {
            return;
        }

        if (countered instanceof LivingEntity livingEntity) {
            JCraft.stun(livingEntity, 10, 3);
            JUtils.cancelMoves(livingEntity);
        }

        JComponentPlatformUtils.getBombTracker(attacker.getUser()).getMainBomb().setBomb(countered);
        //stand.playSound(JSoundRegistry.BTD_COUNTER_HIT, 1, 1);
    }

    @Override
    protected @NonNull BubbleCounterAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull BubbleCounterAttack<A> copy() {
        return copyExtras(new BubbleCounterAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractCounterAttack.Type<BubbleCounterAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<BubbleCounterAttack<?>>, BubbleCounterAttack<?>> buildCodec(RecordCodecBuilder.Instance<BubbleCounterAttack<?>> instance) {
            return baseDefault(instance, BubbleCounterAttack::new);
        }
    }
}
