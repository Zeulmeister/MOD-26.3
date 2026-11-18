package net.arna.jcraft.common.attack.moves.dirtydeedsdonedirtcheap;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.common.tickable.PastDimensions;
import net.arna.jcraft.api.registry.JDimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class DimensionalHopMove<A extends IAttacker<? extends A, ?>> extends AbstractSimpleAttack<DimensionalHopMove<A>, A> {

    @Getter
    private final int dimensionalHopDuration;

    public DimensionalHopMove(final int cooldown, final int windup, final int duration, final float moveDistance,
                              final float damage, final int stun, final float hitboxSize, final float knockback, final float offset,
                              final int dimensionalHopDuration) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
        if (dimensionalHopDuration < 0) {
            throw new IllegalArgumentException("dimensionHopTime cannot be negative!");
        }
        this.dimensionalHopDuration = dimensionalHopDuration;
    }

    @Override
    public @NotNull MoveType<DimensionalHopMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public boolean conditionsMet(A attacker) {
        return super.conditionsMet(attacker) || attacker.getEntityWorld().dimension().equals(JDimensionRegistry.AU_DIMENSION_KEY);
    }

    @Override
    public void onInitiate(final A attacker) {
        super.onInitiate(attacker);

        final LivingEntity baseEntity = attacker.getBaseEntity();
        if (baseEntity.level() != JCraft.auWorld) {
            JCraft.auWorld.getChunkSource().addTicketWithRadius(TicketType.PORTAL, baseEntity.chunkPosition(), 2);
            JCraft.preloadLockTicks = getWindup();
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        final Set<LivingEntity> targets = super.perform(attacker, user);
        if (dimensionalHopDuration == 0) {
            return targets;
        }

        final ServerLevel world = (ServerLevel) attacker.getEntityWorld();

        if (world.dimension().equals(JDimensionRegistry.AU_DIMENSION_KEY)) {
            // Logic for cancelling dimhop early, and generating failsafe data
            if (!(user instanceof ServerPlayer serverPlayer)) {
                return Set.of();
            }

            boolean isStored = PastDimensions.tryExit(user, targets); // Should always be true
            if (!isStored) { // If not stored, force your way back
                PastDimensions.safeReturn(serverPlayer);
            }

            return Set.of();
        }

        if (JCraft.auWorld == null) {
            throw new IllegalStateException("Alternate Universe could not be found.");
        }

        final LivingEntity baseEntity = attacker.getBaseEntity();
        fixLightInAU(baseEntity, world, JCraft.auWorld);

        Set<LivingEntity> toHop = new HashSet<>(targets);
        toHop.add(user);
        int heightOffset = JCraft.auWorld.getHeight() - world.getHeight();
        for (LivingEntity entity : toHop) {
            JCraft.dimensionHop(entity, heightOffset / 2, dimensionalHopDuration);
        }

        return targets;
    }

    static final boolean enableLightingFix = false;

    //todo: fix fixLightInAU() crashing the server repeatedly (its currently not called)
    @SuppressWarnings("DataFlowIssue") // There is no issue
    private static void fixLightInAU(final LivingEntity attacker, final ServerLevel world, final ServerLevel auWorld) {
        if (auWorld == null) return;
    }

    @Override
    protected @NonNull DimensionalHopMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull DimensionalHopMove<A> copy() {
        return copyExtras(new DimensionalHopMove<>(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset(), getDimensionalHopDuration()));
    }

    public static class Type extends AbstractSimpleAttack.Type<DimensionalHopMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<DimensionalHopMove<?>>, DimensionalHopMove<?>> buildCodec(RecordCodecBuilder.Instance<DimensionalHopMove<?>> instance) {
            return instance.group(extras(), attackExtras(), cooldown(), windup(), duration(), moveDistance(), damage(),
                    stun(), hitboxSize(), knockback(), offset(),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("dimensionalHopDuration")
                            .forGetter(DimensionalHopMove::getDimensionalHopDuration))
                    .apply(instance, applyAttackExtras(DimensionalHopMove::new));
        }
    }
}
