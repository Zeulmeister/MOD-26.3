package net.arna.jcraft.common.attack.moves.shared;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.attack.moves.AbstractBarrageAttack;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A simple attack that performs at a set interval.
 * The user may crouch to break blocks.
 */
public final class MainBarrageAttack<A extends IAttacker<? extends A, ?>> extends AbstractBarrageAttack<MainBarrageAttack<A>, A> {
    private static final int MINING_BARRAGE_TIME = 200;
    @Getter
    private final float mineableHardness;
    private final int baseStun, baseDuration;
    private boolean breakBlocks = false;

    public MainBarrageAttack(final int cooldown, final int windup, final int duration, final float moveDistance, final float damage, final int stun,
                             final float hitboxSize, final float knockback, final float offset, final int interval, final float mineableHardness) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset, interval);
        this.mineableHardness = mineableHardness;
        this.baseDuration = duration;
        this.baseStun = stun;
        withShockwaves();
        withHoldable();
    }

    @Override
    public @NonNull MoveType<MainBarrageAttack<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public void onInitiate(final A attacker) {
        breakBlocks = attacker.getUserOrThrow().isShiftKeyDown() && JServerConfig.MINING_BARRAGE.getValue();
        if (!mayBreak(attacker.getUser(), null)) {
            breakBlocks = false;
        }

        withDuration(breakBlocks ? MINING_BARRAGE_TIME : baseDuration);
        super.onInitiate(attacker);
        withStun(breakBlocks ? 1 : baseStun);
    }

    @Override
    public boolean canFinish(final A attacker) {
        if (breakBlocks) {
            return false;
        }
        return super.canFinish(attacker);
    }

    @Override
    public void onUserMoveInput(final A attacker, final MoveInputType type, final boolean pressed, final boolean moveInitiated) {
        super.onUserMoveInput(attacker, type, pressed, moveInitiated);
        // Instant barrage cancel on button release + stop barrage sound
        if (type.getMoveClass() == getMoveClass() && !pressed) {
            attacker.cancelMove();
            if (attacker.getUser() instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, net.minecraft.sounds.SoundSource.PLAYERS));
            }
        }
    }

    @Override
    protected @NonNull MainBarrageAttack<A> getThis() {
        return this;
    }

    @Override
    protected boolean shouldUseSimpleBlockBreakage() {
        return !breakBlocks;
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        final Set<LivingEntity> targets = super.perform(attacker, user);

        if (breakBlocks) {
            final ServerLevel serverWorld = (ServerLevel) user.level();
            final net.minecraft.world.phys.BlockHitResult hit = raycastAimedBlock(serverWorld, attacker, 4.5);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                tryBreak(serverWorld, hit.getBlockPos(), user);
            }
        }

        if (attacker.getBaseEntity().level() instanceof ServerLevel serverLevel) {
            final Vec3 strikePos = attacker.getBaseEntity().position().add(0.0, 1.2, 0.0);
            serverLevel.sendParticles(
                    net.arna.jcraft.api.registry.JParticleTypeRegistry.DO.get(),
                    strikePos.x, strikePos.y, strikePos.z,
                    2, 0.4, 0.4, 0.4, 0.02
            );
        }

        return targets;
    }

    private void tryBreak(final ServerLevel world, final BlockPos pos, final LivingEntity user) {
        if (mayBreak(user, pos, b -> b.getBlock().defaultDestroyTime() <= mineableHardness)) {
            world.destroyBlock(pos, true, user);
        }
    }

    @Override
    public @NonNull MainBarrageAttack<A> copy() {
        return copyExtras(new MainBarrageAttack<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(), getStun(),
                getHitboxSize(), getKnockback(), getOffset(), getInterval(), mineableHardness));
    }

    public static class Type extends AbstractBarrageAttack.Type<MainBarrageAttack<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<MainBarrageAttack<?>>, MainBarrageAttack<?>> buildCodec(RecordCodecBuilder.Instance<MainBarrageAttack<?>> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(),
                    stun(), hitboxSize(), knockback(), offset(), interval(),
                    Codec.FLOAT.fieldOf("mineable_hardness").forGetter(MainBarrageAttack::getMineableHardness))
                    .apply(instance, applyAttackExtras(MainBarrageAttack::new));
        }
    }
}
