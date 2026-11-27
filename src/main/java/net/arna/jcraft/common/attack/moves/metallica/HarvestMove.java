package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.MoveSelectionResult;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.attack.moves.AbstractBarrageAttack;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;
import java.util.Set;

public class HarvestMove<A extends StandEntity<? extends A, ?>> extends AbstractBarrageAttack<HarvestMove<A>, A> {
    public HarvestMove(int duration, float moveDistance, int interval) {
        super(0, 0, duration, moveDistance, 0, 0, 0, 0, 0, interval);
        withHoldable();
    }

    @Override
    public @NonNull MoveType<HarvestMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public void onInitiate(A attacker) {
        super.onInitiate(attacker);
        if (attacker instanceof IIronCapable iron) {
            iron.setSiphonPos(Optional.empty());
        }
    }

    @Override
    public void onUserMoveInput(final A attacker, final MoveInputType type, final boolean pressed, final boolean moveInitiated) {
        super.onUserMoveInput(attacker, type, pressed, moveInitiated);
        if (type.getMoveClass() == getMoveClass() && !pressed) attacker.cancelMove();
    }

    @Override
    public void activeTick(A attacker, int moveStun) {
        super.activeTick(attacker, moveStun);

        LivingEntity user = attacker.getUser();

        if (user instanceof Mob mob && attacker instanceof IIronCapable ironStand) {
            final float iron = ironStand.getIron();
            if (iron >= ironStand.getMaxIron() || (iron >= (ironStand.getMaxIron() / 3.0f) && mob.getRandom().nextFloat() > 0.1f))
                attacker.cancelMove();
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        if (!(attacker instanceof IIronCapable ironStand)) {
            return Set.of();
        }

        if (user instanceof Mob mob) {
            final boolean noBlock = mob.getBlockStateOn().isAir();
            final BlockPos hitPos = mob.getOnPos();

            ironStand.setSiphonPos(noBlock ? Optional.empty() : Optional.of(hitPos));
            if (noBlock) return Set.of();

            float gain = user.level().getBlockState(hitPos).is(JTagRegistry.IRON_BLOCKS) ? 3f : 1.5f;
            if (ironStand.isMetallicaInvisible()) gain /= 2.0f;

            ironStand.addIron(gain);
            return Set.of();
        }

        final BlockHitResult hitResult = JUtils.genericBlockRaycast(user.level(), user, 5, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            final BlockPos hitPos = hitResult.getBlockPos();
            ironStand.setSiphonPos(Optional.of(hitPos));

            float gain = user.level().getBlockState(hitPos).is(JTagRegistry.IRON_BLOCKS) ? 3f : 1.5f;
            if (ironStand.isMetallicaInvisible()) gain /= 2.0f;

            ironStand.addIron(gain);
        } else {
            ironStand.setSiphonPos(Optional.empty());
        }

        return Set.of();
    }

    @Override
    public MoveSelectionResult specificMoveSelectionCriterion(A attacker, LivingEntity mob,
                                                              LivingEntity target, int stunTicks,
                                                              int enemyMoveStun, double distance, StandEntity<?, ?> enemyStand,
                                                              AbstractMove<?, ?> enemyAttack) {
        if (!(attacker instanceof IIronCapable ironStand)) {
            return MoveSelectionResult.PASS;
        }
        final float iron = ironStand.getIron();
        if (iron < (ironStand.getMaxIron() / 3.0f) && mob.getRandom().nextFloat() > 0.1f) return MoveSelectionResult.USE;
        return iron >= ironStand.getMaxIron() ? MoveSelectionResult.STOP : MoveSelectionResult.PASS;
    }

    @Override
    protected @NonNull HarvestMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull HarvestMove<A> copy() {
        return copyExtras(new HarvestMove<>(getDuration(), getMoveDistance(), getInterval()));
    }

    public static class Type extends AbstractBarrageAttack.Type<HarvestMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<HarvestMove<?>>, HarvestMove<?>> buildCodec(RecordCodecBuilder.Instance<HarvestMove<?>> instance) {
            return instance.group(extras(), attackExtras(), duration(), moveDistance(), interval())
                    .apply(instance, applyAttackExtras(HarvestMove::new));
        }
    }
}