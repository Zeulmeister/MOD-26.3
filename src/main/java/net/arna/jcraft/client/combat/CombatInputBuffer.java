package net.arna.jcraft.client.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.network.JNetwork;
import net.arna.jcraft.common.network.c2s.PlayerInputPacket;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

/**
 * 6-Tick Smart Combat Input Buffer (Minecraft 26.3 Upgrade):
 * Captures move inputs pressed near the end of moveStun or stun recovery ([1..6] ticks remaining)
 * and automatically dispatches the buffered attack on frame 0 of recovery for fluid anime combos.
 */
public final class CombatInputBuffer {
    public static final int BUFFER_WINDOW_TICKS = 6;

    @Nullable
    private static MoveInputType bufferedMove = null;
    private static int remainingTicks = 0;

    private CombatInputBuffer() {}

    public static void recordInput(@Nullable MoveInputType moveInput) {
        if (moveInput == null || moveInput == MoveInputType.STAND_SUMMON) {
            return;
        }
        bufferedMove = moveInput;
        remainingTicks = BUFFER_WINDOW_TICKS;
    }

    public static boolean hasBufferedMove() {
        return bufferedMove != null && remainingTicks > 0;
    }

    @Nullable
    public static MoveInputType peekBufferedMove() {
        return hasBufferedMove() ? bufferedMove : null;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }

    public static void clear() {
        bufferedMove = null;
        remainingTicks = 0;
    }

    /**
     * Pure state machine step for unit testing & client tick execution:
     * returns the buffered MoveInputType when canActNow becomes true within the 6-tick window.
     */
    @Nullable
    public static MoveInputType stepBuffer(boolean canActNow) {
        if (bufferedMove == null || remainingTicks <= 0) {
            clear();
            return null;
        }
        if (canActNow) {
            MoveInputType ready = bufferedMove;
            clear();
            return ready;
        }
        remainingTicks--;
        if (remainingTicks <= 0) {
            clear();
        }
        return null;
    }

    public static void onClientTick(LocalPlayer player, Object2BooleanMap<MoveInputType> freshPresses, boolean enabled) {
        if (!enabled || !player.isAlive()) {
            clear();
            return;
        }

        StandEntity<?, ?> stand = JUtils.getStand(player);
        JSpec<?, ?> spec = JUtils.getSpec(player);
        MobEffectInstance dazed = player.getEffect(JStatusRegistry.DAZED.asHolder());

        int recoveryTicks = 0;
        if (stand != null && stand.getMoveStun() > 0) {
            recoveryTicks = Math.max(recoveryTicks, stand.getMoveStun());
        } else if (spec != null && spec.moveStun > 0) {
            recoveryTicks = Math.max(recoveryTicks, spec.moveStun);
        }
        if (dazed != null && dazed.getDuration() > 0) {
            recoveryTicks = Math.max(recoveryTicks, dazed.getDuration());
        }

        // Record newly pressed attack inputs when player is within [1..6] ticks of recovery
        if (recoveryTicks >= 1 && recoveryTicks <= BUFFER_WINDOW_TICKS) {
            for (Object2BooleanMap.Entry<MoveInputType> entry : freshPresses.object2BooleanEntrySet()) {
                if (entry.getBooleanValue()) {
                    recordInput(entry.getKey());
                }
            }
        }

        boolean canActNow = recoveryTicks == 0;
        MoveInputType toFire = stepBuffer(canActNow);
        if (toFire != null) {
            Object2BooleanMap<MoveInputType> replay = new Object2BooleanOpenHashMap<>();
            replay.put(toFire, true);
            JNetwork.sendToServer(JPacketRegistry.C2S_PLAYER_INPUT, PlayerInputPacket.write(null, replay));
        }
    }
}
