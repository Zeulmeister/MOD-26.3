package net.arna.jcraft.fabric;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.argumenttype.AttackArgumentType;
import net.arna.jcraft.common.argumenttype.SpecArgumentType;
import net.arna.jcraft.common.argumenttype.StandArgumentType;
import net.arna.jcraft.common.events.JServerEvents;
import net.arna.jcraft.fabric.common.terrablender.JTerraFabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.BiConsumer;

import static net.arna.jcraft.common.loot.JLootTableHelper.modifications;

public final class JCraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        JCraft.init();
        JTerraFabric.onModInitialized();

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            for (BiConsumer<LootTable.Builder, net.minecraft.core.HolderLookup.Provider> modification : modifications.get(key.identifier())) {
                modification.accept(tableBuilder, registries);
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register(JServerEvents::stopSleeping);
        EntitySleepEvents.ALLOW_BED.register(JServerEvents::allowBed);
        EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.register(JServerEvents::modifySleepingDirection);
        // Daytime coffin sleep handled via CoffinBlock.getBedRule in 26.3

        ArgumentTypeRegistry.registerArgumentType(JCraft.id("stand"), StandArgumentType.class, SingletonArgumentInfo.contextFree(StandArgumentType::stand));
        ArgumentTypeRegistry.registerArgumentType(JCraft.id("spec"), SpecArgumentType.class, SingletonArgumentInfo.contextFree(SpecArgumentType::spec));
        ArgumentTypeRegistry.registerArgumentType(JCraft.id("attack"), AttackArgumentType.class, SingletonArgumentInfo.contextFree(AttackArgumentType::attack));

        JCraft.postInit();
    }
}
