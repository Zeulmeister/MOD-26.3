package net.arna.jcraft.common.attack.moves.killerqueen;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.StateContainer;
import net.arna.jcraft.api.attack.moves.AbstractGrabAttack;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.stand.KillerQueenEntity;
import net.arna.jcraft.common.util.StandAnimationState;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class KQGrabAttack<A extends StandEntity<A, S>, S extends Enum<S> & StandAnimationState<A>>
        extends AbstractGrabAttack<KQGrabAttack<A, S>, A, S> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public KQGrabAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                        final float damage, final int stun, final float hitboxSize, final float knockback,
                        final float offset, final AbstractMove<?, ? super A> hitMove) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, hitMove,
                (StateContainer<S>) (StateContainer) StateContainer.of(KillerQueenEntity.State.GRAB_HIT));
    }

    @Override
    public @NotNull MoveType<KQGrabAttack<A, S>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        Set<LivingEntity> targets = super.perform(attacker, user);
        targets.stream().findFirst().ifPresent(JComponentPlatformUtils.getBombTracker(user).getMainBomb()::setBomb);
        return targets;
    }

    @Override
    protected @NonNull KQGrabAttack<A, S> getThis() {
        return this;
    }

    @Override
    public @NonNull KQGrabAttack<A, S> copy() {
        return copyExtras(new KQGrabAttack<A, S>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(), getStun(),
                getHitboxSize(), getKnockback(), getOffset(), getHitMove()));
    }

    public static class Type extends AbstractGrabAttack.Type<KQGrabAttack<?, ?>> {
        public static final Type INSTANCE = new Type();

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<KQGrabAttack<?, ?>>, KQGrabAttack<?, ?>> buildCodec(RecordCodecBuilder.Instance<KQGrabAttack<?, ?>> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(), stun(),
                    hitboxSize(), knockback(), offset(), hitMove()).apply(instance, applyAttackExtras(
                    (cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, hitMove) ->
                            new KQGrabAttack(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, hitMove)));
        }
    }
}