package net.arna.jcraft.common.attack.moves.horus;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.IIceLanceCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.LargeIcicleProjectile;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class IceLanceAttack<A extends StandEntity<? extends A, ?>> extends AbstractMove<IceLanceAttack<A>, A> {
    public IceLanceAttack(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<IceLanceAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        if (attacker.getCurrentMove() == null) return Set.of();

        LargeIcicleProjectile icicle = new LargeIcicleProjectile(attacker.level(), user);
        if (attacker instanceof IIceLanceCapable ice) {
            ice.setLastLargeIcicle(icicle);
        }

        final Vec3i gravity = GravityChangerAPI.getGravityDirection(user).getUnitVec3i();
        final Vec3 velocity = user.getLookAngle();
        final double e = velocity.x, f = velocity.y, g = velocity.z;
        final double l = velocity.horizontalDistance();
        icicle.snapTo(
                attacker.getX() - gravity.getX() * 1.5, attacker.getY() - gravity.getY() * 1.5, attacker.getZ() - gravity.getZ() * 1.5, (float) (Mth.atan2(-e, -g) * 57.2957763671875),
                (float) (Mth.atan2(f, l) * 57.2957763671875)
        );
        icicle.setDeltaMovement(velocity.scale(1.75));
        icicle.markProjectile();
        icicle.lock();

        attacker.level().addFreshEntity(icicle);
        return Set.of();
    }

    @Override
    protected @NonNull IceLanceAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull IceLanceAttack<A> copy() {
        return copyExtras(new IceLanceAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<IceLanceAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<IceLanceAttack<?>>, IceLanceAttack<?>> buildCodec(RecordCodecBuilder.Instance<IceLanceAttack<?>> instance) {
            return baseDefault(instance, IceLanceAttack::new);
        }
    }
}