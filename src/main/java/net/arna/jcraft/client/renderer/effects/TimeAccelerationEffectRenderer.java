package net.arna.jcraft.client.renderer.effects;

import lombok.experimental.UtilityClass;
import net.arna.jcraft.common.network.s2c.TimeAccelStatePacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.gamerules.GameRules;

@UtilityClass
public class TimeAccelerationEffectRenderer {

    public static void render(final ClientLevel world) {
        

        TimeAccelStatePacket.applyAcceleration(world, t -> {});
    }
}
