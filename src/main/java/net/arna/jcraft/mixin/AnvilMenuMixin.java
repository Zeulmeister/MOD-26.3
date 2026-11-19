package net.arna.jcraft.mixin;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.enchantments.CinderellasKissEnchantment;
import net.arna.jcraft.common.item.StandDiscItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private @Nullable String itemName;

    @Shadow
    private int repairItemCountCost;

    private AnvilMenuMixin(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDefinition) {
        super(menuType, containerId, inventory, access, slotDefinition);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void jcraft$injectAnvil(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        if (left.is(JItemRegistry.CINDERELLA_MASK.get()) && right.is(Items.ENCHANTED_BOOK)) {
            if (jcraft$enchantMask(left, right)) {
                ci.cancel();
            }
        } else if (left.is(JItemRegistry.STAND_DISC.get()) && right.is(JItemRegistry.CINDERELLA_MASK.get())) {
            if (jcraft$switchSkin(left, right)) {
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean jcraft$enchantMask(ItemStack mask, ItemStack book) {
        int bookLevel = CinderellasKissEnchantment.getCKLevel(book);
        Holder<Enchantment> kissHolder = CinderellasKissEnchantment.getCKHolder(book);
        if (bookLevel <= 0 || kissHolder == null) {
            return false;
        }

        int maskLevel = CinderellasKissEnchantment.getCKLevel(mask);
        int newLevel = (maskLevel == bookLevel)
                ? Math.min(CinderellasKissEnchantment.MAX_LEVEL, maskLevel + 1)
                : Math.max(maskLevel, bookLevel);
        if (newLevel <= maskLevel) {
            return false;
        }

        int xpCost = maskLevel + bookLevel;
        ItemStack result = mask.copyWithCount(1);
        EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(kissHolder, newLevel));

        xpCost = jcraft$applyRename(mask, result, xpCost);

        int leftRepair = mask.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightRepair = book.getOrDefault(DataComponents.REPAIR_COST, 0);
        xpCost += leftRepair + rightRepair;
        result.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(Math.max(leftRepair, rightRepair)));

        this.repairItemCountCost = 1;
        this.resultSlots.setItem(0, result);
        this.cost.set(Math.max(1, xpCost));
        this.broadcastChanges();
        return true;
    }

    @Unique
    private boolean jcraft$switchSkin(ItemStack disc, ItemStack mask) {
        StandType standType = StandDiscItem.getStandType(disc);
        if (StandTypeUtil.isNone(standType)) {
            return false;
        }

        int targetSkin = CinderellasKissEnchantment.getCKLevel(mask);
        int skinCount = standType.getData().getInfo().getSkinCount();
        if (targetSkin >= skinCount || targetSkin == StandDiscItem.getSkin(disc)) {
            return false;
        }

        ItemStack result = StandDiscItem.createDiscStack(standType, targetSkin, StandDiscItem.isFragile(disc));
        Component existingCustomName = disc.get(DataComponents.CUSTOM_NAME);
        if (existingCustomName != null) {
            result.set(DataComponents.CUSTOM_NAME, existingCustomName);
        }

        int xpCost = jcraft$applyRename(disc, result, 5);

        this.repairItemCountCost = 1;
        this.resultSlots.setItem(0, result);
        this.cost.set(xpCost);
        this.broadcastChanges();
        return true;
    }

    @Unique
    private int jcraft$applyRename(ItemStack source, ItemStack result, int xpCost) {
        if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
            if (!this.itemName.equals(source.getHoverName().getString())) {
                xpCost++;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
            }
        } else if (source.has(DataComponents.CUSTOM_NAME)) {
            xpCost++;
            result.remove(DataComponents.CUSTOM_NAME);
        }
        return xpCost;
    }
}
