package net.arna.jcraft.common.attack.moves.madeinheaven;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.serverconfig.IntOption;
import net.arna.jcraft.api.stand.ITimeAccelCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.common.network.s2c.TimeAccelStatePacket;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Getter
public final class TimeAccelerationMove<A extends StandEntity<? extends A, ?>> extends AbstractMove<TimeAccelerationMove<A>, A> {
    private final Either<Integer, IntOption> accelerationDuration;

    public TimeAccelerationMove(final int cooldown, final int windup, final int duration, final float moveDistance,
                                final Either<Integer, IntOption> accelerationDuration) {
        super(cooldown, windup, duration, moveDistance);
        this.accelerationDuration = accelerationDuration;
    }

    @Override
    public @NonNull MoveType<TimeAccelerationMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public void tick(final A attacker) {
        super.tick(attacker);

        tickTimeAcceleration(attacker);
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        final int accelTime = accelerationDuration.map(Function.identity(), IntOption::getValue);
        if (attacker instanceof ITimeAccelCapable ta) {
            ta.setAccelTime(accelTime);
            ta.setSpeedometer(0);
            ta.setAfterimage(true);
        }
        TimeAccelStatePacket.sendStart(attacker, accelTime);

        return Set.of();
    }

    private void tickTimeAcceleration(final A attacker) {
        if (!(attacker instanceof ITimeAccelCapable ta)) {
            return;
        }
        final int aTime = ta.getAccelTime();
        ta.setAccelTime(aTime - 1);

        if (aTime > 1) {
            if (JServerConfig.MIH_ACCELERATE_TICKS.getValue()) {
                final List<Entity> toCatch = attacker.level().getEntitiesOfClass(Entity.class,
                        attacker.getBoundingBox().inflate(96), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                for (Entity entity : toCatch) {
                    if (entity instanceof LivingEntity) {
                        continue;
                    }
                    entity.tick();
                }
            }
        } else if (aTime == 1) {
            if (ta.getSpeedometer() == MadeInHeavenEntity.MAXIMUM_SPEEDOMETER) {
                final List<LivingEntity> toCatch = attacker.level().getEntitiesOfClass(LivingEntity.class,
                        attacker.getBoundingBox().inflate(96),
                        EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(e -> e != attacker && e != attacker.getUser()));

                for (LivingEntity entity : toCatch) { // 15s of Standless to any victims of Universe Reset
                    entity.addEffect(new MobEffectInstance(JStatusRegistry.STANDLESS.asHolder(), 300, 0, true, false));
                }
            }

            ta.setAfterimage(false);
            ta.setSpeedometer(0);
        }
    }

    @Override
    protected @NonNull TimeAccelerationMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull TimeAccelerationMove<A> copy() {
        return copyExtras(new TimeAccelerationMove<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(),
                getAccelerationDuration()));
    }

    public static class Type extends AbstractMove.Type<TimeAccelerationMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<TimeAccelerationMove<?>>, TimeAccelerationMove<?>> buildCodec(RecordCodecBuilder.Instance<TimeAccelerationMove<?>> instance) {
            return baseDefault(instance).and(
                    Codec.either(ExtraCodecs.POSITIVE_INT, JServerConfig.INT_OPTION_CODEC).fieldOf("acceleration_duration")
                            .forGetter(TimeAccelerationMove::getAccelerationDuration))
                    .apply(instance, applyExtras(TimeAccelerationMove::new));
        }
    }
}
