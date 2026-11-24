package net.arna.jcraft.common.item;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.common.entity.projectile.BulletProjectile;
import net.arna.jcraft.common.system.GunAiming;
import net.arna.jcraft.common.tickable.PeacemakerReload;
import net.arna.jcraft.common.util.JItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Peacemaker extends Item {

    public static final String SHOTS_ID = "Shots";
    public static final String RELOADING_ID = "Reloading";
    public static final String ANIMATION_ID = "Animation";
    public static final String ANIMATION_SEQUENCE_ID = "AnimationSequence";
    public static final String COCKED_ID = "Cocked";

    public static final int MAX_ROUNDS = 6;
    private static final int FIRE_TICKS = 8;
    private static final int COCK_INPUT_LOCKOUT = 2;

    public static final AzCommand FIRE = Attacks.createAnimationCommand(JCraft.FIRE_CONTROLLER, "fire", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand COCK = Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "cock", AzPlayBehaviors.PLAY_ONCE);

    public Peacemaker(Properties settings) {
        super(settings);
    }

    public static void markAnimation(ItemStack stack, String animation) {
        if (animation == null || animation.isEmpty()) {
            return;
        }
        JItemData.edit(stack, data -> {
            data.putString(ANIMATION_ID, animation);
            data.putLong(ANIMATION_SEQUENCE_ID, data.getLongOr(ANIMATION_SEQUENCE_ID, 0L) + 1);
        });
    }

    public static int getShots(ItemStack stack) {
        CompoundTag data = JItemData.getTag(stack);
        if (data == null || !data.contains(SHOTS_ID)) {
            return MAX_ROUNDS;
        }
        return data.getIntOr(SHOTS_ID, 0);
    }

    private static volatile Supplier<String> fireKeySupplier = () -> "LMB";
    private static volatile Supplier<String> aimKeySupplier = () -> "RMB";
    private static volatile Supplier<String> reloadKeySupplier = () -> "MMB";

    public static void setClientKeySuppliers(Supplier<String> fire, Supplier<String> aim, Supplier<String> reload) {
        fireKeySupplier = fire != null ? fire : () -> "LMB";
        aimKeySupplier = aim != null ? aim : () -> "RMB";
        reloadKeySupplier = reload != null ? reload : () -> "MMB";
    }

    public static String getFireKey() {
        return fireKeySupplier.get();
    }

    public static String getAimKey() {
        return aimKeySupplier.get();
    }

    public static String getReloadKey() {
        return reloadKeySupplier.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipCtx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag context) {
        int shots = getShots(stack);

        tooltipAdder.accept(Component.translatable("tooltip.jcraft.peacemaker.shots").append(" §e" + shots));
        tooltipAdder.accept(Component.translatable("tooltip.jcraft.peacemaker.fire", getFireKey()));
        tooltipAdder.accept(Component.translatable("tooltip.jcraft.peacemaker.aim", getAimKey()));
        tooltipAdder.accept(Component.translatable("tooltip.jcraft.peacemaker.reload", getReloadKey()));
    }

    @Override
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level world, BlockPos pos, LivingEntity user) {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0.0f;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return InteractionResult.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        final CompoundTag data = JItemData.getTag(stack);
        if (data != null && data.getBooleanOr(RELOADING_ID, false) && !PeacemakerReload.isReloading(stack)) {
            abortReload(stack, entity instanceof LivingEntity living ? living : null);
        }
    }

    private static void startReload(ItemStack itemStack, Level world, Player user, InteractionHand hand) {
        JItemData.edit(itemStack, data -> {
            data.putBoolean(RELOADING_ID, true);
            data.putBoolean(COCKED_ID, false);
        });
        final int cooldownTicks = PeacemakerReload.totalTicks(plannedRounds(itemStack, user));
        user.getCooldowns().addCooldown(itemStack, cooldownTicks);
        user.getCooldowns().addCooldown(new ItemStack(JItemRegistry.FV_REVOLVER.get()), cooldownTicks);
        PeacemakerReload.start(user, world, hand, itemStack);
    }

    private static int plannedRounds(ItemStack itemStack, Player user) {
        final int empty = MAX_ROUNDS - getShots(itemStack);
        return user.isCreative() ? empty : Math.min(empty, countBulletsInInventory(user));
    }

    public static boolean canLoadMore(ItemStack itemStack, LivingEntity user) {
        if (getShots(itemStack) >= MAX_ROUNDS) {
            return false;
        }
        if (!(user instanceof Player player)) {
            return false;
        }
        return player.isCreative() || hasBulletInInventory(player);
    }

    public static void loadRound(ItemStack itemStack, LivingEntity user) {
        if (!(user instanceof Player player)) {
            return;
        }
        final int shots = getShots(itemStack);
        if (shots >= MAX_ROUNDS) {
            return;
        }
        if (!player.isCreative() && !consumeBulletFromInventory(player)) {
            return;
        }
        JItemData.edit(itemStack, data -> data.putInt(SHOTS_ID, shots + 1));
    }

    public static void finishReload(ItemStack itemStack, LivingEntity user) {
        clearReloadState(itemStack, user);
    }

    public static void abortReload(ItemStack itemStack, LivingEntity user) {
        clearReloadState(itemStack, user);
    }

    private static void clearReloadState(ItemStack itemStack, LivingEntity user) {
        JItemData.edit(itemStack, data -> data.putBoolean(RELOADING_ID, false));
    }

    public static boolean handleLeftClick(Player player) {
        return handleLeftClick(player, null);
    }

    public static boolean handleLeftClick(Player player, @Nullable Vec3 muzzle) {
        final ItemStack peacemakerStack = player.getMainHandItem();
        if (!(peacemakerStack.getItem() instanceof Peacemaker)) {
            return false;
        }

        Level world = player.level();
        if (player.getCooldowns().isOnCooldown(peacemakerStack)) {
            return true;
        }
        if (player.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            return true;
        }

        CompoundTag data = JItemData.getOrCreateTag(peacemakerStack);
        if (data.getBooleanOr(RELOADING_ID, false)) {
            if (!world.isClientSide()) {
                PeacemakerReload.finishEarly(peacemakerStack, world);
                final int cooldownTicks = PeacemakerReload.endTicks();
                player.getCooldowns().addCooldown(peacemakerStack, cooldownTicks);
                player.getCooldowns().addCooldown(new ItemStack(JItemRegistry.FV_REVOLVER.get()), cooldownTicks);
            }
            return true;
        }

        if (getShots(peacemakerStack) < 1 && !player.isCreative()) {
            return true;
        }

        if (!world.isClientSide()) {
            if (data.getBooleanOr(COCKED_ID, false)) {
                JItemData.edit(peacemakerStack, d -> d.putBoolean(COCKED_ID, false));
                player.getCooldowns().addCooldown(peacemakerStack, FIRE_TICKS);
                player.getCooldowns().addCooldown(new ItemStack(JItemRegistry.FV_REVOLVER.get()), FIRE_TICKS);

                FIRE.sendForItem(player, peacemakerStack);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), JSoundRegistry.PEACEMAKER_FIRE.get(), SoundSource.PLAYERS, 1f, 1f);
                shoot(peacemakerStack, world, player, muzzle);
            } else {
                JItemData.edit(peacemakerStack, d -> d.putBoolean(COCKED_ID, true));
                player.getCooldowns().addCooldown(peacemakerStack, COCK_INPUT_LOCKOUT);
                player.getCooldowns().addCooldown(new ItemStack(JItemRegistry.FV_REVOLVER.get()), COCK_INPUT_LOCKOUT);

                COCK.sendForItem(player, peacemakerStack);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), JSoundRegistry.GUN_COCK.get(), SoundSource.PLAYERS, 1f, 1f);
            }
        }
        return true;
    }

    public static boolean handleReloadInput(Player player) {
        final ItemStack peacemakerStack = player.getMainHandItem();
        if (!(peacemakerStack.getItem() instanceof Peacemaker)) {
            return false;
        }
        final Level world = player.level();
        if (player.hasEffect(JStatusRegistry.DAZED.asHolder()) || player.isSpectator()) {
            return true;
        }
        final CompoundTag data = JItemData.getOrCreateTag(peacemakerStack);
        if (getShots(peacemakerStack) >= MAX_ROUNDS || data.getBooleanOr(RELOADING_ID, false) || !canLoadMore(peacemakerStack, player)) {
            return true;
        }
        if (!world.isClientSide()) {
            startReload(peacemakerStack, world, player, InteractionHand.MAIN_HAND);
        }
        return true;
    }

    public static boolean isReloading(ItemStack itemStack) {
        final CompoundTag data = JItemData.getTag(itemStack);
        return data != null && data.getBooleanOr(RELOADING_ID, false);
    }

    public static void shoot(ItemStack itemStack, Level world, LivingEntity user) {
        shoot(itemStack, world, user, null);
    }

    public static void shoot(ItemStack itemStack, Level world, LivingEntity user, @Nullable Vec3 muzzle) {
        int shots = getShots(itemStack);

        if (!(user instanceof Player player && player.isCreative())) {
            if (shots < 1) {
                return;
            }
            JItemData.edit(itemStack, d -> d.putInt(SHOTS_ID, shots - 1));
        }

        final boolean aiming = GunAiming.isAiming(user);
        float inaccuracy = 2.5f;
        if (aiming) inaccuracy *= GunAiming.SPREAD_MULTIPLIER;

        BulletProjectile bullet = new BulletProjectile(world, user, 9f, 10f, 2, 7f);
        bullet.shootFromRotation(user, user.getXRot(), user.getYRot(), 0f, 10, inaccuracy);

        final Vec3 forward = Vec3.directionFromRotation(0f, user.getYRot());
        final boolean offhand = user.getOffhandItem() == itemStack;
        final boolean rightArm = user.getMainArm() == HumanoidArm.RIGHT;
        double side = rightArm != offhand ? 0.35 : -0.35;
        if (aiming) side = 0.0;

        final Vec3 eye = user.getEyePosition();
        Vec3 spawn;
        if (muzzle != null && muzzle.distanceToSqr(eye) < 6.25) {
            spawn = muzzle;
        } else {
            spawn = eye.add(forward.scale(0.25))
                    .add(forward.cross(new Vec3(0, 1, 0)).scale(side))
                    .add(0, -0.35, 0);
        }
        bullet.setPos(spawn);
        bullet.xo = spawn.x;
        bullet.yo = spawn.y;
        bullet.zo = spawn.z;

        world.addFreshEntity(bullet);

        if (user instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(JItemRegistry.PEACEMAKER.get()));
        }
    }

    private static boolean hasBulletInInventory(Player player) {
        return countBulletsInInventory(player) > 0;
    }

    private static int countBulletsInInventory(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            final ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == JItemRegistry.BULLET.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean consumeBulletFromInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == JItemRegistry.BULLET.get()) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        JItemData.edit(stack, nbt -> {
            nbt.putInt(SHOTS_ID, MAX_ROUNDS);
            nbt.putBoolean(RELOADING_ID, false);
        });
        return stack;
    }
}
