package net.arna.jcraft.fabric.common.terrablender;

import net.arna.jcraft.api.registry.JBiomeRegistry;
import net.arna.jcraft.api.registry.JBlockRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import java.util.List;

public class MaterialRulesFabric {
    public static MaterialRule makeRules() {
        return new SequenceRule(List.of());
    }

    public static MaterialRule makeRules(final RegistryAccess registryAccess) {
        final MaterialCondition isAtOrAboveWaterLevel = MaterialRules.waterBlockCheck(-1, 0);
        final MaterialCondition isDevilsPalm = MaterialRules.isBiome(
                registryAccess.lookupOrThrow(Registries.BIOME),
                JBiomeRegistry.DEVILS_PALM
        );

        final MaterialRule hotSand = MaterialRules.state(JBlockRegistry.HOT_SAND_BLOCK.get().defaultBlockState());
        final MaterialRule sandstone = MaterialRules.state(Blocks.SANDSTONE.defaultBlockState());

        return MaterialRules.ifTrue(
                isDevilsPalm,
                MaterialRules.ifTrue(
                        MaterialRules.abovePreliminarySurface(),
                        MaterialRules.sequence(
                                MaterialRules.ifTrue(
                                        MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR),
                                        MaterialRules.ifTrue(isAtOrAboveWaterLevel, hotSand)
                                ),
                                MaterialRules.ifTrue(
                                        MaterialRules.stoneDepthCheck(0, true, CaveSurface.FLOOR),
                                        sandstone
                                )
                        )
                )
        );
    }
}
