package net.arna.jcraft.common.attack.moves.thesun;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractBarrageAttack;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.stand.TheSunEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class MeteorShowerAttack<A extends StandEntity<? extends A, ?>> extends AbstractBarrageAttack<MeteorShowerAttack<A>, A> {
    public MeteorShowerAttack(int cooldown, int windup, int duration, int interval) {
        super(cooldown, windup, duration, 0, 0, 0, 0, 0, 0, interval);
        ranged = true;
    }

    @Override
    public @NonNull MoveType<MeteorShowerAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        Set<LivingEntity> targets = super.perform(attacker, user);

        float scale = attacker instanceof TheSunEntity sun ? sun.getRawScale() : 2.0f;
        for (int i = 0; i < scale * 1.5; i++) {
            Vec3 originPos = attacker instanceof TheSunEntity sun
                    ? sun.randomPos()
                    : attacker.position().add(
                    attacker.getRandom().nextGaussian() * 0.8,
                    1.0 + attacker.getRandom().nextGaussian() * 0.4,
                    attacker.getRandom().nextGaussian() * 0.8);
            FireMeteorAttack.fireMeteor(attacker, user, originPos,
                    JUtils.randUnitVec(attacker.getRandom()), 1.25f, 0f);
        }

        return targets;
    }

    @Override
    protected @NonNull MeteorShowerAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull MeteorShowerAttack<A> copy() {
        return copyExtras(new MeteorShowerAttack<>(getCooldown(), getWindup(), getDuration(), getInterval()));
    }

    public static class Type extends AbstractBarrageAttack.Type<MeteorShowerAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<MeteorShowerAttack<?>>, MeteorShowerAttack<?>> buildCodec(RecordCodecBuilder.Instance<MeteorShowerAttack<?>> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), interval())
                    .apply(instance, applyAttackExtras(MeteorShowerAttack::new));
        }
    }
}