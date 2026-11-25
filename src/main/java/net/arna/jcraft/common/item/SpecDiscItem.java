package net.arna.jcraft.common.item;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.spec.SpecData;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.spec.SpecTypeUtil;
import net.arna.jcraft.common.util.JItemData;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SpecDiscItem extends Item {

    private static final String SPEC_ID = "SpecID";

    public SpecDiscItem(Properties settings) {
        super(settings);
    }

    public static void syncDiscModel(ItemStack stack) {
        SpecType type = getSpecType(stack);
        if (type == null || SpecTypeUtil.isNone(type)) {
            stack.set(DataComponents.ITEM_MODEL, JCraft.id("spec_disc"));
        } else {
            stack.set(DataComponents.ITEM_MODEL, JCraft.id("spec_disc_" + type.getId().getPath()));
        }
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public @NonNull InteractionResult use(Level world, Player user, @NonNull InteractionHand hand) {
        final ItemStack itemStack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (JCraft.wasRecentlyAttacked(user.getCombatTracker())) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.combat"));
            return InteractionResult.FAIL;
        }

        final CompoundTag data = JItemData.getOrCreateTag(itemStack);
        SpecType itemSpec = SpecTypeUtil.readFromNBT(data, SPEC_ID);

        final CommonSpecComponent specData = JComponentPlatformUtils.getSpecData(user);
        final SpecType userSpec = specData.getType();

        final boolean itemEmpty = SpecTypeUtil.isNone(itemSpec);
        final boolean userEmpty = SpecTypeUtil.isNone(userSpec);

        if (itemEmpty && userEmpty) {
            return InteractionResult.FAIL;
        }

        if (!itemEmpty && itemSpec == userSpec) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.same_spec"));
            return InteractionResult.FAIL;
        }

        specData.setType(itemEmpty ? net.arna.jcraft.api.registry.JSpecTypeRegistry.NONE.get() : itemSpec);
        JItemData.edit(itemStack, tag -> {
            if (userEmpty) {
                tag.remove(SPEC_ID);
            } else {
                tag.putString(SPEC_ID, userSpec.getId().toString());
            }
        });
        syncDiscModel(itemStack);

        user.getCooldowns().addCooldown(itemStack, 20);
        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext tooltipCtx, @NonNull TooltipDisplay tooltipDisplay, @NonNull Consumer<Component> tooltipAdder, @NonNull TooltipFlag context) {
        SpecType type = getSpecType(stack);
        if (type == null) {
            tooltipAdder.accept(Component.literal("Empty").withStyle(s -> s.applyFormat(ChatFormatting.GRAY)));
            return;
        }
        SpecData data = type.getData();
        tooltipAdder.accept(data.getName().copy().withStyle(s -> s.withColor(ChatFormatting.GRAY)));
    }

    public static ItemStack createDiscStack(SpecType type) {
        ItemStack stack = new ItemStack(JItemRegistry.SPEC_DISC.get());
        JItemData.edit(stack, nbt -> nbt.putString(SPEC_ID, type.getId().toString()));
        syncDiscModel(stack);
        return stack;
    }

    public static boolean isEmptyDisc(ItemStack stack) {
        CompoundTag tag = JItemData.getTag(stack);
        return tag == null || !tag.contains(SPEC_ID);
    }

    public static SpecType getSpecType(ItemStack stack) {
        if (!stack.is(JItemRegistry.SPEC_DISC.get())) {
            return null;
        }
        CompoundTag nbt = JItemData.getTag(stack);
        if (nbt == null || !nbt.contains(SPEC_ID)) {
            return null;
        }
        return SpecTypeUtil.readFromNBT(nbt, SPEC_ID);
    }
}
