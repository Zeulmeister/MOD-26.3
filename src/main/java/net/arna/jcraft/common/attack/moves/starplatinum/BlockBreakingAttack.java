package net.arna.jcraft.common.attack.moves.starplatinum;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

import static net.minecraft.world.level.block.Block.getId;

public final class BlockBreakingAttack<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<BlockBreakingAttack<A>, A> {
    public BlockBreakingAttack(final int cooldown, final int windup, final int duration, final float moveDistance,
                               final float damage, final int stun, final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
    }

    @Override
    public void performHook(final A attacker, final Set<LivingEntity> targets,
                            final Set<AABB> boxes, final DamageSource damageSource, final Vec3 forwardPos,
                            final Vec3 rotationVector) {
        LivingEntity baseEntity = attacker.getBaseEntity();
        Level world = baseEntity.level();
        LivingEntity user = attacker.getUserOrThrow();
        if (!mayBreak(user, null)) return;

        LivingEntity aimEntity = attacker.isRemote() ? baseEntity : user;
        Vec3 eyePos = AbstractSimpleAttack.getAimOrigin(attacker);
        net.minecraft.world.phys.BlockHitResult hit = AbstractSimpleAttack.raycastAimedBlock(world, attacker, 5.0);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return;
        }

        final BlockPos bPos = hit.getBlockPos();
        final net.minecraft.core.Direction hitFace = hit.getDirection();
        final net.minecraft.core.Direction.Axis faceAxis = hitFace.getAxis();
        final net.minecraft.core.Direction axisA = faceAxis == net.minecraft.core.Direction.Axis.X
                ? net.minecraft.core.Direction.SOUTH
                : net.minecraft.core.Direction.EAST;
        final net.minecraft.core.Direction axisB = faceAxis == net.minecraft.core.Direction.Axis.Y
                ? net.minecraft.core.Direction.SOUTH
                : net.minecraft.core.Direction.UP;

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                final BlockPos curPos = bPos.relative(axisA, a).relative(axisB, b);

                if (!mayBreak(user, curPos, s -> s.getBlock().getExplosionResistance() <= 10 && !s.isAir() && !s.hasBlockEntity())) {
                    continue;
                }

                if (!curPos.equals(bPos)) {
                    final net.minecraft.world.phys.BlockHitResult los = world.clip(new net.minecraft.world.level.ClipContext(
                            eyePos, Vec3.atCenterOf(curPos),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            aimEntity
                    ));
                    if (los.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && !los.getBlockPos().equals(curPos)) {
                        continue;
                    }
                }

                final BlockState curState = world.getBlockState(curPos);
                world.levelEvent(null, 2001, curPos, getId(curState)); // Particles

                FallingBlockEntity fallingBlock = FallingBlockEntity.fall(world, curPos, curState);
                fallingBlock.setDeltaMovement(rotationVector.add(
                        (axisA.getStepX() * a + axisB.getStepX() * b) * 0.35,
                        0.45,
                        (axisA.getStepZ() * a + axisB.getStepZ() * b) * 0.35
                ));
                fallingBlock.time = -120;
                fallingBlock.syncVelocity = true;
                fallingBlock.needsSync = true;
            }
        }
    }

    @Override
    public @NonNull MoveType<BlockBreakingAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    protected @NonNull BlockBreakingAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull BlockBreakingAttack<A> copy() {
        return copyExtras(new BlockBreakingAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(), getStun(),
                getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<BlockBreakingAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<BlockBreakingAttack<?>>, BlockBreakingAttack<?>> buildCodec(RecordCodecBuilder.Instance<BlockBreakingAttack<?>> instance) {
            return attackDefault(instance, BlockBreakingAttack::new);
        }
    }
}
