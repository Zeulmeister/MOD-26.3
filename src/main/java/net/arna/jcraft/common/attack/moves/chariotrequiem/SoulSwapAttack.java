package net.arna.jcraft.common.attack.moves.chariotrequiem;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chariot Requiem's Soul Swap pulse:
 * when 2+ targets are caught, cyclically swaps their world positions;
 * when 1 target is caught, disorients them by inverting their facing direction.
 * All caught victims suffer Nausea, Weakness, and spectral soul particles.
 */
public final class SoulSwapAttack<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<SoulSwapAttack<A>, A> {
    public SoulSwapAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                          final float damage, final int stun, final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
    }

    @Override
    public @NotNull MoveType<SoulSwapAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    protected void performHook(final A attacker, final Set<LivingEntity> targets, final Set<AABB> boxes,
                               final DamageSource damageSource, final Vec3 forwardPos, final Vec3 rotationVector) {
        if (!attacker.hasUser() || !(attacker.getEntityWorld() instanceof ServerLevel serverLevel) || targets.isEmpty()) {
            return;
        }

        final LivingEntity user = attacker.getUserOrThrow();
        final Set<LivingEntity> uniqueVictims = new LinkedHashSet<>();
        for (final LivingEntity hit : targets) {
            final LivingEntity resolved = JUtils.getUserIfStand(hit);
            if (resolved != null && resolved != user && resolved.isAlive()) {
                uniqueVictims.add(resolved);
            }
        }
        if (uniqueVictims.isEmpty()) {
            return;
        }

        final List<LivingEntity> victims = new ArrayList<>(uniqueVictims);
        if (victims.size() >= 2) {
            final List<Vec3> originalPositions = victims.stream().map(LivingEntity::position).toList();
            final int count = victims.size();
            for (int i = 0; i < count; i++) {
                final LivingEntity victim = victims.get(i);
                final Vec3 nextPos = originalPositions.get((i + 1) % count);
                victim.resetFallDistance();
                victim.teleportTo(nextPos.x, nextPos.y, nextPos.z);
                victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, false));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false));
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        nextPos.x, nextPos.y + victim.getBbHeight() * 0.5, nextPos.z,
                        14, 0.4, 0.5, 0.4, 0.05);
            }
        } else {
            final LivingEntity solo = victims.get(0);
            final float invertedYaw = solo.getYRot() + 180.0f;
            solo.setYRot(invertedYaw);
            solo.setYHeadRot(invertedYaw);
            solo.setYBodyRot(invertedYaw);
            solo.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, false));
            solo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false));
            solo.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, true, false));
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    solo.getX(), solo.getY() + solo.getBbHeight() * 0.5, solo.getZ(),
                    14, 0.4, 0.5, 0.4, 0.05);
        }
    }

    @Override
    protected @NonNull SoulSwapAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull SoulSwapAttack<A> copy() {
        return copyExtras(new SoulSwapAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<SoulSwapAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<SoulSwapAttack<?>>, SoulSwapAttack<?>> buildCodec(
                final RecordCodecBuilder.Instance<SoulSwapAttack<?>> instance) {
            return attackDefault(instance, SoulSwapAttack::new);
        }
    }
}
