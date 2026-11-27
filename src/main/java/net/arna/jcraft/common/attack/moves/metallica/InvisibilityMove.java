package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.tickable.MagneticFields;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.lang.ref.WeakReference;
import java.util.Set;

public class InvisibilityMove<A extends StandEntity<? extends A, ?>> extends AbstractMove<InvisibilityMove<A>, A> {
    private WeakReference<MagneticFields.MagneticField> magneticField;

    public InvisibilityMove(int cooldown, int windup, int duration) {
        super(cooldown, windup, duration, 0f);
    }

    @Override
    public @NonNull MoveType<InvisibilityMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    private void setInvisible(final A stand, final boolean invis) {
        if (!(stand instanceof IIronCapable iron)) {
            return;
        }
        iron.setMetallicaInvisible(invis);

        if (invis) {
            final LivingEntity user = stand.getUserOrThrow();

            user.addEffect(
                    new MobEffectInstance(MobEffects.INVISIBILITY, 39, 0, true, false)
            );

            magneticField = new WeakReference<>(MagneticFields.createField((ServerLevel) stand.level(),
                    null, stand.position(), 3.0f, 5.0f));
        } else {
            MagneticFields.MagneticField field = magneticField == null ? null : magneticField.get();
            if (field == null) {
                JCraft.LOGGER.warn("InvisibilityMove#MAGNETIC_FIELD was null when decloaking!");
            } else {
                field.time = 0;
            }
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        if (attacker instanceof IIronCapable iron) {
            final boolean newInvis = !iron.isMetallicaInvisible();
            setInvisible(attacker, newInvis);
        }
        return Set.of();
    }

    @Override
    public void tick(A attacker) {
        super.tick(attacker);

        if (!(attacker instanceof IIronCapable iron)) {
            return;
        }
        final boolean invisible = iron.isMetallicaInvisible();
        if (!invisible || attacker.tickCount % 2 != 0) return;

        final boolean canStayInvis = iron.drainIron(0.75f);
        if (!canStayInvis) {
            setInvisible(attacker, false);
        } else {
            final LivingEntity user = attacker.getUserOrThrow();
            final MagneticFields.MagneticField field = magneticField == null ? null : magneticField.get();
            if (field != null)
                field.pos = attacker.position().add(GravityChangerAPI.getEyeOffset(user));
            user.addEffect(
                    new MobEffectInstance(MobEffects.INVISIBILITY, 21, 0, true, false)
            );
        }
    }

    @Override
    protected @NonNull InvisibilityMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull InvisibilityMove<A> copy() {
        return copyExtras(new InvisibilityMove<>(getCooldown(), getWindup(), getDuration()));
    }

    public static class Type extends AbstractMove.Type<InvisibilityMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<InvisibilityMove<?>>, InvisibilityMove<?>> buildCodec(RecordCodecBuilder.Instance<InvisibilityMove<?>> instance) {
            return instance.group(extras(), cooldown(), windup(), duration()).apply(instance, applyExtras(InvisibilityMove::new));
        }
    }
}