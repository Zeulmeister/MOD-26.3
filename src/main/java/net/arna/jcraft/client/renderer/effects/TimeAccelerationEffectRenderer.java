package net.arna.jcraft.client.renderer.effects;

import lombok.experimental.UtilityClass;
import net.arna.jcraft.common.network.s2c.TimeAccelStatePacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClocks;

import java.util.Map;

@UtilityClass
public class TimeAccelerationEffectRenderer {

    public static void render(final ClientLevel world) {
        if (world == null) {
            return;
        }
        TimeAccelStatePacket.applyAcceleration(world, ticks ->
                world.registryAccess()
                        .lookupOrThrow(Registries.WORLD_CLOCK)
                        .get(WorldClocks.OVERWORLD)
                        .ifPresent(clock -> world.clockManager().handleUpdates(
                                world.getGameTime(),
                                Map.of(clock, new ClockNetworkState(ticks, 0.0f, 1.0f))
                        ))
        );
    }
}
