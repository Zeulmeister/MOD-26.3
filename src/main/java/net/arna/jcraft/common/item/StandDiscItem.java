package net.arna.jcraft.common.item;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.util.JItemData;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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

public class StandDiscItem extends Item {
    private static final TextColor[] SKIN_LEVEL_COLORS = {
            TextColor.fromLegacyFormat(ChatFormatting.GRAY),
            TextColor.fromLegacyFormat(ChatFormatting.RED),
            TextColor.fromLegacyFormat(ChatFormatting.BLUE),
            TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE),
            TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY),
            TextColor.fromLegacyFormat(ChatFormatting.GOLD)
    };
    private static final Component DEFAULT_SKIN = Component.literal("Default");

    public StandDiscItem(Properties settings) {
        super(settings);
    }

    public static void syncDiscModel(ItemStack stack) {
        StandType type = getStandType(stack);
        if (type == null || StandTypeUtil.isNone(type)) {
            stack.set(DataComponents.ITEM_MODEL, JCraft.id("stand_disc"));
        } else {
            int maxSkin = Math.max(3, type.getData().getInfo().getSkinCount() - 1);
            int skin = Math.max(0, Math.min(maxSkin, getSkin(stack)));
            stack.set(DataComponents.ITEM_MODEL, JCraft.id("stand_disc_" + type.getId().getPath() + "_" + skin));
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
        final boolean oneUse = data.getByteOr("OneUse", (byte) 0) != 0;
        StandType itemStand = StandTypeUtil.readFromNBT(data, "StandID");
        int itemSkin = data.getIntOr("Skin", 0);

        if (itemStand != null && JCraft.getExclusiveStandsData().isStandUsed(itemStand)) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.stand_in_use"));
            return InteractionResult.FAIL;
        }

        final CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(user);
        final StandType userStand = standData.getType();
        final int userSkin = standData.getSkin();

        final boolean itemEmpty = StandTypeUtil.isNone(itemStand);
        final boolean userEmpty = StandTypeUtil.isNone(userStand);

        if (oneUse && !userEmpty) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.fragile_has_stand").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (itemEmpty && userEmpty) {
            return InteractionResult.FAIL;
        }

        if (!itemEmpty && itemStand == userStand && itemSkin == userSkin) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.same_stand"));
            return InteractionResult.FAIL;
        }

        if (!itemEmpty && StandTypeUtil.isIn(itemStand, JTagRegistry.PLAYER_STAND_BLACKLIST, world.registryAccess())) {
            user.sendOverlayMessage(Component.translatable("jcraft.disc.blacklisted"));
            return InteractionResult.FAIL;
        }

        standData.setTypeAndSkin(itemEmpty ? net.arna.jcraft.api.registry.JStandTypeRegistry.NONE.get() : itemStand, itemEmpty ? 0 : itemSkin, false);
        JItemData.edit(itemStack, tag -> {
            if (userEmpty) {
                tag.remove("StandID");
                tag.remove("Skin");
            } else {
                tag.putString("StandID", userStand.getId().toString());
                tag.putInt("Skin", userSkin);
            }
        });
        syncDiscModel(itemStack);

        final StandEntity<?, ?> stand = standData.getStand();
        if (stand != null) {
            stand.desummon();
            if (!stand.isRemoved()) {
                stand.discard();
            }
            standData.setStand(null);
        }

        if (!itemEmpty) {
            JCraft.summon(world, user);
        }
        user.getCooldowns().addCooldown(itemStack, 20);

        if (oneUse) {
            itemStack.shrink(1);
        }

        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }

    @Override
    @SuppressWarnings("deprecation") // Official MC 26.3 Item tooltip hook (invoked by ItemStack#addDetailsToTooltip)
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext tooltipCtx, @NonNull TooltipDisplay tooltipDisplay, @NonNull Consumer<Component> tooltipAdder, @NonNull TooltipFlag context) {
        StandType type = getStandType(stack);
        if (type == null) {
            tooltipAdder.accept(Component.literal("Empty").withStyle(s -> s.applyFormat(ChatFormatting.GRAY)));
            return;
        }

        StandData data = type.getData();
        tooltipAdder.accept(data.getInfo().getName().copy().withStyle(s -> s.withColor(data.isEvolution() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY)));

        int skin = getSkin(stack);
        tooltipAdder.accept((skin == 0 || skin >= data.getInfo().getSkinCount() ? DEFAULT_SKIN : data.getInfo().getSkinNames().get(skin - 1)).copy()
                .withStyle(s -> s.withColor(SKIN_LEVEL_COLORS[Math.max(0, Math.min(skin, SKIN_LEVEL_COLORS.length - 1))])));
    }

    public static ItemStack createDiscStack(StandType type, int skin, boolean oneUse) {
        if (skin < 0 || skin >= type.getData().getInfo().getSkinCount()) {
            throw new IndexOutOfBoundsException("Skin out of bounds");
        }

        ItemStack stack = new ItemStack(JItemRegistry.STAND_DISC.get());
        JItemData.edit(stack, nbt -> {
            nbt.putString("StandID", type.getId().toString());
            nbt.putInt("Skin", skin);
            if (oneUse) {
                nbt.putByte("OneUse", (byte) 1);
            }
        });
        syncDiscModel(stack);
        return stack;
    }

    public static ItemStack createDiscStack(StandType type, int skin) {
        return createDiscStack(type, skin, false);
    }

    public static boolean isEmptyDisc(ItemStack stack) {
        CompoundTag tag = JItemData.getTag(stack);
        return tag == null || !tag.contains("StandID");
    }

    public static boolean isFragile(ItemStack stack) {
        CompoundTag tag = JItemData.getTag(stack);
        return tag != null && tag.getByteOr("OneUse", (byte) 0) != 0;
    }

    public static StandType getStandType(ItemStack stack) {
        if (!stack.is(JItemRegistry.STAND_DISC.get())) {
            return null;
        }
        CompoundTag nbt = JItemData.getTag(stack);
        if (nbt == null || !nbt.contains("StandID")) {
            return null;
        }
        return StandTypeUtil.readFromNBT(nbt, "StandID");
    }

    public static void setSkin(ItemStack stack, int skin) {
        if (!stack.is(JItemRegistry.STAND_DISC.get())) {
            return;
        }
        JItemData.edit(stack, nbt -> nbt.putInt("Skin", skin));
        syncDiscModel(stack);
    }

    public static int getSkin(ItemStack stack) {
        if (!stack.is(JItemRegistry.STAND_DISC.get())) {
            return 0;
        }
        CompoundTag nbt = JItemData.getTag(stack);
        return nbt == null ? 0 : nbt.getIntOr("Skin", 0);
    }
}
