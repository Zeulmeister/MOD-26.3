package net.arna.jcraft.common.attack.moves.starplatinum.theworld;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.entity.stand.SPTWEntity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

public final class SPTWGroundSlamAttack<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<SPTWGroundSlamAttack<A>, A> {
    public SPTWGroundSlamAttack(final int cooldown, final int windup, final int duration, final float moveDistance, final float damage, final int stun,
                                final float hitboxSize, final float knockback, final float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
    }

    @Override
    public @NonNull MoveType<SPTWGroundSlamAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        Set<LivingEntity> targets = super.perform(attacker, user);

        final LivingEntity originEntity = attacker.isRemote() ? attacker.getBaseEntity() : user;
        final Vec3 pos = originEntity.position();
        for (LivingEntity target : targets) {
            if (net.arna.jcraft.common.util.JUtils.isBlocking(target)) {
                continue;
            }
            final Vec3 delta = target.position().subtract(pos);
            final Vec3 dir = delta.lengthSqr() > 1.0E-4 ? delta.normalize() : originEntity.getLookAngle();
            final Vec3 launchVec = dir.scale(1.3);
            final Vec3 impulse = new Vec3(launchVec.x, launchVec.y + 0.4, launchVec.z);

            if (net.arna.jcraft.common.util.JUtils.isAffectedByTimeStop(target)) {
                net.arna.jcraft.platform.JComponentPlatformUtils.getTimeStopData(target)
                        .ifPresent(ts -> ts.addTotalVelocity(impulse));
            } else {
                target.push(impulse.x, impulse.y, impulse.z);
                target.syncVelocity = true;
                if (target instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                }
            }
        }

        if (net.arna.jcraft.common.config.JServerConfig.SPTW_IGNITE_CAMPFIRES.getValue()
                && originEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            final net.minecraft.core.BlockPos center = originEntity.blockPosition();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        final net.minecraft.core.BlockPos checkPos = center.offset(dx, dy, dz);
                        final net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(checkPos);
                        if (state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                                && !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
                                && !state.getValue(net.minecraft.world.level.block.CampfireBlock.WATERLOGGED)
                                && net.arna.jcraft.common.util.JUtils.mayAlter(serverLevel, user, checkPos, null)) {
                            serverLevel.setBlockAndUpdate(checkPos, state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
                        }
                    }
                }
            }
        }

        return targets;
    }

    @Override
    protected @NonNull SPTWGroundSlamAttack<A> getThis() {
        return this;
    }

    @Override
    public @NonNull SPTWGroundSlamAttack<A> copy() {
        return copyExtras(new SPTWGroundSlamAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<SPTWGroundSlamAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<SPTWGroundSlamAttack<?>>, SPTWGroundSlamAttack<?>> buildCodec(RecordCodecBuilder.Instance<SPTWGroundSlamAttack<?>> instance) {
            return attackDefault(instance, SPTWGroundSlamAttack::new);
        }
    }
}
