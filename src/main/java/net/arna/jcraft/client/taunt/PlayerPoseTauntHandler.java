package net.arna.jcraft.client.taunt;

import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer;
import net.arna.jcraft.client.rendering.handler.ColorShiftShaderHandler;
import net.arna.jcraft.common.network.c2s.PoseTauntPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side JoJo Pose Taunt handler (hold P for 1.5s / 30 ticks, or trigger instantly from Radial Wheel).
 */
public final class PlayerPoseTauntHandler {
    private static int poseProgressTicks = 0;
    public static final int REQUIRED_TICKS = 30;
    private static boolean wasTaunting = false;

    private PlayerPoseTauntHandler() {}

    public static boolean isPosing() {
        return poseProgressTicks > 0;
    }

    public static float getProgressRatio() {
        return Math.min(1.0F, poseProgressTicks / (float) REQUIRED_TICKS);
    }

    public static void triggerClientEffects(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;
        if (JClientConfig.getInstance().isColorShiftEnabled()) {
            ColorShiftShaderHandler.INSTANCE.trigger(50, 0.60F);
        }
        JoJoOnomatopoeiaRenderer.triggerBurst(true, 55);
        player.sendOverlayMessage(Component.translatable("jcraft.taunt.activated"));
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && player.isAlive() && client.gui.screen() == null) {
            boolean isKeyDown = JCraftClient.POSE_TAUNT_KEY != null && JCraftClient.POSE_TAUNT_KEY.isDown();
            if (isKeyDown) {
                ClientLevel level = client.level;
                if (!wasTaunting) {
                    wasTaunting = true;
                    poseProgressTicks = 0;
                    if (level != null) {
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 1.2F, false);
                    }
                }

                poseProgressTicks++;
                if (level != null && poseProgressTicks % 3 == 0) {
                    RandomSource rng = level.getRandom();
                    Vec3 pos = player.position();
                    double spread = player.getBbWidth() * 1.5;
                    level.addParticle(
                            JParticleTypeRegistry.GO.get(),
                            false, false,
                            pos.x + rng.triangle(0.0, spread),
                            pos.y + 0.3 + rng.nextDouble() * 1.6,
                            pos.z + rng.triangle(0.0, spread),
                            0.0, 0.04, 0.0
                    );
                }

                if (poseProgressTicks == REQUIRED_TICKS) {
                    PoseTauntPacket.send();
                    triggerClientEffects(client);
                    if (level != null) {
                        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.45F, false);
                    }
                }
            } else {
                poseProgressTicks = 0;
                wasTaunting = false;
            }
        } else {
            poseProgressTicks = 0;
            wasTaunting = false;
        }
    }
}
