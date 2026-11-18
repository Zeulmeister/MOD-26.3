package net.arna.jcraft.common.villagertrades;

import net.arna.jcraft.JCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.VillagerTrade;

/**
 * In Minecraft 26.3, Villager trades are natively data-driven via {@code data/jcraft/villager_trade/...}
 * and {@code data/minecraft/tags/villager_trade/cartographer/level_*.json}.
 */
public final class VillagerTradesModifier {
    public static final ResourceKey<VillagerTrade> METEORITE_MAP =
            ResourceKey.create(Registries.VILLAGER_TRADE, JCraft.id("cartographer/3/meteorite_map"));
    public static final ResourceKey<VillagerTrade> CINDERELLA_MAP =
            ResourceKey.create(Registries.VILLAGER_TRADE, JCraft.id("cartographer/4/cinderella_map"));
    public static final ResourceKey<VillagerTrade> MONASTERY_MAP =
            ResourceKey.create(Registries.VILLAGER_TRADE, JCraft.id("cartographer/5/monastery_map"));
    public static final ResourceKey<VillagerTrade> VAMPIRE_LAIR_MAP =
            ResourceKey.create(Registries.VILLAGER_TRADE, JCraft.id("cartographer/5/vampire_lair_map"));

    private VillagerTradesModifier() {
    }

    public static void init() {
        // Data-driven trades loaded automatically from data/jcraft/villager_trade/ and data/minecraft/tags/villager_trade/
    }
}
