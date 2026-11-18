package net.arna.jcraft.common.item;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
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

import java.util.function.Consumer;

public class StoneMaskItem extends Item {

    protected static final AzCommand DORMANT_CMD = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.stone_mask.dormant", AzPlayBehaviors.LOOP);
    protected static final AzCommand CLENCH_CMD = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.stone_mask.clench", AzPlayBehaviors.PLAY_ONCE);

    protected static final int CLENCH_DURATION = 100;
    protected static final Int2IntMap CLENCH = new Int2IntOpenHashMap();

    static {
        if (Platform.getEnv() == EnvType.CLIENT) {
            ClientTickEvent.CLIENT_POST.register(client -> {
                IntSet toRemove = new IntOpenHashSet();
                for (int i : CLENCH.keySet()) {
                    int newTime = CLENCH.get(i) - 1;
                    if (newTime <= 0) {
                        toRemove.add(i);
                        continue;
                    }
                    CLENCH.put(i, newTime);
                }
                toRemove.forEach(CLENCH::remove);
            });
        }
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
        if (entity instanceof ServerPlayer player && JCraft.wasRecentlyAttacked(player.getCombatTracker())) {
            CommonSpecComponent specComponent = JComponentPlatformUtils.getSpecData(player);
            if (specComponent.getType() != JSpecTypeRegistry.VAMPIRE.get()) {
                specComponent.setType(JSpecTypeRegistry.VAMPIRE.get());
                level.playSound(null, entity.blockPosition(), JSoundRegistry.VAMPIRE_SPEC_CHANGE.get(), SoundSource.PLAYERS, 1f, 1f);
                CLENCH.put(entity.getId(), CLENCH_DURATION);
                CLENCH_CMD.sendForItem(entity, stack);
            }
        }
        if (!CLENCH.containsKey(entity.getId())) {
            DORMANT_CMD.sendForItem(entity, stack);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("jcraft.stonemask.desc"));
    }
}
