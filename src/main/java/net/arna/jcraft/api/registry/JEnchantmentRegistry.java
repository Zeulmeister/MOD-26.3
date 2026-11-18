package net.arna.jcraft.api.registry;

import net.arna.jcraft.common.enchantments.CinderellasKissEnchantment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public interface JEnchantmentRegistry {
    ResourceKey<Enchantment> CINDERELLAS_KISS = CinderellasKissEnchantment.KEY;

    static void init() {}
}
