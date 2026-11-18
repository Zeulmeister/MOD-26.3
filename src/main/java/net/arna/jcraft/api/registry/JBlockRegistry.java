package net.arna.jcraft.api.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.block.CoffinBlock;
import net.arna.jcraft.common.block.FoolishSandBlock;
import net.arna.jcraft.common.block.HotSandBlock;
import net.arna.jcraft.common.block.SoulBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

public interface JBlockRegistry {

    DeferredRegister<Block> BLOCK_REGISTRY = DeferredRegister.create(JCraft.MOD_ID, Registries.BLOCK);

    RegistrySupplier<Block> FOOLISH_SAND_BLOCK = register("foolish_sand_block", BlockBehaviour.Properties.of()
            .strength(0.5f).sound(SoundType.SAND), FoolishSandBlock::new);

    RegistrySupplier<Block> SOUL_BLOCK = register("soul_block", BlockBehaviour.Properties.of()
            .strength(4.0f).sound(SoundType.SOUL_SOIL), SoulBlock::new);

    RegistrySupplier<Block> METEORITE_BLOCK = register("meteorite_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .requiresCorrectToolForDrops().strength(6.0f, 1200f).sound(SoundType.ANCIENT_DEBRIS).mapColor(MapColor.COLOR_BLACK), Block::new);

    RegistrySupplier<Block> POLISHED_METEORITE_BLOCK = register("polished_meteorite_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .requiresCorrectToolForDrops().strength(6.0f, 1200f).sound(SoundType.ANCIENT_DEBRIS).mapColor(MapColor.COLOR_BLACK), Block::new);

    RegistrySupplier<Block> METEORITE_IRON_ORE_BLOCK = register("meteorite_iron_ore_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
            .requiresCorrectToolForDrops().strength(9.0f, 1200f).sound(SoundType.ANCIENT_DEBRIS).mapColor(MapColor.COLOR_BLACK), Block::new);

    RegistrySupplier<Block> STELLAR_IRON_BLOCK = register("stellar_iron_block", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .requiresCorrectToolForDrops().strength(7.5f, 1000f).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_YELLOW), Block::new);

    RegistrySupplier<Block> HOT_SAND_BLOCK = register("hot_sand_block", BlockBehaviour.Properties.of()
            .strength(0.5f).sound(SoundType.SAND).mapColor(MapColor.COLOR_ORANGE), HotSandBlock::new);

    RegistrySupplier<Block> CINDERELLA_GREEN_BLOCK = register("cinderella_green_block", BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA)
            .mapColor(MapColor.TERRACOTTA_GREEN), Block::new);

    RegistrySupplier<Block> SOUL_WOOD_BLOCK = register("soul_wood_block", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD), RotatedPillarBlock::new);

    RegistrySupplier<Block> COFFIN_BLOCK = register("coffin", BlockBehaviour.Properties.of()
            .sound(SoundType.WOOD).noOcclusion(), CoffinBlock::new);

    private static <T extends Block> RegistrySupplier<T> register(String id, BlockBehaviour.Properties props, Function<BlockBehaviour.Properties, T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JCraft.id(id));
        return BLOCK_REGISTRY.register(id, () -> factory.apply(props.setId(key)));
    }

    static void init() {}
}
