package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractHoldableMove;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class BisectChargeMove<A extends StandEntity<A, ?>> extends AbstractHoldableMove<BisectChargeMove<A>, A> {
    public BisectChargeMove(int cooldown, int windup, int duration, float moveDistance, int minimumCharge) {
        super(cooldown, windup, duration, moveDistance, minimumCharge);
    }

    @Override
    public @NonNull MoveType<BisectChargeMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public boolean conditionsMet(A attacker) {
        if (!super.conditionsMet(attacker)) {
            return false;
        }
        return !(attacker instanceof IIronCapable iron) || iron.getIron() >= 24.0f || iron.getBisectChargeTime() > 0;
    }

    @Override
    public void onInitiate(A attacker) {
        super.onInitiate(attacker);
        if (attacker instanceof IIronCapable iron) {
            iron.setBisectChargeTime(0);
        }
    }

    @Override
    public void activeTick(A attacker, int moveStun) {
        super.activeTick(attacker, moveStun);
        if (attacker instanceof IIronCapable iron && iron.drainIron(2.0f)) {
            iron.setBisectChargeTime(iron.getBisectChargeTime() + 1);
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        return Set.of();
    }

    @Override
    protected @NonNull BisectChargeMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull BisectChargeMove<A> copy() {
        return copyExtras(new BisectChargeMove<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getMinimumCharge()));
    }

    public static class Type extends AbstractHoldableMove.Type<BisectChargeMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<BisectChargeMove<?>>, BisectChargeMove<?>> buildCodec(RecordCodecBuilder.Instance<BisectChargeMove<?>> instance) {
            return holdableDefault(instance, BisectChargeMove::new);
        }
    }
}