package net.arna.jcraft.common.enchantments;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.arna.jcraft.JCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

public final class CinderellasKissEnchantment {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(
            Registries.ENCHANTMENT, net.minecraft.resources.Identifier.fromNamespaceAndPath("jcraft", "cinderellas_kiss"));
    public static final int MAX_LEVEL = 3;

    private CinderellasKissEnchantment() {}

    /**
     * Returns the level of Cinderella's Kiss on {@code stack} (checking both active and stored enchantments),
     * or {@code 0} if absent.
     */
    public static int getCKLevel(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int lvl = findLevel(stack.get(DataComponents.ENCHANTMENTS));
        if (lvl > 0) {
            return lvl;
        }
        return findLevel(stack.get(DataComponents.STORED_ENCHANTMENTS));
    }

    /**
     * Returns the {@link Holder} for Cinderella's Kiss if present on {@code stack}'s active or stored enchantments.
     */
    public static @Nullable Holder<Enchantment> getCKHolder(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Holder<Enchantment> holder = findHolder(stack.get(DataComponents.ENCHANTMENTS));
        if (holder != null) {
            return holder;
        }
        return findHolder(stack.get(DataComponents.STORED_ENCHANTMENTS));
    }

    private static int findLevel(@Nullable ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(KEY)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static @Nullable Holder<Enchantment> findHolder(@Nullable ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return null;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(KEY)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
