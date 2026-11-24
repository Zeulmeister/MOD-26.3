package net.arna.jcraft.common.network.c2s;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.vehicle.AbstractGroundVehicleEntity;
import net.arna.jcraft.common.events.JServerPlayerInputEvent;
import net.arna.jcraft.common.item.Peacemaker;
import net.arna.jcraft.common.network.s2c.ServerChannelFeedbackPacket;
import net.arna.jcraft.common.util.*;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

import static net.arna.jcraft.JCraft.QUEUE_MOVESTUN_LIMIT;
import static net.arna.jcraft.JCraft.SPEC_QUEUE_MOVESTUN_LIMIT;

public class PlayerInputPacket {
    private static final int HOLD_TIMEOUT_TICKS = 20; // 1.0s safety lease while held heartbeats arrive
    private static final Map<ServerPlayer, Object2BooleanMap<MoveInputType>> successMap = new WeakHashMap<>();
    private static final int MOVEMENT_INPUT_TYPES;

    static {
        MOVEMENT_INPUT_TYPES = MovementInputType.values().length;
        TickEvent.SERVER_PRE.register(PlayerInputPacket::tick);
    }

    public static FriendlyByteBuf write(Object2BooleanMap<MovementInputType> movementInput, Object2BooleanMap<MoveInputType> moveInput) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writeInput(buf, movementInput);
        writeInput(buf, moveInput);
        return buf;
    }

    private static void writeInput(FriendlyByteBuf buf, @Nullable Object2BooleanMap<? extends Enum<?>> input) {
        if (input == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(input.size());
        for (Object2BooleanMap.Entry<? extends Enum<?>> entry : input.object2BooleanEntrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        int moveVecCount = buf.readVarInt();
        if (moveVecCount < 0 || moveVecCount > MOVEMENT_INPUT_TYPES) {
            server.execute(() -> player.connection.disconnect(Component.nullToEmpty("Illegal input packet!")));
            return;
        }
        MovementInputType[] movementTypes = new MovementInputType[moveVecCount];
        boolean[] movementStates = new boolean[moveVecCount];
        for (int i = 0; i < moveVecCount; i++) {
            movementTypes[i] = buf.readEnum(MovementInputType.class);
            movementStates[i] = buf.readBoolean();
        }

        int moveCount = buf.readVarInt();
        if (moveCount < 0 || moveCount > MoveInputType.types) {
            server.execute(() -> player.connection.disconnect(Component.nullToEmpty("Illegal input packet!")));
            return;
        }
        MoveInputType[] moveTypes = new MoveInputType[moveCount];
        boolean[] moveStates = new boolean[moveCount];
        for (int i = 0; i < moveCount; i++) {
            moveTypes[i] = buf.readEnum(MoveInputType.class);
            moveStates[i] = buf.readBoolean();
        }

        server.execute(() -> {
            InputStateManager sm = getInputStateManager(player);
            applyMovementInput(server, player, sm, movementTypes, movementStates);
            applyMoveInput(server, player, sm, moveTypes, moveStates);
        });
    }

    public static void handleHold(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        buf.readVarInt(); // Throwaway Movement input data
        int count = buf.readVarInt();
        if (count < 0 || count > MoveInputType.types) {
            server.execute(() -> player.connection.disconnect(Component.nullToEmpty("Illegal input packet!")));
            return;
        }

        MoveInputType[] types = new MoveInputType[count];
        for (int i = 0; i < count; i++) {
            types[i] = buf.readEnum(MoveInputType.class);
            buf.readBoolean(); // Throwaway hold input data
        }

        server.execute(() -> {
            InputStateManager sm = getInputStateManager(player);
            for (MoveInputType type : types) {
                if (JUtils.canHoldMove(player, type)) {
                    sm.heldInputs.put(type, HOLD_TIMEOUT_TICKS);
                }
            }
        });
    }

    private static void applyMovementInput(MinecraftServer server, ServerPlayer player, InputStateManager sm,
                                           MovementInputType[] types, boolean[] pressedStates) {
        for (int i = 0; i < types.length; i++) {
            MovementInputType type = types[i];
            boolean pressed = pressedStates[i];

            switch (type) {
                case FORWARD -> sm.forward = pressed;
                case BACKWARD -> sm.backward = pressed;
                case LEFT -> sm.left = pressed;
                case RIGHT -> sm.right = pressed;
                case JUMP -> sm.jumping = pressed;
                case CROUCH -> sm.sneaking = pressed;
                case DASH -> {
                    sm.dashing = pressed;
                    if (pressed) {
                        DashData.tryDash(sm.calcForward(), sm.calcSide(), player);
                    }
                }
            }
        }

        if (sm.jumping) {
            checkComboBreak(player);
        }
    }

    private static void applyMoveInput(MinecraftServer server, ServerPlayer player, InputStateManager sm,
                                       MoveInputType[] types, boolean[] pressedStates) {
        for (int i = 0; i < types.length; i++) {
            MoveInputType type = types[i];
            boolean pressed = pressedStates[i];

            if (pressed) {
                if (JUtils.canHoldMove(player, type)) {
                    sm.heldInputs.put(type, HOLD_TIMEOUT_TICKS);
                }
                boolean b = executeMoveInput(player, type);
                successMap.computeIfAbsent(player, p -> new Object2BooleanOpenHashMap<>()).put(type, b);
                dispatchUserMoveInput(player, type, true, b);
            } else {
                sm.heldInputs.remove(type);
                boolean b = successMap.computeIfAbsent(player, p -> new Object2BooleanOpenHashMap<>()).getOrDefault(type, false);
                dispatchUserMoveInput(player, type, false, b);
            }
        }
    }

    private static void dispatchUserMoveInput(ServerPlayer player, MoveInputType type, boolean pressed, boolean initiated) {
        JServerPlayerInputEvent.EVENT.invoker().onPlayerInput(player, type, pressed, initiated);
        boolean success = initiated;

        StandEntity<?, ?> stand = JUtils.getStand(player);
        if (stand != null && stand.allowMoveHandling()) {
            stand.onUserMoveInput(type, pressed, success);
            success = false; // If a stand is out, the move input success belongs to it.
        }

        JSpec<?, ?> spec = JUtils.getSpec(player);
        if (spec != null) {
            spec.onUserMoveInput(type, pressed, success);
        }
    }

    private static boolean executeMoveInput(ServerPlayer player, MoveInputType type) {
        ServerLevel world = (ServerLevel) player.level();
        switch (type) {
            case STAND_SUMMON -> {
                CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(player);
                StandEntity<?, ?> stand = standData.getStand();
                if (stand != null) {
                    int moveStun = stand.getMoveStun();
                    if (moveStun > 0 && moveStun < QUEUE_MOVESTUN_LIMIT) {
                        stand.queueMove(MoveInputType.STAND_SUMMON);
                        return false;
                    } else {
                        stand.desummon();
                        return true;
                    }
                } else if (world != null) {
                    JCraft.summon(world, player);
                    return true;
                }
                return false;
            }
            case LIGHT -> {
                if (Peacemaker.handleLeftClick(player, null)) {
                    return true;
                }
                return initStandOrSpecMove(player, MoveInputType.LIGHT);
            }
            case TOSS -> {
                if (Peacemaker.handleReloadInput(player)) {
                    return true;
                }
                return initStandOrSpecMove(player, MoveInputType.TOSS);
            }
            case UTILITY -> {
                StandEntity<?, ?> stand = JUtils.getStand(player);
                if (stand != null) {
                    return initStandMove(stand, MoveInputType.UTILITY);
                } else {
                    StandEntity<?, ?> stand2 = JCraft.summon(world, player);
                    if (stand2 != null && !stand2.wantToBlock) {
                        return stand2.initMove(MoveClass.UTILITY);
                    }
                    return false;
                }
            }
            default -> {
                return initStandOrSpecMove(player, type);
            }
        }
    }

    private static boolean initStandOrSpecMove(ServerPlayer player, MoveInputType type) {
        StandEntity<?, ?> stand = JUtils.getStand(player);

        boolean standExists = stand != null;
        boolean forwardToSpec = !standExists || !stand.allowMoveHandling() || stand.forwardInputToSpec(type);
        boolean success = false;
        
        if (standExists && stand.allowMoveHandling()) {
            success = initStandMove(stand, type);
        }

        if (forwardToSpec) {
            JSpec<?, ?> spec = JUtils.getSpec(player);

            if (spec == null) {
                return success;
            } else if (spec.initMove(type.getMoveClass())) {
                success = true;
            } else if (spec.moveStun > 0 && spec.moveStun < SPEC_QUEUE_MOVESTUN_LIMIT) {
                spec.queuedMove = type;
            }
        }

        return success;
    }

    private static boolean initStandMove(StandEntity<?, ?> stand, MoveInputType type) {
        if (!stand.blocking) {
            if (!stand.acceptsUserMoveInit(type))
                return false;

            int moveStun = stand.getMoveStun();

            if (stand.initMove(type.getMoveClass()))
                return true;

            if (moveStun > 0 && moveStun < QUEUE_MOVESTUN_LIMIT)
                stand.queueMove(type);
        }

        return false;
    }

    private static void checkComboBreak(ServerPlayer player) {
        // Combo break if stunned, jumping and crouching
        InputStateManager sm = getInputStateManager(player);
        final boolean blocking = JUtils.isBlocking(player);

        if (sm == null || !sm.jumping || !player.isShiftKeyDown()) {
            return;
        }

        if (blocking) {
            StandEntity<?, ?> stand = JUtils.getStand(player);

            // This check is redundant, but I'm putting it here if we add spec blocking in the future.
            if (stand == null) {
                JCraft.LOGGER.warn("Player " + player + " was blocking despite having no stand?");
            } else {
                if (stand.getMoveStun() < 2) {
                    return;
                }
                // Else is in blockstun due to an attack

                JCraft.tryPushBlock((ServerLevel) player.level(), player, stand);
            }
        } else {
            MobEffectInstance stun = player.getEffect(JStatusRegistry.DAZED.asHolder());
            if (stun != null) {
                JCraft.comboBreak((ServerLevel) player.level(), player, stun);
            }
        }
    }

    public static InputStateManager getInputStateManager(ServerPlayer player) {
        return ((IJInputStateManagerHolder) player).jcraft$getJInputStateManager();
    }

    private static void tick(MinecraftServer instance) {
        for (ServerPlayer player : instance.getPlayerList().getPlayers()) {
            InputStateManager sm = getInputStateManager(player);

            // Handle held inputs (lease countdown only; never re-trigger moves every tick while held)
            if (!sm.heldInputs.isEmpty()) {
                new ArrayList<>(sm.heldInputs.entrySet()).forEach(entry ->
                        handleHeldInput(player, entry.getKey(), entry.getValue(), sm));
            }

            int forward = sm.calcForward();
            int side = sm.calcSide();
            JComponentPlatformUtils.getMiscData(player).updateRemoteInputs(forward, side, sm.jumping);

            StandEntity<?, ?> stand = JUtils.getStand(player);
            if (stand != null) {
                stand.updateRemoteInputs(forward, side, sm.jumping, sm.sneaking);
            }

            if (player.getVehicle() instanceof AbstractGroundVehicleEntity groundVehicle) {
                groundVehicle.updateInputs(forward, side, sm.jumping, sm.sneaking);
            }

            if (sm.dashing) {
                DashData.tryDash(forward, side, player);
            }

            if (!sm.jumping) {
                continue;
            }

            if (DashData.isDashing(player))
            // 5s cooldown for superjumping
            {
                JComponentPlatformUtils.getCooldowns(player).setCooldown(CooldownType.DASH, 100);
            }

            checkComboBreak(player);
        }
    }

    private static void handleHeldInput(ServerPlayer player, MoveInputType type, Integer integer, InputStateManager sm) {
        if (integer <= 0) {
            sm.heldInputs.remove(type);
            return;
        }
        int newValue = integer - 1;
        // JUtils.canHoldMove() may change after a holdable button was pressed if the player swaps their abilities
        if (newValue <= 0 || !JUtils.canHoldMove(player, type)) {
            sm.heldInputs.remove(type);
            boolean initiated = successMap.computeIfAbsent(player, p -> new Object2BooleanOpenHashMap<>()).getOrDefault(type, false);
            dispatchUserMoveInput(player, type, false, initiated);
        } else {
            sm.heldInputs.put(type, newValue);
        }
    }
}
