package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.BisectProjectile;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class BisectAttack<A extends StandEntity<? extends A, ?>> extends AbstractMove<BisectAttack<A>, A> {
    public BisectAttack(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NonNull MoveType<BisectAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        BisectProjectile bisect = new BisectProjectile(attacker.level(), user);
        bisect.shootFromRotation(user, user.getXRot(), user.getYRot(), 0, 2.0f, 0);
        int charge = attacker instanceof IIronCapable iron ? iron.getBisectChargeTime() : 10;
        bisect.setScale(Math.max(0.5f, charge / 10.0f));
        attacker.level().addFreshEntity(bisect);

        return Set.of();
    }

    @Override
    protected @NonNull BisectAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull BisectAttack<A> copy() {
        return copyExtras(new BisectAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<BisectAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<BisectAttack<?>>, BisectAttack<?>> buildCodec(RecordCodecBuilder.Instance<BisectAttack<?>> instance) {
            return baseDefault(instance, BisectAttack::new);
        }
    }
}