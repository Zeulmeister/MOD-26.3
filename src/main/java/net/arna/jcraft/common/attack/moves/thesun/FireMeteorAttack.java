package net.arna.jcraft.common.attack.moves.thesun;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.MeteorProjectile;
import net.arna.jcraft.common.entity.stand.TheSunEntity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;

public class FireMeteorAttack<A extends StandEntity<? extends A, ?>> extends AbstractMove<FireMeteorAttack<A>, A> {
    @Getter
    private final int meteors;
    @Getter
    private final float meteorVelocity, meteorSpeed, meteorDivergence;
    @Getter
    private final double explodeScaleMinimum;
    @Getter
    @NonNull
    private final IntCollection hitMoments;
    private Vec3 targetPosition;

    public FireMeteorAttack(int cooldown, int duration, int meteors, float meteorVelocity, float meteorSpeed, float meteorDivergence,
                            double explodeScaleMinimum, @NonNull IntCollection hitMoments) {
        super(cooldown, hitMoments.intStream().min().orElse(duration + 1), duration, 0);
        this.meteors = meteors;
        this.meteorVelocity = meteorVelocity;
        this.meteorSpeed = meteorSpeed;
        this.meteorDivergence = meteorDivergence;
        this.explodeScaleMinimum = explodeScaleMinimum;
        this.hitMoments = new IntImmutableList(hitMoments.intStream().sorted().toArray());
        ranged = true;
    }

    @Override
    public @NonNull MoveType<FireMeteorAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public boolean shouldPerform(final A attacker, final int moveStun) {
        return attacker.hasUser() && hitMoments.contains(getDuration() - attacker.getMoveStun());
    }

    @Override
    public void onInitiate(A attacker) {
        super.onInitiate(attacker);

        if (attacker instanceof TheSunEntity sun) {
            targetPosition = sun.acquireTargetPosition();
        } else if (attacker.hasUser()) {
            final LivingEntity user = attacker.getUserOrThrow();
            targetPosition = user.getEyePosition().add(user.getLookAngle().scale(32.0));
        } else {
            targetPosition = attacker.getEyePosition().add(attacker.getLookAngle().scale(16.0));
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        Vec3 pos = attacker instanceof TheSunEntity sun ? sun.randomPos() : attacker.getEyePosition().add(0.0, 0.4, 0.0);
        for (int i = 0; i < meteors; i++) {
            Vec3 velocity = JUtils.getLookVector(pos, targetPosition).scale(meteorVelocity);
            MeteorProjectile meteor = fireMeteor(attacker, user, pos, velocity, meteorSpeed, meteorDivergence);
            meteor.setNoGravity(true);

            if (attacker instanceof TheSunEntity sun && explodeScaleMinimum <= sun.getRawScale()) {
                meteor.setExplosive(true);
            }
        }

        return Set.of();
    }

    public static MeteorProjectile fireMeteor(StandEntity<?, ?> attacker, @NonNull LivingEntity user, Vec3 pos, Vec3 velocity) {
        return fireMeteor(attacker, user, pos, velocity, 1.25f, 10f);
    }

    public static MeteorProjectile fireMeteor(StandEntity<?, ?> attacker, @NonNull LivingEntity user, Vec3 pos, Vec3 velocity, float speed, float divergence) {
        final MeteorProjectile meteor = new MeteorProjectile(attacker.level(), user, attacker instanceof TheSunEntity sun ? sun : null);
        meteor.setSkin(attacker.getSkin());
        meteor.setPos(pos);
        meteor.shoot(velocity.x, velocity.y, velocity.z, speed, divergence);

        attacker.level().addFreshEntity(meteor);
        final AbstractMove<?, ?> move = attacker.getCurrentMove();
        if (move != null && !move.isBarrage()) {
            attacker.playSound(JSoundRegistry.SUN_METEOR_FIRE.get(), 1f, 1f);
        }

        return meteor;
    }

    @Override
    protected @NonNull FireMeteorAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull FireMeteorAttack<A> copy() {
        return copyExtras(new FireMeteorAttack<>(getCooldown(), getDuration(), meteors, meteorVelocity, meteorSpeed,
                meteorDivergence, explodeScaleMinimum, hitMoments));
    }

    public static class Type extends AbstractMove.Type<FireMeteorAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<FireMeteorAttack<?>>, FireMeteorAttack<?>> buildCodec(RecordCodecBuilder.Instance<FireMeteorAttack<?>> instance) {
            return instance.group(extras(), cooldown(), duration(),
                    Codec.INT.fieldOf("meteors").forGetter(FireMeteorAttack::getMeteors),
                    Codec.FLOAT.fieldOf("meteor_velocity").forGetter(FireMeteorAttack::getMeteorVelocity),
                    Codec.FLOAT.fieldOf("meteor_speed").forGetter(FireMeteorAttack::getMeteorSpeed),
                    Codec.FLOAT.fieldOf("meteor_divergence").forGetter(FireMeteorAttack::getMeteorDivergence),
                    Codec.DOUBLE.optionalFieldOf("explosive_scale_minimum", 2.0).forGetter(FireMeteorAttack::getExplodeScaleMinimum),
                    ExtraCodecs.NON_NEGATIVE_INT.listOf()
                            .<IntCollection>xmap(IntOpenHashSet::new, ArrayList::new)
                            .fieldOf("hit_moments").forGetter(FireMeteorAttack::getHitMoments)
                    ).apply(instance, applyExtras(FireMeteorAttack::new));
        }
    }
}
