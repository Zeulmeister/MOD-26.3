package net.arna.jcraft.common.attack.moves.dirtydeedsdonedirtcheap;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.registry.JDimensionRegistry;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.common.tickable.PastDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        copyTerrainToAU(baseEntity, world, JCraft.auWorld);

        Set<LivingEntity> toHop = new HashSet<>(targets);
        toHop.add(user);
        int heightOffset = JCraft.auWorld.getHeight() - world.getHeight();
        for (LivingEntity entity : toHop) {
            JCraft.dimensionHop(entity, heightOffset / 2, dimensionalHopDuration);
        }

        return targets;
    }

    private static void copyTerrainToAU(final LivingEntity attacker, final ServerLevel world, final ServerLevel auWorld) {
        if (auWorld == null || world == auWorld) {
            return;
        }

        final int radius = 2;
        final ChunkPos origin = attacker.chunkPosition();
        final int heightOffset = (auWorld.getHeight() - world.getHeight()) / 2;
        final boolean sectionAligned = (heightOffset & 15) == 0;
        final ThreadedLevelLightEngine lightEngine = auWorld.getChunkSource().getLightEngine();
        final EnumSet<Heightmap.Types> heightmapsToPrime = EnumSet.of(
                Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.OCEAN_FLOOR,
                Heightmap.Types.WORLD_SURFACE
        );

        final BlockPos.MutableBlockPos srcCursor = new BlockPos.MutableBlockPos();
        final BlockState air = Blocks.AIR.defaultBlockState();
        final List<LevelChunk> copiedChunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                final int cx = origin.x() + dx;
                final int cz = origin.z() + dz;
                final ChunkPos chunkPos = new ChunkPos(cx, cz);

                JCraft.preloadChunk(auWorld, cx, cz);

                final LevelChunk srcChunk = world.getChunk(cx, cz);
                final LevelChunk dstChunk = auWorld.getChunk(cx, cz);

                for (BlockPos bePos : new ArrayList<>(dstChunk.getBlockEntitiesPos())) {
                    dstChunk.removeBlockEntity(bePos);
                }

                final LevelChunkSection[] srcSections = srcChunk.getSections();
                final LevelChunkSection[] dstSections = dstChunk.getSections();

                for (int dstIdx = 0; dstIdx < dstSections.length; dstIdx++) {
                    final LevelChunkSection dstSec = dstSections[dstIdx];
                    final int dstMinY = dstChunk.getSectionYFromSectionIndex(dstIdx) << 4;

                    if (sectionAligned) {
                        final int srcIdx = srcChunk.getSectionIndex(dstMinY + heightOffset);
                        if (srcIdx < 0 || srcIdx >= srcSections.length || srcSections[srcIdx].hasOnlyAir()) {
                            if (!dstSec.hasOnlyAir()) {
                                clearSection(dstSec, air);
                            }
                            continue;
                        }
                        final LevelChunkSection srcSec = srcSections[srcIdx];
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    BlockState state = srcSec.getBlockState(x, y, z);
                                    if (state.is(JTagRegistry.AU_REPLACED_WITH_AIR)) {
                                        state = air;
                                    }
                                    dstSec.setBlockState(x, y, z, state, false);
                                }
                            }
                        }
                        dstSec.recalcBlockCounts();
                    } else {
                        for (int y = 0; y < 16; y++) {
                            final int srcY = dstMinY + y + heightOffset;
                            final boolean validY = !world.isOutsideBuildHeight(srcY);
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    BlockState state = air;
                                    if (validY) {
                                        srcCursor.set((cx << 4) + x, srcY, (cz << 4) + z);
                                        state = srcChunk.getBlockState(srcCursor);
                                        if (state.is(JTagRegistry.AU_REPLACED_WITH_AIR)) {
                                            state = air;
                                        }
                                    }
                                    dstSec.setBlockState(x, y, z, state, false);
                                }
                            }
                        }
                        dstSec.recalcBlockCounts();
                    }
                }

                Heightmap.primeHeightmaps(dstChunk, heightmapsToPrime);
                dstChunk.setLightCorrect(false);
                dstChunk.initializeLightSources();
                dstChunk.markUnsaved();

                lightEngine.setLightEnabled(chunkPos, true);
                for (int dstIdx = 0; dstIdx < dstSections.length; dstIdx++) {
                    lightEngine.updateSectionStatus(
                            SectionPos.of(chunkPos, dstChunk.getSectionYFromSectionIndex(dstIdx)),
                            dstSections[dstIdx].hasOnlyAir()
                    );
                }
                lightEngine.propagateLightSources(chunkPos);
                copiedChunks.add(dstChunk);
            }
        }

        for (final LevelChunk dstChunk : copiedChunks) {
            final ChunkPos chunkPos = dstChunk.getPos();
            lightEngine.lightChunk(dstChunk, false).thenRunAsync(() -> {
                final List<ServerPlayer> watchers = auWorld.getChunkSource().chunkMap.getPlayers(chunkPos, false);
                if (!watchers.isEmpty()) {
                    final ClientboundLevelChunkWithLightPacket packet =
                            new ClientboundLevelChunkWithLightPacket(dstChunk, lightEngine, null, null);
                    for (ServerPlayer watcher : watchers) {
                        watcher.connection.send(packet);
                    }
                }
            }, auWorld.getServer());
        }
    }

    private static void clearSection(final LevelChunkSection section, final BlockState air) {
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    section.setBlockState(x, y, z, air, false);
                }
            }
        }
        section.recalcBlockCounts();
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
