package net.arna.jcraft.common.enchantments;

import net.arna.jcraft.JCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class CinderellasKissEnchantment {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, JCraft.id("cinderellas_kiss"));
    private CinderellasKissEnchantment() {}
}
