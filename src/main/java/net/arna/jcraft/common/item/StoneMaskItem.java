package net.arna.jcraft.common.item;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class StoneMaskItem extends Item {

    protected static final AzCommand DORMANT_CMD = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.stone_mask.dormant", AzPlayBehaviors.LOOP);
    protected static final AzCommand CLENCH_CMD = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.stone_mask.clench", AzPlayBehaviors.PLAY_ONCE);

    protected static final int CLENCH_DURATION = 100;
    private static final Map<Integer, Long> CLENCH_UNTIL = new ConcurrentHashMap<>();

    public static void clearAll() {
        CLENCH_UNTIL.clear();
    }

    public static void clearForEntity(int entityId) {
        CLENCH_UNTIL.remove(entityId);
    }

    public StoneMaskItem(ArmorMaterial materialIn, ArmorType slot, Properties builder) {
        super(builder.humanoidArmor(materialIn, slot));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (slot != EquipmentSlot.HEAD) {
            return;
        }
        long now = level.getGameTime();
        if (entity instanceof ServerPlayer player && JCraft.wasRecentlyAttacked(player.getCombatTracker())) {
            CommonSpecComponent specComponent = JComponentPlatformUtils.getSpecData(player);
            if (specComponent.getType() != JSpecTypeRegistry.VAMPIRE.get()) {
                specComponent.setType(JSpecTypeRegistry.VAMPIRE.get());
                level.playSound(null, entity.blockPosition(), JSoundRegistry.VAMPIRE_SPEC_CHANGE.get(), SoundSource.PLAYERS, 1f, 1f);
                CLENCH_UNTIL.put(entity.getId(), now + CLENCH_DURATION);
                CLENCH_CMD.sendForItem(entity, stack);
            }
        }
        Long expiry = CLENCH_UNTIL.get(entity.getId());
        if (expiry == null || now >= expiry) {
            if (expiry != null) {
                CLENCH_UNTIL.remove(entity.getId());
            }
            DORMANT_CMD.sendForItem(entity, stack);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("jcraft.stonemask.desc"));
    }
}
