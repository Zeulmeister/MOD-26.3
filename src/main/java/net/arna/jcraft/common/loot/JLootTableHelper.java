package net.arna.jcraft.common.loot;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.common.enchantments.CinderellasKissEnchantment;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.BinomialDistributionGenerator;

import java.util.function.BiConsumer;

public class JLootTableHelper {
    public static final Multimap<Identifier, BiConsumer<LootTable.Builder, HolderLookup.Provider>> modifications =
            MultimapBuilder.hashKeys().linkedHashSetValues().build();

    public static void registerLootTables() {
        registerModification(JLootTableHelper::addMaskPool,
                Identifier.withDefaultNamespace("chests/abandoned_mineshaft"),
                Identifier.withDefaultNamespace("chests/buried_treasure"),
                Identifier.withDefaultNamespace("chests/end_city_treasure"),
                Identifier.withDefaultNamespace("chests/pillager_outpost"),
                Identifier.withDefaultNamespace("chests/simple_dungeon"),
                Identifier.withDefaultNamespace("chests/spawn_bonus_chest"),
                Identifier.withDefaultNamespace("chests/stronghold_library"),
                Identifier.withDefaultNamespace("chests/woodland_mansion")
        );
    }

    public static void registerModification(BiConsumer<LootTable.Builder, HolderLookup.Provider> modifier, Identifier... lootTables) {
        for (Identifier lootTable : lootTables) {
            modifications.put(lootTable, modifier);
        }
    }

    private static void addMaskPool(LootTable.Builder builder, HolderLookup.Provider registries) {
        Holder<Enchantment> kiss = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(CinderellasKissEnchantment.KEY).orElse(null);

        LootPool.Builder maskPool = LootPool.lootPool()
                .when(LootItemRandomChanceCondition.randomChance(0.08f));
        if (kiss != null) {
            maskPool.add(LootItem.lootTableItem(JItemRegistry.CINDERELLA_MASK.get())
                            .setWeight(1)
                            .apply(new SetEnchantmentsFunction.Builder()
                                    .withEnchantment(kiss, net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.BinomialDistributionGenerator(net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(3)), net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue(0.4f)))))))
                    .add(LootItem.lootTableItem(Items.BOOK)
                            .setWeight(2)
                            .apply(new SetEnchantmentsFunction.Builder()
                                    .withEnchantment(kiss, net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1)))));
        } else {
            maskPool.add(LootItem.lootTableItem(JItemRegistry.CINDERELLA_MASK.get()).setWeight(1));
        }
        builder.withPool(maskPool);

        builder.withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(JItemRegistry.STONE_MASK.get()))
                .when(LootItemRandomChanceCondition.randomChance(0.04f)));
    }
}
