package net.arna.jcraft.client.remote;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.common.network.c2s.RemoteControlTogglePacket;
import net.arna.jcraft.common.network.c2s.StandRotationPacket;
import net.arna.jcraft.common.util.JUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Full Pilot Mode client handler (O key): switches camera to StandEntity, syncs 3D movement/rotation,
 * and alerts the player if their vulnerable physical body takes damage.
 */
@Environment(EnvType.CLIENT)
public final class StandRemoteControlHandler {
    private static StandEntity<?, ?> controlledStand = null;
    private static int bodyDangerTicks = 0;
    private static float lastPlayerHealth = 20.0F;

    private StandRemoteControlHandler() {}

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            resetCamera(client);
            return;
        }

        if (client.gui.screen() == null && JCraftClient.PILOT_MODE_KEY != null && JCraftClient.PILOT_MODE_KEY.isPressedThisTick()) {
            RemoteControlTogglePacket.send();
        }

        StandEntity<?, ?> stand = JUtils.getStand(player);
        boolean shouldControl = stand != null && stand.isAlive() && stand.isRemoteAndControllable();

        if (shouldControl) {
            if (client.getCameraEntity() != stand) {
                client.setCameraEntity(stand);
                controlledStand = stand;
                lastPlayerHealth = player.getHealth();
                bodyDangerTicks = 0;
            }

            if (controlledStand != null && controlledStand.isAlive()) {
                // Feed local WASD + Jump + Sneak inputs directly to client stand for responsive 3D flight
                int f = (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
                int s = (client.options.keyLeft.isDown() ? 1 : 0) - (client.options.keyRight.isDown() ? 1 : 0);
                boolean jump = client.options.keyJump.isDown();
                boolean sneak = client.options.keyShift.isDown();
                controlledStand.updateRemoteInputs(f, s, jump, sneak);

                controlledStand.yHeadRotO = controlledStand.yHeadRot;
                controlledStand.yBodyRotO = controlledStand.yBodyRot;
                controlledStand.xRotO = controlledStand.getXRot();
                controlledStand.yRotO = controlledStand.getYRot();

                StandRotationPacket.send(
                        controlledStand.getX(),
                        controlledStand.getY(),
                        controlledStand.getZ(),
                        controlledStand.getYRot(),
                        controlledStand.getXRot()
                );
            }

            if (player.hurtTime > 0 || player.getHealth() < lastPlayerHealth) {
                if (bodyDangerTicks <= 0) {
                    player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 0.6F);
                    player.playSound(SoundEvents.SHIELD_BLOCK.value(), 0.9F, 0.8F);
                }
                bodyDangerTicks = 80;
            }

            lastPlayerHealth = player.getHealth();
            if (bodyDangerTicks > 0) {
                bodyDangerTicks--;
            }
        } else {
            if (controlledStand != null || (client.getCameraEntity() != null && client.getCameraEntity() != player)) {
                resetCamera(client);
            }
            bodyDangerTicks = 0;
            lastPlayerHealth = player.getHealth();
        }
    }

    public static void resetCamera(Minecraft client) {
        if (client.player != null) {
            client.player.resetFallDistance();
            if (client.getCameraEntity() != client.player) {
                client.setCameraEntity(client.player);
            }
        }
        controlledStand = null;
        bodyDangerTicks = 0;
    }

    public static boolean isControlling(Minecraft client) {
        return controlledStand != null
                && client.getCameraEntity() == controlledStand
                && controlledStand.isAlive()
                && controlledStand.isRemoteAndControllable();
    }

    @Nullable
    public static StandEntity<?, ?> getControlledStand() {
        return controlledStand;
    }

    public static int getBodyDangerTicks() {
        return bodyDangerTicks;
    }
}
