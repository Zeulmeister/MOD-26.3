package net.arna.jcraft.api.registry;

import net.arna.jcraft.JCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;

public interface JConfiguredFeatureRegistry {
    ResourceKey<Feature> SAND_DISK = ResourceKey.create(Registries.FEATURE, JCraft.id("sand_disk_cf"));
}
