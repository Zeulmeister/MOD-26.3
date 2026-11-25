package net.arna.jcraft.common.attack.moves.kingcrimson;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.common.entity.stand.KingCrimsonEntity;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class PredictionMove<A extends StandEntity<? extends A, ?>> extends AbstractMove<PredictionMove<A>, A> {
    private final Map<Entity, Vec3> predictionInfo = new WeakHashMap<>();

    public PredictionMove(final int cooldown, final int windup, final int duration, final float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NonNull MoveType<PredictionMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public boolean canBeQueued(A attacker) {
        return false;
    }

    @Override
    public void onInitiate(final A attacker) {
        super.onInitiate(attacker);

        predictionInfo.clear();

        // Send epitaph state start
        if (attacker.getUser() instanceof ServerPlayer player) {
            net.arna.jcraft.common.network.JNetwork.sendToPlayer(player, JPacketRegistry.S2C_EPITAPH_STATE,
                    new FriendlyByteBuf(Unpooled.buffer().writeBoolean(true)));
        }
    }

    @Override
    public void activeTick(final A attacker, final int moveStun) {
        super.activeTick(attacker, moveStun);

        if (moveStun == getWindupPoint()) {
            beginPrediction(attacker); // Clientside prediction, serverside is in specialAttack()
        }

        if (attacker.tickCount % 2 != 0) return;

        tickPredictions(attacker);
        if (attacker.hasUser()) {
            attacker.getUserOrThrow().addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    10, 2, true, false));
        }
    }

    @Override
    public @NonNull Set<LivingEntity> perform(final A attacker, final LivingEntity user) {
        return Set.of();
    }

    @Override
    public boolean onInitMove(A attacker, MoveClass moveClass) {
        if (moveClass != MoveClass.ULTIMATE || !attacker.hasUser()) return false;

        LivingEntity user = attacker.getUserOrThrow();
        final CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(user);
        if (cooldowns.getCooldown(CooldownType.STAND_ULTIMATE) <= 0) {
            cooldowns.setCooldown(CooldownType.STAND_ULTIMATE, 20);
            finishPrediction(attacker);
        }

        return true;
    }

    public void beginPrediction(final A attacker) {
        if (!(attacker.getUser() instanceof ServerPlayer player)) {
            return;
        }

        for (final Entity entity : getEntitiesToCatch(attacker.level(), attacker, player)) {
            predictionInfo.put(entity, entity.position());
        }

        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(true);
        buf.writeVarInt(getWindupPoint());
        net.arna.jcraft.common.network.JNetwork.sendToPlayer(player, JPacketRegistry.S2C_TIME_ERASE_PREDICTION_STATE, buf);
    }

    public void finishPrediction(final A attacker) {
        for (Map.Entry<Entity, Vec3> prediction : predictionInfo.entrySet()) {
            final Entity entity = prediction.getKey();
            if (entity == null) {
                continue;
            }

            final Vec3 pos = prediction.getValue();
            entity.teleportTo(pos.x, pos.y, pos.z);
        }

        cancelPrediction(attacker, predictionInfo);
        attacker.cancelMove();
    }

    public void cancelPrediction(final StandEntity<?, ?> attacker) {
        cancelPrediction(attacker, predictionInfo);
    }

    @Override
    public void onCancel(final A attacker) {
        super.onCancel(attacker);
        cancelPrediction(attacker);
    }

    @Override
    public void onDeactivate(final A attacker) {
        super.onDeactivate(attacker);
        cancelPrediction(attacker);
    }

    public static void cancelPrediction(final StandEntity<?, ?> attacker, final Map<Entity, Vec3> predictionInfo) {
        if (attacker.getUser() instanceof ServerPlayer player) {
            net.arna.jcraft.common.network.JNetwork.sendToPlayer(player, JPacketRegistry.S2C_EPITAPH_STATE, new FriendlyByteBuf(Unpooled.buffer().writeBoolean(false)));
            net.arna.jcraft.common.network.JNetwork.sendToPlayer(player, JPacketRegistry.S2C_TIME_ERASE_PREDICTION_STATE, new FriendlyByteBuf(Unpooled.buffer().writeBoolean(false)));
        }

        predictionInfo.clear();
    }

    public void tickPredictions(final A attacker) {
        final Map<Entity, Vec3> predictions = new HashMap<>(predictionInfo);
        updatePredictions(predictions, attacker.getMoveStun());
        predictionInfo.clear();
        predictionInfo.putAll(predictions);
    }

    public static List<Entity> getEntitiesToCatch(final Level world, final StandEntity<?, ?> stand, final Player player) {
        if (world == null || stand == null) {
            return Collections.emptyList();
        }

        return world.getEntitiesOfClass(Entity.class, stand.getBoundingBox().inflate(64),
                EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(e -> e != stand && e != player && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(e.getType()).is(JTagRegistry.UNAFFECTED_BY_EPITAPH)));
    }

    public static void updatePredictions(final Map<Entity, Vec3> predictions, final int ticksLeft) {
        final Set<Entity> updated = new HashSet<>();
        for (Entity entity : new ArrayList<>(predictions.keySet())) {
            updatePrediction(predictions, entity, updated, ticksLeft);
        }
    }

    private static void updatePrediction(final Map<Entity, Vec3> predictions, final Entity entity,
                                         final Set<Entity> updated, final int ticksLeft) {
        if (entity == null || !updated.add(entity)) {
            return;
        }
        if (!entity.isAlive() || ticksLeft <= 0) {
            if (entity != null && entity.isAlive()) {
                predictions.put(entity, entity.position());
            }
            return;
        }

        final Level world = entity.level();
        final Vec3 currentPos = entity.position();
        final int simTicks = Math.min(ticksLeft, 40);

        Vec3 velocity = entity instanceof Player player
                ? JComponentPlatformUtils.getMiscData(player).getDesiredVelocity()
                : entity.getDeltaMovement();

        final Vec3i gravityDir = GravityChangerAPI.getGravityDirection(entity).getUnitVec3i();
        final Vec3 gravityPerTick = new Vec3(gravityDir.getX(), gravityDir.getY(), gravityDir.getZ()).scale(0.08);
        final boolean inLiquid = entity.isInWater() || entity.isInLava();

        Vec3 futurePos = currentPos;
        Vec3 simVel = velocity;
        for (int t = 0; t < simTicks; t++) {
            if (!entity.onGround() && !inLiquid && !entity.isNoGravity()) {
                simVel = simVel.add(gravityPerTick);
            }
            futurePos = futurePos.add(simVel);
            simVel = new Vec3(simVel.x * 0.91, simVel.y * 0.98, simVel.z * 0.91);
        }

        final Entity vehicle = entity.getVehicle();
        if (vehicle != null && vehicle.isAlive()) {
            updatePrediction(predictions, vehicle, updated, ticksLeft);
            Vec3 vehiclePred = predictions.get(vehicle);
            if (vehiclePred != null) {
                futurePos = vehiclePred.add(0.0, entity.getY() - vehicle.getY(), 0.0);
            }
        }

        final Vec3 probeStart = currentPos.add(0.0, 0.1, 0.0);
        final Vec3 probeEnd = futurePos.add(0.0, 0.1, 0.0);
        if (probeStart.distanceToSqr(probeEnd) < 1.0E-6) {
            predictions.put(entity, currentPos);
            return;
        }

        BlockHitResult hitResult = world.clip(new ClipContext(
                probeStart, probeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, entity));
        Vec3 resolved = hitResult.getType() == BlockHitResult.Type.MISS
                ? futurePos
                : hitResult.getLocation().subtract(0.0, 0.1, 0.0);
        predictions.put(entity, resolved);
    }

    @Override
    protected @NonNull PredictionMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull PredictionMove<A> copy() {
        return copyExtras(new PredictionMove<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static class Type extends AbstractMove.Type<PredictionMove<?>> {
        public static final Type INSTANCE = new Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<PredictionMove<?>>, PredictionMove<?>> buildCodec(RecordCodecBuilder.Instance<PredictionMove<?>> instance) {
            return baseDefault(instance, PredictionMove::new);
        }
    }
}
