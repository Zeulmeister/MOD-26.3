package net.arna.jcraft.common.attack.moves.horus;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.IIceLanceCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.LargeIcicleProjectile;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class HorusDetonateAttack<A extends StandEntity<? extends A, ?>> extends AbstractMove<HorusDetonateAttack<A>, A> {
    public HorusDetonateAttack(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NonNull MoveType<HorusDetonateAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public boolean conditionsMet(A attacker) {
        return super.conditionsMet(attacker)
                && (!(attacker instanceof IIceLanceCapable ice) || ice.getLastLargeIcicle() != null);
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        if (attacker instanceof IIceLanceCapable ice) {
            LargeIcicleProjectile icicle = ice.getLastLargeIcicle();
            if (icicle != null) {
                icicle.detonate();
            }
        }
        return Set.of();
    }

    @Override
    protected @NonNull HorusDetonateAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull HorusDetonateAttack<A> copy() {
        return copyExtras(new HorusDetonateAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<HorusDetonateAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<HorusDetonateAttack<?>>, HorusDetonateAttack<?>> buildCodec(RecordCodecBuilder.Instance<HorusDetonateAttack<?>> instance) {
            return baseDefault(instance, HorusDetonateAttack::new);
        }
    }
}