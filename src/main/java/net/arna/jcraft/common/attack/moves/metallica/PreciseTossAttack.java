package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.ScalpelProjectile;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class PreciseTossAttack<A extends StandEntity<? extends A, ?>> extends AbstractMove<PreciseTossAttack<A>, A> {
    public PreciseTossAttack(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<PreciseTossAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        Vec3 pos = attacker.position();
        Vec3 upVec = GravityChangerAPI.getEyeOffset(user);
        for (int i = 1; i < 4; i++) {
            ScalpelProjectile scalpel = ScalpelProjectile.fromMetallica(attacker);
            if (scalpel == null) continue;
            scalpel.setPos(pos.add(upVec.scale(0.25 * i)));
            scalpel.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.25F, 0.0F);
            attacker.level().addFreshEntity(scalpel);
        }

        return Set.of();
    }

    @Override
    protected @NonNull PreciseTossAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull PreciseTossAttack<A> copy() {
        return copyExtras(new PreciseTossAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<PreciseTossAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<PreciseTossAttack<?>>, PreciseTossAttack<?>> buildCodec(RecordCodecBuilder.Instance<PreciseTossAttack<?>> instance) {
            return baseDefault(instance, PreciseTossAttack::new);
        }
    }
}