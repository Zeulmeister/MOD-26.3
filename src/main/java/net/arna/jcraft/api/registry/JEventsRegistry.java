package net.arna.jcraft.api.registry;

import dev.architectury.event.events.common.*;
import net.arna.jcraft.api.serverconfig.ConfigOption;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.events.JBlockEvents;
import net.arna.jcraft.common.events.JEntityEvents;
import net.arna.jcraft.common.events.JServerEvents;
import net.arna.jcraft.common.network.c2s.ConfigUpdatePacket;
import net.arna.jcraft.common.network.s2c.AttackerDataPacket;
import net.arna.jcraft.common.tickable.PastDimensions;
import net.arna.jcraft.common.worldgen.VillageStructureInjector;
import net.minecraft.world.level.Level;

import java.util.List;

public interface JEventsRegistry {
    static void registerEvents() {
        // NOTE: THESE ARE ALSO REGISTERED ON THE CLIENT
        EntityEvent.LIVING_HURT.register(JServerEvents::hurt);
        JEntityEvents.POST_ADD.register(JServerEvents::entityLoad);
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd, removalReason) -> JServerEvents.playerRespawn(newPlayer, conqueredEnd));
        EntityEvent.LIVING_DEATH.register(JServerEvents::death);
        TickEvent.SERVER_POST.register(JServerEvents::serverPostTick);
        TickEvent.SERVER_LEVEL_POST.register(JServerEvents::serverLevelPostTick);
        JBlockEvents.BEFORE_SET.register(JServerEvents::beforeBlockSet);
        JBlockEvents.BEFORE_BLOCK_LOOT.register(JServerEvents::processBlockLoot);

        // Disable item/block usage while stunned
        InteractionEvent.RIGHT_CLICK_ITEM.register(JServerEvents::rightClick);

        InteractionEvent.LEFT_CLICK_BLOCK.register(JServerEvents::leftClickBlock);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(JServerEvents::rightClickBlock);

        // Send initial values of server config options to the player.
        PlayerEvent.PLAYER_JOIN.register((player) -> {
            ConfigUpdatePacket.sendOptionsToClient(player, ConfigOption.getOptions().values());
            AttackerDataPacket.send(List.of(player));
            JServerEvents.playerJoin(player);
        });
        PlayerEvent.PLAYER_QUIT.register((player) -> {
            net.arna.jcraft.JCraft.stopTimestop(player);
            net.arna.jcraft.common.system.GunAiming.set(player, false);
            net.arna.jcraft.common.network.c2s.PoseTauntPacket.clear(player);
            net.arna.jcraft.common.item.StoneMaskItem.clearForEntity(player.getId());
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
            net.arna.jcraft.JCraft.stopTimestop(player);
        });

        LifecycleEvent.SERVER_BEFORE_START.register(JServerConfig::load);
        LifecycleEvent.SERVER_BEFORE_START.register(VillageStructureInjector::onServerBeforeStart);
        LifecycleEvent.SERVER_STARTED.register(JServerEvents::finishLoading);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(level -> {
            if (level.dimension().equals(Level.OVERWORLD)) {
                JServerEvents.savePastDimensions(level.getServer());
            }
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            JServerEvents.saveExclusives(server);
            JServerEvents.saveServerJoins(server);
            JServerEvents.savePastDimensions(server);
            PastDimensions.clearAll();
            net.arna.jcraft.common.tickable.Timestops.clearAll();
            net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.clearAll();
            net.arna.jcraft.common.tickable.PeacemakerReload.clearAll();
            net.arna.jcraft.common.system.GunAiming.clearAll();
            net.arna.jcraft.common.network.c2s.PoseTauntPacket.clearAll();
            net.arna.jcraft.common.item.StoneMaskItem.clearAll();
        });
    }
}
